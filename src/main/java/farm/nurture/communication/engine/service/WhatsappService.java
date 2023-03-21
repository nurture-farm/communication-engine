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

package farm.nurture.communication.engine.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Timestamp;
import farm.nurture.communication.engine.Constants;
import farm.nurture.communication.engine.TimeOutConfigs;
import farm.nurture.communication.engine.cache.LanguageCache;
import farm.nurture.communication.engine.dto.HttpClientRequest;
import farm.nurture.communication.engine.event.CommunicationEventHandler;
import farm.nurture.communication.engine.event.DerivedCommunicationEvent;
import farm.nurture.communication.engine.metric.MetricGroupNames;
import farm.nurture.communication.engine.metric.MetricTracker;
import farm.nurture.communication.engine.metric.Metrics;
import farm.nurture.communication.engine.models.MessageAcknowledgement;
import farm.nurture.communication.engine.models.WhatsappUsers;
import farm.nurture.communication.engine.repository.MessageAcknowledgementRepository;
import farm.nurture.communication.engine.repository.WhatsappUsersRepository;
import farm.nurture.communication.engine.vendor.GupShupVendor;
import farm.nurture.communication.engine.vendor.KarixVendor;
import farm.nurture.communication.engine.vendor.Vendor;
import farm.nurture.communication.engine.vendor.VendorType;
import farm.nurture.core.contracts.communication.engine.CommunicationEvent;
import farm.nurture.core.contracts.communication.engine.Placeholder;
import farm.nurture.infra.util.ApplicationConfiguration;
import farm.nurture.kafka.Producer;
import farm.nurture.communication.engine.Constants;
import farm.nurture.util.http.HttpUtils;
import farm.nurture.util.http.TimeOutConfig;
import farm.nurture.util.http.client.NFAsyncHttpClient;
import farm.nurture.util.http.client.NFHttpClient;
import farm.nurture.util.serializer.Serializer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Singleton
public class WhatsappService {

    public static final String ACCEPT = "Accept";
    public static final String ACCEPT_VALUE = "*/*";
    public static final Integer EXPIREMONTHS=2;

    @Inject
    private MessageAcknowledgementRepository messageAcknowledgementRepository;

    @Inject
    private WhatsappUsersRepository whatsappUsersRepository;

    @Inject
    private LanguageCache languageCache;

    @Inject
    private NFAsyncHttpClient nfAsyncHttpClient;

    @Inject
    private NFHttpClient nfHttpClient;

    @Inject
    private TimeOutConfig timeOutConfig;

    @Inject
    private CloseableHttpClient closeableHttpClient;

    @Inject
    private Producer producer;
    
    @Inject
    private GupShupVendor gupshupVendor;
    
    @Inject
    private KarixVendor karixVendor;

    private int connectionTimeout;

    private int requestTimeout;

    private Metrics metrics = Metrics.getInstance();

    private static final ObjectMapper mapper = new ObjectMapper();

    private static final Map<String, Serializer> serializerMap = new HashMap<>();

    static {
        mapper.enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);
        serializerMap.put(Constants.APPLICATION_JSON, new PlainJsonSerializer());
    }

    public void sendMessage(DerivedCommunicationEvent event) {

        log.info("Sending Whatsapp message from event : {}", event);

        boolean success = false;
        MetricTracker tracker = new MetricTracker(MetricGroupNames.NF_CE, "whatsapp_service", Constants.LABEL_TEMPLATE, Constants.LABEL_LANGUAGE, Constants.LABEL_VENDOR );

        try {

            WhatsappUsers whatsappUsers = whatsappUsersRepository.getByMobileNumberKey(event.getWhatsappAttributes().getMobileNumber());
            Vendor vendor;
            if(event.getVendor() == VendorType.GUPSHUP){
                vendor = gupshupVendor;
            }else {
                vendor = karixVendor;
            }
            if(validateWhatsAppUser(whatsappUsers)) {

                    log.info("WhatsappUsers is present with opt_in status in whatsapp_user for event : {}", event);
                    HttpClientRequest httpClientRequest = vendor.getWhatsAppData(event);
                    String referenceId = populateMessageAcknowledgements(event, MessageAcknowledgement.State.VENDOR_UNDELIVERED);
                    if (referenceId != null) {
                        CommunicationEventHandler.IN_FLIGHT_COUNTER.incrementAndGet();
                        if (vendor == gupshupVendor)
                            timeOutConfig = TimeOutConfigs.whatsappServiceGupshupTimeOutConfig();
                        else
                            timeOutConfig = TimeOutConfigs.whatsappServiceKarixTimeOutConfig();
                        nfAsyncHttpClient.sendMessage(httpClientRequest.getMethod(), httpClientRequest.getUrl(),
                                httpClientRequest.getRequestParams(), httpClientRequest.getHeaders(),
                                httpClientRequest.getRequestBody(), new WhatsappServiceCallback(this, producer, event, messageAcknowledgementRepository, referenceId, languageCache), timeOutConfig);
                        log.info("Message has been sent in whatsApp service using referenceId : {} and DerivedCommunicationEvent : {}", referenceId, event);
                        success = true;
                }
            }else {
                log.info("WhatsappUsers is not present with opt_in status in whatsapp_user for event : {}", event);
                metrics.onIncrement(MetricGroupNames.NF_CE_WHATSAPP_SERVICE, "user_not_opted_in");
            }

        } catch (Exception e) {
            log.error("Error in sending Whatsapp message for event : {}", event, e);

        } finally {
            tracker.stop(success, event.getTemplateName(), languageCache.getLanguageById(event.getLanguageId()).getName(),event.getVendor().name());
        }
    }

    private String populateMessageAcknowledgements(DerivedCommunicationEvent derivedCommunicationEvent, MessageAcknowledgement.State messState) {
        log.info("Execution for populateMessageAcknowledgements in WhatsappService started using DerivedCommunicationEvent : {} and messState : {}",
                derivedCommunicationEvent, messState);
        CommunicationEvent commEvent ;
        String referenceId ;
        try {
            commEvent = CommunicationEvent.parseFrom((byte[]) derivedCommunicationEvent.getOriginalEvent().getMessage());
            referenceId = commEvent.getReferenceId();
            HashMap<String, Object> attributeMap = new HashMap<>();
            List<Placeholder> metaData = derivedCommunicationEvent.getMetaData();
            if(metaData!= null && metaData.size() > 0) {
                metaData.forEach(placeholder -> attributeMap.put("content_metadata_"+placeholder.getKey(), placeholder.getValue()));
            }
            MessageAcknowledgement messageAcknowledgement = MessageAcknowledgement.builder()
                    .actorId(derivedCommunicationEvent.getActorId())
                    .actorType(derivedCommunicationEvent.getActorType())
                    .mobileNumber(derivedCommunicationEvent.getWhatsappAttributes().getMobileNumber())
                    .communicationChannel(Constants.WHATSAPP)
                    .referenceId(referenceId)
                    .tempateName(derivedCommunicationEvent.getTemplateName())
                    .languageId(derivedCommunicationEvent.getLanguageId())
                    .messageContent(derivedCommunicationEvent.getContent())
                    .isUnicode(derivedCommunicationEvent.isUnicode())
                    .vendorName(derivedCommunicationEvent.getVendor().name())
                    .vendorMessageId("")
                    .state(messState)
                    .retryCount(derivedCommunicationEvent.getRetryCount())
                    .placeHolders(derivedCommunicationEvent.getPlaceholders())
                    .attributes(attributeMap)
                    .parentReferenceId(derivedCommunicationEvent.getParentReferenceId())
                    .campaignName(derivedCommunicationEvent.getCampaignName())
                    .actorContactId(derivedCommunicationEvent.getWhatsappAttributes().getMobileNumber())
                    .build();
            messageAcknowledgementRepository.insertMessageAcknowledgement(messageAcknowledgement);
        } catch (InvalidProtocolBufferException e) {
            log.error("Unable to deserialize protobuf in Whatsapp service", e);
            return null;
        }
        return referenceId;
    }

    private boolean validateOptInWhatsAppUser(WhatsappUsers whatsappUsers) {
        log.info("Execution for validateOptInWhatsAppUser in WhatsappService started using WhatsappUsers : {}", whatsappUsers);
        return (WhatsappUsers.WhatsAppStatus.OPT_IN == whatsappUsers.getStatus());
    }
    private boolean dateOlderThanxMonths(java.sql.Timestamp timestamp, int month)
    {
        LocalDateTime currentDate = LocalDateTime.now();
        LocalDateTime currentDateMinusXMonths = currentDate.minusMonths(month);
        LocalDateTime lastUpdatedDate = timestamp.toLocalDateTime();
        if (lastUpdatedDate.isBefore(currentDateMinusXMonths)) {
            return true;
        }
        return false;
    }
    private boolean validateNoAccntWhatsAppUser(WhatsappUsers whatsappUsers) {
        log.info("Execution for validateNoAccntWhatsAppUser in WhatsappService started using WhatsappUsers : {}", whatsappUsers);
        if (WhatsappUsers.WhatsAppStatus.NO_ACCNT == whatsappUsers.getStatus())
        {
            return dateOlderThanxMonths(whatsappUsers.getUpdatedAt(), EXPIREMONTHS);
        }
        else
            return false;
    }
    private boolean validateWhatsAppUser(WhatsappUsers whatsappUsers)
    {
        return ((whatsappUsers != null && StringUtils.isNotEmpty(whatsappUsers.getMobileNumber())) && (validateOptInWhatsAppUser(whatsappUsers) || validateNoAccntWhatsAppUser(whatsappUsers)));
    }

}
