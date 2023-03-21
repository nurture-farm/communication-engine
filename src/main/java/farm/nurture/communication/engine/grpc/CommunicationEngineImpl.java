/*
 *  Copyright 2023 NURTURE AGTECH PVT LTD
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package farm.nurture.communication.engine.grpc;


import com.google.inject.Inject;
import com.google.protobuf.Timestamp;
import farm.nurture.communication.engine.Constants;
import farm.nurture.communication.engine.cache.LanguageCache;
import farm.nurture.communication.engine.cache.TemplateCacheValue;
import farm.nurture.communication.engine.dto.WhatsAppOptUserResponse;
import farm.nurture.communication.engine.event.DerivedCommunicationEvent;
import farm.nurture.communication.engine.helper.RequestMapper;
import farm.nurture.communication.engine.helper.RequestValidator;
import farm.nurture.communication.engine.helper.ResponseMapper;
import farm.nurture.communication.engine.helper.TemplateHelper;
import farm.nurture.communication.engine.metric.MetricGroupNames;
import farm.nurture.communication.engine.metric.MetricTracker;
import farm.nurture.communication.engine.models.Language;
import farm.nurture.communication.engine.models.MessageAcknowledgement;
import farm.nurture.communication.engine.models.Template;
import farm.nurture.communication.engine.models.WhatsappUsers;
import farm.nurture.communication.engine.repository.MessageAcknowledgementRepository;
import farm.nurture.communication.engine.repository.TemplateRepository;
import farm.nurture.communication.engine.repository.WhatsappUsersRepository;
import farm.nurture.communication.engine.service.OptUserService;
import farm.nurture.communication.engine.service.SMSService;
import farm.nurture.communication.engine.service.TemplateManagementService;
import farm.nurture.communication.engine.service.WhatsappService;
import farm.nurture.communication.engine.utils.AESUtils;
import farm.nurture.communication.engine.utils.VendorLoadBalancer;
import farm.nurture.communication.engine.vendor.VendorType;
import farm.nurture.core.contracts.common.ActorID;
import farm.nurture.core.contracts.common.Attribs;
import farm.nurture.core.contracts.common.RequestHeaders;
import farm.nurture.core.contracts.common.enums.*;
import farm.nurture.core.contracts.communication.engine.*;
import farm.nurture.kafka.Event;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static farm.nurture.communication.engine.Constants.*;


@Slf4j
public class CommunicationEngineImpl implements CommunicationEngine {

    private static final String otpTemplateLike = "%otp%";
    private static final String whatsappOptInTemplate = "whatsapp_optin_sms";
    private static final String defaultFarmerName = "farmer";
    private static final String optInNumber = "9029059263";
    private static final String optInLink = "https://nrf.page.link/home";
    @Inject
    private RequestValidator requestValidator;
    @Inject
    private RequestMapper requestMapper;
    @Inject
    private ResponseMapper responseMapper;
    @Inject
    private WhatsappUsersRepository whatsappUsersRepository;
    @Inject
    private OptUserService optUserService;
    @Inject
    private MessageAcknowledgementRepository messageAcknowledgementRepository;
    @Inject
    private LanguageCache languageCache;
    @Inject
    private WhatsappService whatsappService;
    @Inject
    private SMSService smsService;
    @Inject
    private AESUtils aesUtils;
    @Inject
    private TemplateRepository templateRepository;

    @Inject
    private TemplateHelper templateHelper;
    @Inject
    private VendorLoadBalancer vendorLoadBalancer;

    @Inject
    private TemplateManagementService templateManagementService;


    @Override
    public OptInRespone optInUser(OptInRequest request) {
        log.info("OptIn whatsapp request. Request: {}", request);
        OptInRespone response = requestValidator.validate(request);
        if (response != null) return response;

        boolean success = false;
        MetricTracker tracker = new MetricTracker(MetricGroupNames.NF_CE, "whatsapp_opt_in_user");
        Map<String, String> attributeMap = new HashMap<>();

        try {
            WhatsappUsers whatsappUsers = whatsappUsersRepository.getByMobileNumberKey(request.getMobileNumber());
            log.info("Successfully fetched whatsappUsers for request: {}", request);
            if (validateOptInWhatsAppUser(whatsappUsers)) {
                attributeMap.put("id", String.valueOf(whatsappUsers.getId()));
                return responseMapper.mapToOptInRespone(ResponseStatus.SUCCESSFUL, ResponseStatusCode.OK, attributeMap);
            }

            WhatsAppOptUserResponse optInUserResponse = optUserService.whatsappOptUser(request.getMobileNumber(), WhatsappUsers.WhatsAppStatus.OPT_IN);

            String responseMessageIdCode = StringUtils.EMPTY;
            if (optInUserResponse != null &&
                    optInUserResponse.getData() != null && CollectionUtils.isNotEmpty(optInUserResponse.getData().getResponseMessage())) {
                responseMessageIdCode = optInUserResponse.getData().getResponseMessage().get(0).getId();
            }

            if (optInUserResponse != null && optInUserResponse.getResponse() != null &&
                    (Constants.OPT_USER_DUPLICATE_ENTRY_CODE.equals(optInUserResponse.getResponse().getId())
                            || (StringUtils.isNotEmpty(responseMessageIdCode) && Constants.OPT_USER_DUPLICATE_ENTRY_CODE.equals(responseMessageIdCode))
                            || Constants.OPT_USER_SUCCESS_STATUS.equalsIgnoreCase(optInUserResponse.getResponse().getStatus()))) {
                log.info("WhatsApp Opt in successful using OptInRequest : {}", request);
                boolean whatsAppEntry;
                if (whatsappUsers != null && StringUtils.isNotEmpty(whatsappUsers.getMobileNumber()) && StringUtils.isNotEmpty(whatsappUsers.getStatus().name())) {
                    whatsAppEntry = whatsappUsersRepository.updateWhatsappUsers(WhatsappUsers.builder()
                            .status(WhatsappUsers.WhatsAppStatus.OPT_IN).mobileNumber(request.getMobileNumber()).build());
                } else {
                    String namespace = "FARM";
                    String sourceSystem = "FARM_APP";
                    if (request.getNameSpace() != NameSpace.NO_NAMESPACE) {
                        namespace = request.getNameSpace().name();
                    }

                    whatsappUsers = WhatsappUsers.builder()
                            .mobileNumber(request.getMobileNumber())
                            .status(WhatsappUsers.WhatsAppStatus.OPT_IN)
                            .namespace(namespace)
                            .source(sourceSystem)
                            .build();
                    whatsAppEntry = whatsappUsersRepository.insertWhatsappUsers(whatsappUsers);
                }
                if (!whatsAppEntry) {
                    return responseMapper.mapToOptInRespone(ResponseStatus.ERROR, ResponseStatusCode.INTERNAL_SERVER_ERROR, null);
                }
//                if (validateOptInWhatsAppUser(whatsappUsers)) {
//                    attributeMap.put("id", String.valueOf(whatsappUsers.getId()));
//                    return whatsAppOptIn(request, optInUserResponse, attributeMap);
//                }
//                log.error("Unable to get validateOptInWhatsAppUser for request:{}", request);
//                return responseMapper.mapToOptInRespone(ResponseStatus.ERROR, ResponseStatusCode.INTERNAL_SERVER_ERROR, null);
            }
            attributeMap.put("optInId", optInUserResponse.getResponse().getId());
            attributeMap.put("status", optInUserResponse.getResponse().getStatus());
            attributeMap.put("details", optInUserResponse.getResponse().getDetails());

            OptInRespone optInRespone = responseMapper.mapToOptInRespone(ResponseStatus.SUCCESSFUL, ResponseStatusCode.OK, attributeMap);
            success = true;
            return optInRespone;

        } catch (Exception e) {
            log.error("Error in optIn whatsapp user, request : {}", request, e);
            return responseMapper.mapToOptInRespone(ResponseStatus.ERROR, ResponseStatusCode.INTERNAL_SERVER_ERROR, null);

        } finally {
            tracker.stop(success);
        }
    }

    private OptInRespone whatsAppOptIn(OptInRequest request, WhatsAppOptUserResponse optInUserResponse, Map<String, String> attributeMap) {
//        sendSMSEvent(request);
        log.info("Successfully served sendSMS. OptInRequest: {}", request);

        attributeMap.put("optInId", optInUserResponse.getResponse().getId());
        attributeMap.put("details", optInUserResponse.getResponse().getDetails());
        return responseMapper.mapToOptInRespone(ResponseStatus.SUCCESSFUL, ResponseStatusCode.OK, attributeMap);
    }

    private void sendSMSEvent(OptInRequest request) throws Exception {
        log.info("SendSMSEvent using OptInRequest : {}", request);
        final String referenceId = UUID.randomUUID().toString();
        final RequestHeaders requestHeader = request.getRequestHeaders();

        Map<String, Object> placeholderMap = new HashMap<>();
        placeholderMap.put("farmer_name", defaultFarmerName); // TODO : replace with farmer name
        placeholderMap.put("phone_number", optInNumber);
        placeholderMap.put("link_to_optin", optInLink);

        List<Placeholder> placeholders = placeholderMap.entrySet().stream().map(entry ->
                Placeholder.newBuilder().setKey(entry.getKey()).setValue(String.valueOf(entry.getValue())).build())
                .collect(Collectors.toList());

        CommunicationEvent communicationEvent = CommunicationEvent.newBuilder()
                .setReceiverActorDetails(ActorDetails.newBuilder().setMobileNumber(request.getMobileNumber())
                        .setLanguageCode(requestHeader.getLanguageCode()).build())
                .setReceiverActor(request.getActor())
                .addAllPlaceholder(placeholders)
                .setReferenceId(referenceId)
                .addChannel(CommunicationChannel.SMS)
                .setTemplateName(whatsappOptInTemplate)
                .build();
        log.info("CommunicationEvent value is:{}", communicationEvent);

        Event<byte[], byte[]> event = new Event<>(communicationEvent.getReferenceId().getBytes(), communicationEvent.toByteArray());

        Language language = languageCache.getLanguageByCode(communicationEvent.getReceiverActorDetails()
                .getLanguageCode().name().toLowerCase().replace('_', '-'));
        log.info("Language:{} in sendSMSEvent for whatsOptIn using OptInRequest:{}", language, request);

        String content = aesUtils.getContent(communicationEvent, language);
        log.info("Content:{} for OptInRequest:{}", content, request);

        ActorID actorId = communicationEvent.getReceiverActor();
        DerivedCommunicationEvent derivedEvent = new DerivedCommunicationEvent(event, referenceId, requestHeader.getClientId(), content, language != null ? language.getUnicode() : false,
                null, null, CommunicationChannel.SMS, language.getId(), whatsappOptInTemplate, placeholderMap, null, null, null,
                communicationEvent.getCampaignName(),
                actorId != null ? actorId.getActorId() : 0L,
                actorId != null ? actorId.getActorType() : ActorType.NO_ACTOR
        );
        derivedEvent.setSmsAttributes(new DerivedCommunicationEvent.SMSAttributes(communicationEvent.getReceiverActorDetails().getMobileNumber()));
        derivedEvent.setVendor(vendorLoadBalancer.getVendor(CommunicationChannel.SMS.name()));
        log.info("Derived:{} event for optInRequest:{}", derivedEvent, request);
        smsService.sendSms(derivedEvent);
    }

    private boolean validateOptInWhatsAppUser(WhatsappUsers whatsappUsers) {
        return whatsappUsers != null && StringUtils.isNotEmpty(whatsappUsers.getMobileNumber()) && StringUtils.isNotEmpty(whatsappUsers.getStatus().name())
                && WhatsappUsers.WhatsAppStatus.OPT_IN == whatsappUsers.getStatus();
    }

    private boolean validateOptOutWhatsAppUser(WhatsappUsers whatsappUsers) {
        return whatsappUsers != null && StringUtils.isNotEmpty(whatsappUsers.getMobileNumber()) && StringUtils.isNotEmpty(whatsappUsers.getStatus().name())
                && WhatsappUsers.WhatsAppStatus.OPT_OUT == whatsappUsers.getStatus();
    }

    @Override
    public MessageAcknowledgementResponse searchMessageAcknowledgements(MessageAcknowledgementRequest request) {

        log.info("Serving getDeliveryInfo request. Request: {}", request);
        MessageAcknowledgementResponse response = requestValidator.validate(request);
        if (response != null) return response;

        boolean success = false;
        MetricTracker tracker = new MetricTracker(MetricGroupNames.NF_CE, "get_delivery_info");

        try {
            Timestamp startTime = request.getStartTime();
            Timestamp endTime = request.getEndTime();

            List<MessageAcknowledgement> messageAcks = messageAcknowledgementRepository.
                    getMessageAcknowledgementByDuration(request.getChannelsList(),
                            new java.sql.Timestamp(startTime.getSeconds() * 1000),
                            new java.sql.Timestamp(endTime.getSeconds() * 1000),
                            request.getTemplateNameLike(),
                            request.getMobileNumber(),
                            request.getReferenceId(),
                            request.getLimit(),
                            request.getOffset(),
                            request.getResponseOrderType());
            log.info("Successfully fetched deliveryInfo. request: {}, Number of record: {}", request, messageAcks.size());
            MessageAcknowledgementResponse messageAcknowledgementResponse = responseMapper.mapToMessageAcknowledgementResponse(ResponseStatus.SUCCESSFUL, ResponseStatusCode.OK, messageAcks);
            success = true;
            return messageAcknowledgementResponse;
        } catch (Exception e) {
            log.error("Error in fetching deliveryInfo, request : {}", request, e);
            return responseMapper.mapToMessageAcknowledgementResponse(ResponseStatus.ERROR, ResponseStatusCode.INTERNAL_SERVER_ERROR, null);

        } finally {
            tracker.stop(success);
        }
    }

    @Override
    public AddTemplateResponse addTemplate(AddTemplateRequest request) {

        log.info("Serving addTemplate request. Request: {}", request);
        AddTemplateResponse response = requestValidator.validate(request);
        if (response != null) return response;

        boolean success = false;
        MetricTracker tracker = new MetricTracker(MetricGroupNames.NF_CE, "add_template");
        List<VendorResponse> vendorResponseList = new ArrayList<>();
        try {
            Template template = mapRequestTemplate(request);
            Map<String, String> attributesMap = new HashMap<>();
            for (Attribs attribs : request.getAttribsList()) {
                attributesMap.put(attribs.getKey(), attribs.getValue());
            }
            Map<String, Object> metaDataMap = new HashMap<>();
            for (Attribs attribs : request.getMetaDataList()) {
                metaDataMap.put(attribs.getKey(), attribs.getValue());
            }
            template.setAttributes(attributesMap);
            template.setMetaData(metaDataMap);

            if(request.getChannel() == CommunicationChannel.WHATSAPP ){

                if(!metaDataMap.containsKey(GUPSHUP_TEMPLATE_NAME)){
                    log.info("Serving create template api gupshup request {} ", template);
                    VendorResponse gupshupResponse  = templateManagementService.createTemplateInGupshup(template);
                    vendorResponseList.add(gupshupResponse);
                }

                if(!metaDataMap.containsKey(KARIX_TEMPLATE_NAME)){
                    log.info("Serving create template api karix request {} ", template);
                    VendorResponse karixResponse = templateManagementService.createTemplateInKarix(template);
                    vendorResponseList.add(karixResponse);
                }

            }
            if(request.getChannel() == CommunicationChannel.APP_NOTIFICATION || request.getChannel() == CommunicationChannel.EMAIL){
                template.setActive(true);
            }
            Integer id = templateRepository.insertTemplates(template);
            attributesMap.put("id", String.valueOf(id));
            log.info("Successfully served addTemplate request. Request: {}", request);
            success = true;
            return responseMapper.mapToAddTemplateResponse(ResponseStatus.SUCCESSFUL, ResponseStatusCode.OK, attributesMap, vendorResponseList);
        } catch (Exception e) {
            log.error("Error in serving addTemplate, request : {}", request, e);
            return responseMapper.mapToAddTemplateResponse(ResponseStatus.ERROR, ResponseStatusCode.INTERNAL_SERVER_ERROR, null, vendorResponseList);
        } finally {
            tracker.stop(success);
        }
    }

    private Template mapRequestTemplate(AddTemplateRequest request) {

        return Template.builder().name(request.getName())
                .languageId(languageCache.getLanguageByCode(request.getLanguageCode().name().toLowerCase().replace('_', '-')).getId())
                .contentType(request.getTemplateContentType() == TemplateContentType.HTML ? Template.ContentType.HTML : Template.ContentType.STRING)
                .content(request.getContent())
                .active(false).ownerEmail(request.getOwner()).vertical(request.getVertical())
                .title(request.getTitle())
                .build();
    }

    @Override
    public OptOutResponse optOutUser(OptOutRequest request) {
        log.info("OptOut whatsapp request. Request: {}", request);
        OptOutResponse response = requestValidator.validate(request);
        if (response != null) return response;

        boolean success = false;
        MetricTracker tracker = new MetricTracker(MetricGroupNames.NF_CE, "whatsapp_opt_out_user");
        Map<String, String> attributeMap = new HashMap<>();

        try {
            WhatsappUsers whatsappUsers = whatsappUsersRepository.getByMobileNumberKey(request.getMobileNumber());
            log.info("Successfully fetched whatsappUsers optoutUser for request: {}", request);
            if (validateOptOutWhatsAppUser(whatsappUsers)) {
                attributeMap.put("id", String.valueOf(whatsappUsers.getId()));
                return responseMapper.mapToOptOutRespone(ResponseStatus.SUCCESSFUL, ResponseStatusCode.OK);
            }

            WhatsAppOptUserResponse optOutUserResponse = optUserService.whatsappOptUser(request.getMobileNumber(), WhatsappUsers.WhatsAppStatus.OPT_OUT);

            String responseMessageIdCode = StringUtils.EMPTY;
            if (optOutUserResponse != null && optOutUserResponse.getData() != null && CollectionUtils.isNotEmpty(optOutUserResponse.getData().getResponseMessage())) {
                responseMessageIdCode = optOutUserResponse.getData().getResponseMessage().get(0).getId();
            }

            if (optOutUserResponse != null && optOutUserResponse.getResponse() != null &&
                    (Constants.OPT_USER_DUPLICATE_ENTRY_CODE.equals(optOutUserResponse.getResponse().getId())
                            || (StringUtils.isNotEmpty(responseMessageIdCode) && Constants.OPT_USER_DUPLICATE_ENTRY_CODE.equals(responseMessageIdCode))
                            || Constants.OPT_USER_SUCCESS_STATUS.equalsIgnoreCase(optOutUserResponse.getResponse().getStatus()))) {
                log.info("WhatsApp Opt in successful using OptOutRequest : {}", request);
                boolean whatsAppEntry;
                if (whatsappUsers != null && StringUtils.isNotEmpty(whatsappUsers.getMobileNumber()) && StringUtils.isNotEmpty(whatsappUsers.getStatus().name())) {
                    whatsAppEntry = whatsappUsersRepository.updateWhatsappUsers(WhatsappUsers.builder()
                            .status(WhatsappUsers.WhatsAppStatus.OPT_OUT).mobileNumber(request.getMobileNumber()).build());
                } else {
                    String namespace = "FARM";
                    if (request.getNameSpace() != NameSpace.NO_NAMESPACE) {
                        namespace = request.getNameSpace().name();
                    }
                    whatsappUsers = WhatsappUsers.builder()
                            .mobileNumber(request.getMobileNumber())
                            .status(WhatsappUsers.WhatsAppStatus.OPT_OUT)
                            .namespace(namespace)
                            .build();
                    whatsAppEntry = whatsappUsersRepository.insertWhatsappUsers(whatsappUsers);
                }
                if (!whatsAppEntry) {
                    return responseMapper.mapToOptOutRespone(ResponseStatus.ERROR, ResponseStatusCode.INTERNAL_SERVER_ERROR);
                }
            }
            attributeMap.put("optInId", optOutUserResponse.getResponse().getId());
            attributeMap.put("status", optOutUserResponse.getResponse().getStatus());
            attributeMap.put("details", optOutUserResponse.getResponse().getDetails());

            OptOutResponse optOutRespone = responseMapper.mapToOptOutRespone(ResponseStatus.SUCCESSFUL, ResponseStatusCode.OK);
            success = true;
            return optOutRespone;

        } catch (Exception e) {
            log.error("Error in optOut whatsapp user, request : {}", request, e);
            return responseMapper.mapToOptOutRespone(ResponseStatus.ERROR, ResponseStatusCode.INTERNAL_SERVER_ERROR);

        } finally {
            tracker.stop(success);
        }
    }

    @Override
    public GetAllTemplateResponse getAllTemplate(GetAllTemplateRequest getAllTemplateRequest) {
        boolean success = true;
        MetricTracker tracker = new MetricTracker(MetricGroupNames.NF_CE, "fetched_all_templates");
        GetAllTemplateResponse getAllTemplateResponse;
        try {
            List<Template> templateList = templateRepository.getAllTemplate(getAllTemplateRequest);
            log.info("Successfully fetched all templates request, Number of record: {}", templateList.size());
            getAllTemplateResponse = responseMapper.mapToTemplateResponse(ResponseStatus.SUCCESSFUL, ResponseStatusCode.OK, templateList);
        } catch (Exception e) {
            log.error("Error in fetching all the templates: {} ", e);
            getAllTemplateResponse = responseMapper.mapToTemplateResponse(ResponseStatus.ERROR, ResponseStatusCode.INTERNAL_SERVER_ERROR, null);
            success = false;
        } finally {
            tracker.stop(success);
        }
        return getAllTemplateResponse;
    }

    @Override
    public AddTemplateResponse updateTemplate(TemplateUpdateRequest templateUpdateRequest) {

        log.info("Serving update Template request. Request: {}", templateUpdateRequest);
        AddTemplateResponse response = requestValidator.validate(templateUpdateRequest);
        if (response != null) return response;

        boolean success = false;
        MetricTracker tracker = new MetricTracker(MetricGroupNames.NF_CE, "update_template");

        try {

            Integer id = templateRepository.updateTemplate(templateUpdateRequest);
            Map<String, String> attributesMap = new HashMap<>();
            attributesMap.put("id", String.valueOf(id));
            log.info("Successfully served update Template request. Request: {}, attributesMap {}", templateUpdateRequest, attributesMap);
            success = true;
            response = responseMapper.mapToAddTemplateResponse(ResponseStatus.SUCCESSFUL, ResponseStatusCode.OK, attributesMap,null );
        } catch (Exception e) {
            log.error("Error in serving update template, request : {}", templateUpdateRequest, e);
            response = responseMapper.mapToAddTemplateResponse(ResponseStatus.ERROR, ResponseStatusCode.INTERNAL_SERVER_ERROR, null, null);

        } finally {
            tracker.stop(success);
        }
        return response;
    }

    private String activationTest(ActivationRequest activationRequest,
                                  Language language,
                                  TemplateCacheValue templateCacheValue, CommunicationVendor vendor, VendorType vendorType) throws IOException {

        final String referenceId = UUID.randomUUID().toString();

        ActorDetails actorDetails = ActorDetails.newBuilder().
                    setMobileNumber(activationRequest.getMobileNumber()).
                    setLanguageCode(activationRequest.getLanguageCode()).build();

        CommunicationEvent.Builder builder = CommunicationEvent.newBuilder()
                    .setReceiverActorDetails(actorDetails)
                    .setTemplateName(activationRequest.getTemplateName())
                    .addChannel(activationRequest.getChannel())
                    .setReferenceId(referenceId)
                    .setVendor(vendor);

        if(activationRequest.getMedia() != null){
            builder.setMedia(activationRequest.getMedia());
        }
        Map<String, String> placeholderMap = null;
        Map<String, String> attributesMap = null;
        Map<String, Object> vendorMetaDataMap = null;
        String content = templateCacheValue.getTemplate().getContent();
        if(CollectionUtils.isNotEmpty(activationRequest.getPlaceholdersList())){
            builder.addAllPlaceholder(activationRequest.getPlaceholdersList());
            placeholderMap = getPlaceholderMapFromList(activationRequest.getPlaceholdersList());
            content = templateHelper.getContent(templateCacheValue, placeholderMap);
            attributesMap = templateHelper.getAttributes(templateCacheValue, placeholderMap);
        }
        vendorMetaDataMap = templateHelper.getMetaData(templateCacheValue);

        CommunicationEvent communicationEvent = builder.build();

        Event<byte[], byte[]> event = new Event<>(communicationEvent.getReferenceId().getBytes(), communicationEvent.toByteArray());

        DerivedCommunicationEvent derivedEvent = new DerivedCommunicationEvent(event, referenceId, "ACTIVATION-TEST",
                content, language != null ? language.getUnicode() : false,
                null, null,
                activationRequest.getChannel(), language.getId(), activationRequest.getTemplateName(), placeholderMap, null, attributesMap,
                vendorMetaDataMap,
                communicationEvent.getCampaignName(),
                0L, ActorType.NO_ACTOR
        );
        derivedEvent.setVendor(vendorType);
        switch (activationRequest.getChannel()) {
            case SMS:
                derivedEvent.setSmsAttributes(new DerivedCommunicationEvent.SMSAttributes(activationRequest.getMobileNumber()));
                log.info("Derived event: {} for sms activation template request: {}", derivedEvent, activationRequest);
                smsService.sendSms(derivedEvent);
                break;
            case WHATSAPP:
                if (activationRequest.getMedia() != null && activationRequest.getMedia().getMediaType().getNumber() != 0) {
                    DerivedCommunicationEvent.WhatsappAttributes.Media.MediaBuilder media = DerivedCommunicationEvent.WhatsappAttributes.Media.builder();
                    media.mediaAccessType(communicationEvent.getMedia().getMediaAccessType()).mediaInfo(communicationEvent.getMedia().getMediaInfo())
                            .documentName(communicationEvent.getMedia().getDocumentName()).mediaType(communicationEvent.getMedia().getMediaType());
                    derivedEvent.setWhatsappAttributes(new DerivedCommunicationEvent.WhatsappAttributes(
                            activationRequest.getMobileNumber(), media.build()));
                }else{
                    derivedEvent.setWhatsappAttributes(DerivedCommunicationEvent.WhatsappAttributes.builder().mobileNumber(
                            activationRequest.getMobileNumber()).build());
                }
                log.info("Derived event: {} for whatsapp activation template request: {}", derivedEvent, activationRequest);
                whatsappService.sendMessage(derivedEvent);
        }
        return referenceId;
    }

    private VendorResponse getVendorResponse(String referenceId, String communicationChannel, String vendorName){
        VendorResponse vendorResponse;
        List<MessageAcknowledgement> messageAcknowledgementList =
                messageAcknowledgementRepository.getGetMessageAcknowledgementEnableByReferenceIdAndCommunicationChannel(referenceId,
                communicationChannel);
        if(messageAcknowledgementList.size() != 0)
        {
            MessageAcknowledgement messageAcknowledgement = messageAcknowledgementList.get(0);
            if(messageAcknowledgement.getState() == MessageAcknowledgement.State.VENDOR_UNDELIVERED || messageAcknowledgement.getState() == MessageAcknowledgement.State.CUSTOMER_UNDELIVERED || messageAcknowledgement.getState() == MessageAcknowledgement.State.VENDOR_DELIVERED){
                vendorResponse = responseMapper.mapToVendorResponse(
                        vendorName,
                        ResponseStatus.ERROR, ResponseStatusCode.BAD_REQUEST,
                        messageAcknowledgement.getAttributes());
            }else {
                vendorResponse = responseMapper.mapToVendorResponse(
                        vendorName,
                        ResponseStatus.SUCCESSFUL, ResponseStatusCode.OK, null);
            }
        }
        else{
            vendorResponse = responseMapper.mapToVendorResponse(vendorName,
                    ResponseStatus.ERROR, ResponseStatusCode.INTERNAL_SERVER_ERROR, null);
        }
        return vendorResponse;
    }

    @Override
    public ActivationResponse activateTemplate(ActivationRequest activationRequest) {
        log.info("Serving activate Template request. Request: {}", activationRequest);
        ActivationResponse activationResponse;
        activationResponse = requestValidator.validate(activationRequest);
        if (activationResponse != null) return activationResponse;
        String vendorName = null;
        boolean success = false;
        MetricTracker tracker = new MetricTracker(MetricGroupNames.NF_CE, "activate_template");
        try {
            Language language = languageCache.getLanguageByCode(activationRequest.getLanguageCode().name().toLowerCase().replace('_', '-'));
            TemplateCacheValue templateCacheValue = templateHelper.getAllTemplateFromNameAndLangId(activationRequest.getTemplateName(), language);
            if (templateCacheValue == null) {
                log.error("Error in serving activateTemplate, TEMPLATE NOT FOUND for request {}", activationRequest);
                VendorResponse.Builder vendorResponse = VendorResponse.newBuilder().setStatusCode(ResponseStatusCode.BAD_REQUEST)
                        .setStatus(ResponseStatus.ERROR).setVendorName(vendorName);
                return responseMapper.mapToActivationResponse(Collections.singletonList(vendorResponse.build()), false);
            }
            String referenceIdForGupshup = activationTest(activationRequest, language, templateCacheValue, CommunicationVendor.GUPSHUP, VendorType.GUPSHUP);
            String referenceIdForKarix = activationTest(activationRequest, language, templateCacheValue,  CommunicationVendor.KARIX, VendorType.KARIX);
            try{
                Thread.sleep(7000); } catch (Exception e){
                log.info("Thread Execution Error", e);
            }
            VendorResponse vendorResponse1 = getVendorResponse(referenceIdForGupshup, activationRequest.getChannel().name(), VendorType.GUPSHUP.name());
            VendorResponse vendorResponse2 = getVendorResponse(referenceIdForKarix, activationRequest.getChannel().name(), VendorType.KARIX.name());
            /**
             *TODO: Once system is stable check template is whitelisted on both gupshup & karix
             */
            if(vendorResponse1.getStatus() == ResponseStatus.SUCCESSFUL) {
                templateRepository.updateTemplateToActive(templateCacheValue.getTemplate().getName(), templateCacheValue.getTemplate().getLanguageId());
            }
            boolean active = vendorResponse1.getStatus() == ResponseStatus.SUCCESSFUL && vendorResponse2.getStatus() == ResponseStatus.SUCCESSFUL;
            activationResponse = responseMapper.mapToActivationResponse(Arrays.asList(vendorResponse1, vendorResponse2), active);
            success = true;
        } catch (Exception ex) {
            log.error("Error in serving activationTemplate request : {}, error : {}", activationRequest, ex);
            VendorResponse.Builder vendorResponse = VendorResponse.newBuilder().setStatusCode(ResponseStatusCode.BAD_REQUEST)
                    .setStatus(ResponseStatus.ERROR).setVendorName(vendorName);
            activationResponse = responseMapper.mapToActivationResponse(Collections.singletonList(vendorResponse.build()), false);
        } finally {
            tracker.stop(success);
        }
        return activationResponse;
    }

    private Map<String, String> getPlaceholderMapFromList(List<Placeholder> placeholderList) {
        return placeholderList.stream()
                .collect(Collectors.toMap(Placeholder::getKey, Placeholder::getValue));

    }

    //    @Override
//    public BulkCommunicationResponse sendBulkCommunication(BulkCommunicationEvent events) {
//        log.info("Serving sendWhatsappMessage request. Number of events: {}, Events: {}", events.getCommunicationEventsList().size(), events);
//        BulkCommunicationResponse response = requestValidator.validate(events);
//        if(response != null) return response;
//
//        boolean success = false;
//        MetricTracker tracker = new MetricTracker(MetricGroupNames.NF_CE, "send_whatsapp_message");
//        try {
//            for(CommunicationEvent communicationEvent: events.getCommunicationEventsList()) {
//                boolean isUnicode = communicationEvent.getReceiverActorDetails().getLanguageCode() == LanguageCode.EN_US ? false:true;
//                Language language = languageCache.getLanguageByCode(communicationEvent.getReceiverActorDetails().getLanguageCode().name().toLowerCase().replace('_', '-'));
//
//                Event<byte[], byte[]> event = new Event<>(communicationEvent.getReferenceId().getBytes(), communicationEvent.toByteArray());
//
//                String content = aesUtils.getContent(communicationEvent, language);
//                DerivedCommunicationEvent derivedEvent = new DerivedCommunicationEvent(event, "sendWhatsappMessageAPI", content, isUnicode,
//                        null, null, CommunicationChannel.WHATSAPP, language.getId(), communicationEvent.getTemplateName());
//                derivedEvent.setWhatsappAttributes(new DerivedCommunicationEvent.WhatsappAttributes(communicationEvent.getReceiverActorDetails().getMobileNumber()));
//                whatsappService.sendMessage(derivedEvent);
//            }
//            log.info("Successfully served sendWhatsappMessage. Events: {}", events);
//            success = true;
//            return responseMapper.mapSendWhatsappEventResponse(ResponseStatus.SUCCESSFUL, ResponseStatusCode.OK);
//        } catch (Exception e) {
//            log.error("Error in serving sendWhatsappMessage, Events : {}", events, e);
//            return responseMapper.mapSendWhatsappEventResponse(ResponseStatus.ERROR, ResponseStatusCode.INTERNAL_SERVER_ERROR);
//
//        } finally {
//            tracker.stop(success);
//        }
//    }


}
