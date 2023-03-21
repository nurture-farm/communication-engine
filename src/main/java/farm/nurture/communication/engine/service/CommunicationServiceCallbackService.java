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

import com.google.inject.Inject;
import com.google.inject.Singleton;
import farm.nurture.communication.engine.Constants;
import farm.nurture.communication.engine.cache.LanguageCache;
import farm.nurture.communication.engine.dto.CommunicationServiceCallbackRequest;
import farm.nurture.communication.engine.dto.GupshupWhatsAppIncomingCallbackRequest;
import farm.nurture.communication.engine.event.DerivedCommunicationEvent;
import farm.nurture.communication.engine.metric.Metrics;
import farm.nurture.communication.engine.models.MessageAcknowledgement;
import farm.nurture.communication.engine.models.WhatsappUsers;
import farm.nurture.communication.engine.repository.MessageAcknowledgementRepository;
import farm.nurture.communication.engine.repository.WhatsappUsersRepository;
import farm.nurture.communication.engine.utils.ExecutorServiceImpl;
import farm.nurture.communication.engine.vendor.VendorType;
import farm.nurture.core.contracts.common.ActorID;
import farm.nurture.core.contracts.common.enums.ActorType;
import farm.nurture.core.contracts.common.enums.CommunicationChannel;
import farm.nurture.core.contracts.communication.engine.CommunicationEvent;
import farm.nurture.infra.util.StringUtils;
import farm.nurture.kafka.Event;
import lombok.extern.slf4j.Slf4j;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static farm.nurture.communication.engine.Constants.CLEVER_TAP_CAMPAIGN_PREFIX;


@Slf4j
@Singleton
public class CommunicationServiceCallbackService {


    public static final String ISSUE_UPDATE_MSG_ACK = "Unable to update MessageAcknowledgements. CommunicationServiceCallbackRequest: {}, CommunicationServiceCallbackRequestStatus: {} ";
    private static final int totalRetryCount = 2;
    private static final String DND_FAIL = "DND_FAIL";
    private static final String DND_TIMEOUT = "DND_TIMEOUT";
    private static final String BLOCKED_FOR_USER = "BLOCKED_FOR_USER";
    private static final String UNKNOWN_SUBSCRIBER = "UNKNOWN_SUBSCRIBER";
    private static final String INVALID_RECIPIENT = "Invalid Recipient";
    private static final String DEFERED = "DEFERED";
    private static final String TRY_MULTIPLE_TIMES = "Already has been tried for totalRetryCount : {} times for given communicationChannel : {} and reference_id : {}";
    private static final String RETRY_REQUEST = "Retry request for call back service has been submitted for given communicationChannel : {} and reference_id : {}";
    private static final String MSG_ACK_ERROR = "there is some error in messageAcknowledgementRepository while fetching entry for given communicationChannel : {} and vendorMessageId : {} as it is failing";
    private static final String NO_ENTRY_MSG_ACK = "there is no entry in message_acknowledgements for given communicationChannel : {} and vendorMessageId : {}";
    private static final String UPDATE_MSG_ACK_WITH_CAUSE = "Updating messageAcknowledgment with cause {} for externalId: {}";
    private static final String UPDATE_MSG_ACK = "Updating messageAcknowledgment as Success for externalId: {}";
    private static final String ISSUE_NO_ENTRY_MSG_ACK = "there is some error in messageAcknowledgementRepository or there is no entry present in message_acknowledgements for given communicationChannel : {} and reference_id : {}";
    private static final String CLIENT_ID = "GupShupCallBack";
    Metrics metrics = Metrics.getInstance();
    @Inject
    ExecutorServiceImpl executorService;
    @Inject
    private MessageAcknowledgementRepository messageAcknowledgementRepository;
    @Inject
    private WhatsappUsersRepository whatsappUsersRepository;
    @Inject
    private SMSService smsService;
    @Inject
    private LanguageCache languageCache;

    public boolean updateMessageAcknowledgements(String communicationChannel, CommunicationServiceCallbackRequest communicationServiceCallbackRequest,
                                                 String merticsGroupName
    ) {

        boolean isUpdateMsgAck = false;
        MessageAcknowledgementRepository.ResponseObject responseObject = null;
        MessageAcknowledgement.State currState = MessageAcknowledgement.State.CUSTOMER_DELIVERED;

        try {
            responseObject = messageAcknowledgementRepository.getMessageAcknowledgementByVendorMessageId
                    (communicationChannel, communicationServiceCallbackRequest.getExternalId());

            if (!responseObject.success || responseObject.messageAcknowledgement == null) {
                String ERROR = (!responseObject.success) ? MSG_ACK_ERROR : NO_ENTRY_MSG_ACK;
                log.error(ERROR, communicationChannel, communicationServiceCallbackRequest.getExternalId());
                return false;
            }

            String referenceId = responseObject.messageAcknowledgement.getReferenceId();
            Short languageId = responseObject.messageAcknowledgement.getLanguageId();
            WhatsappUsers whatsappUsers = whatsappUsersRepository.getByMobileNumberKey(responseObject.messageAcknowledgement.getActorContactId());
            if (communicationServiceCallbackRequest.getCause().equals(DND_FAIL) ||
                    communicationServiceCallbackRequest.getCause().equals(DND_TIMEOUT) ||
                    communicationServiceCallbackRequest.getCause().equals(BLOCKED_FOR_USER) ||
                    communicationServiceCallbackRequest.getCause().equals(UNKNOWN_SUBSCRIBER) ||
                    communicationServiceCallbackRequest.getCause().equals(INVALID_RECIPIENT) ||
                    communicationServiceCallbackRequest.getCause().equals(DEFERED)) {

                log.info(UPDATE_MSG_ACK_WITH_CAUSE, communicationServiceCallbackRequest.getCause(), communicationServiceCallbackRequest.getExternalId());
                currState = MessageAcknowledgement.State.CUSTOMER_UNDELIVERED;
                if(communicationServiceCallbackRequest.getCause().equals(UNKNOWN_SUBSCRIBER)||communicationServiceCallbackRequest.getCause().equals(INVALID_RECIPIENT))
                {
                    if(whatsappUsers != null && communicationChannel.equalsIgnoreCase(CommunicationChannel.WHATSAPP.name())) {
                        if (whatsappUsers.getStatus() == WhatsappUsers.WhatsAppStatus.NO_ACCNT) {
                            whatsappUsersRepository.updateWhatsappUsersUpdatedTime(whatsappUsers);
                        } else {
                            whatsappUsers.setStatus(WhatsappUsers.WhatsAppStatus.NO_ACCNT);
                            whatsappUsersRepository.updateWhatsappUsers(whatsappUsers);
                        }
                    }
                }
                messageAcknowledgementRepository.updateMessageAcknowledgementActorDelivery(
                        currState, null, communicationChannel,
                        communicationServiceCallbackRequest,
                        responseObject.messageAcknowledgement.getAttributes(), responseObject.messageAcknowledgement.getVersion());

                messageAcknowledgementRepository.sendEventToProducer(
                        messageAcknowledgementRepository.getAcknowledgement(responseObject.messageAcknowledgement, null,
                                currState, communicationServiceCallbackRequest, null));

                return true;
            }

            if (communicationServiceCallbackRequest.getStatus() == CommunicationServiceCallbackRequest.Status.FAIL ||
                    communicationServiceCallbackRequest.getStatus() == CommunicationServiceCallbackRequest.Status.SUBMITTED ||
                    communicationServiceCallbackRequest.getStatus() == CommunicationServiceCallbackRequest.Status.UNKNOWN
            ) {
                isUpdateMsgAck = retryService(merticsGroupName, responseObject, communicationServiceCallbackRequest, communicationChannel, referenceId, languageId);
            } else {
                switch (communicationServiceCallbackRequest.getStatus()) {
                    case SENT: //CUSTOMER_SENT
                        currState = MessageAcknowledgement.State.CUSTOMER_SENT;
                        break;
                    case SUCCESS: // CUSTOMER_DELIVERED
                        currState = MessageAcknowledgement.State.CUSTOMER_DELIVERED;
                        break;
                    case VIEW: //CUSTOMER_READ
                        currState = MessageAcknowledgement.State.CUSTOMER_READ;
                        break;
                }

                metrics.onIncrement(merticsGroupName, CommunicationServiceCallbackRequest.Status.SUCCESS.name(),
                        Constants.LABEL_TEMPLATE_LANGUAGE_STATE_LIST,
                        responseObject.messageAcknowledgement.getTempateName(),
                        languageCache.getLanguageById(responseObject.messageAcknowledgement.getLanguageId()).getName(),
                        currState.name(),
                        communicationServiceCallbackRequest.getVendorName()
                );
                if(whatsappUsers != null &&communicationChannel.equalsIgnoreCase(CommunicationChannel.WHATSAPP.name())&& whatsappUsers.getStatus()== WhatsappUsers.WhatsAppStatus.NO_ACCNT)
                {
                    whatsappUsers.setStatus(WhatsappUsers.WhatsAppStatus.OPT_IN);
                    whatsappUsersRepository.updateWhatsappUsers(whatsappUsers);
                }
                messageAcknowledgementRepository.
                        updateMessageAcknowledgementActorDelivery(
                                currState, communicationServiceCallbackRequest.getDeliveredTS(),
                                communicationChannel, communicationServiceCallbackRequest,
                                responseObject.messageAcknowledgement.getAttributes(), responseObject.messageAcknowledgement.getVersion());
                isUpdateMsgAck = true;

            }
        } catch (Exception e) {
            log.error(ISSUE_UPDATE_MSG_ACK, communicationServiceCallbackRequest,
                    communicationServiceCallbackRequest.getStatus(), e);
        } finally {
            if (responseObject != null && validateCleverTapCampaign(responseObject.messageAcknowledgement, communicationChannel)) {
                executorService.sendEvent(responseObject.messageAcknowledgement, currState.name());
            }
        }
        return isUpdateMsgAck;
    }

    private boolean retryService(String merticsGroupName, MessageAcknowledgementRepository.ResponseObject responseObject,
                                 CommunicationServiceCallbackRequest communicationServiceCallbackRequest,
                                 String communicationChannel, String referenceId, Short languageId) {
        List<MessageAcknowledgement> messageAcknowledgementList =
                messageAcknowledgementRepository.getGetMessageAcknowledgementEnableByReferenceIdAndCommunicationChannel(referenceId,
                        communicationChannel);
        int countOfRecords = messageAcknowledgementList.size();
        if (countOfRecords == 0) {
            log.error(ISSUE_NO_ENTRY_MSG_ACK, communicationChannel, referenceId);
            return false;
        } else {
            messageAcknowledgementRepository.updateMessageAcknowledgementActorDelivery(MessageAcknowledgement.State.CUSTOMER_UNDELIVERED,
                    null, communicationChannel, communicationServiceCallbackRequest,
                    responseObject.messageAcknowledgement.getAttributes(), responseObject.messageAcknowledgement.getVersion());

            messageAcknowledgementRepository.sendEventToProducer(
                    messageAcknowledgementRepository.getAcknowledgement(responseObject.messageAcknowledgement, null,
                            MessageAcknowledgement.State.CUSTOMER_UNDELIVERED, communicationServiceCallbackRequest, null));

            metrics.onIncrement(merticsGroupName,
                    "FAILED",
                    Constants.LABEL_TEMPLATE_LANGUAGE_STATE_LIST,
                    responseObject.messageAcknowledgement.getTempateName(),
                    languageCache.getLanguageById(responseObject.messageAcknowledgement.getLanguageId()).getName(),
                    MessageAcknowledgement.State.CUSTOMER_UNDELIVERED.name(),
                    communicationServiceCallbackRequest.getVendorName()
            );

            if (countOfRecords < totalRetryCount) {
                if (communicationChannel.equals(CommunicationChannel.SMS.name())) {
                    retrySMSService(responseObject, communicationServiceCallbackRequest, referenceId, languageId);
                    log.info(RETRY_REQUEST, communicationChannel, referenceId);

                }
                /*
                 TODO: retry Whatsapp Service Logic
                else if (communicationChannel.equals(CommunicationChannel.WHATSAPP.name())) {

                }
                */
            } else if (countOfRecords == totalRetryCount) {
                log.info(TRY_MULTIPLE_TIMES, totalRetryCount, communicationChannel, referenceId);
            }
            return true;
        }
    }

    private void retrySMSService(MessageAcknowledgementRepository.ResponseObject responseObject,
                                 CommunicationServiceCallbackRequest request,
                                 String reference_id, Short language_id) {

        ActorID actorID = ActorID.newBuilder().setActorId(responseObject.messageAcknowledgement.getActorId()).
                setActorType(responseObject.messageAcknowledgement.getActorType()).build();

        CommunicationEvent communicationEvent = CommunicationEvent.newBuilder().setReceiverActor(actorID)
                .addChannel(CommunicationChannel.SMS)
                .setTemplateName(responseObject.messageAcknowledgement.getTempateName()).setReferenceId(reference_id).build();

        Event<byte[], byte[]> event = new Event<>(String.valueOf(1).getBytes(), communicationEvent.toByteArray());

        DerivedCommunicationEvent derivedEvent = new
                DerivedCommunicationEvent(event, reference_id, CLIENT_ID,
                responseObject.messageAcknowledgement.getMessageContent(),
                responseObject.messageAcknowledgement.getIsUnicode(), null, null,
                CommunicationChannel.SMS, language_id,
                responseObject.messageAcknowledgement.getTempateName(),
                responseObject.messageAcknowledgement.getPlaceHolders(),
                null, null, null,  responseObject.messageAcknowledgement.getCampaignName(),
                responseObject.messageAcknowledgement.getActorId(),
                responseObject.messageAcknowledgement.getActorType()
        );

        if (responseObject.messageAcknowledgement.getVendorName().equals(VendorType.GUPSHUP.name())) {
            derivedEvent.setVendor(VendorType.KARIX);
        } else {
            derivedEvent.setVendor(VendorType.GUPSHUP);
        }
        derivedEvent.setSmsAttributes(new DerivedCommunicationEvent.SMSAttributes(request.getPhoneNo()));
        smsService.sendSms(derivedEvent);
    }

    private boolean validateCleverTapCampaign(MessageAcknowledgement messageAcknowledgement, String communicationChannel) {
        return messageAcknowledgement!= null && communicationChannel.equals(CommunicationChannel.WHATSAPP.name()) &&
                StringUtils.isNonEmpty(messageAcknowledgement.getCampaignName())
                && messageAcknowledgement.getActorType() != ActorType.NO_ACTOR
                && messageAcknowledgement.getActorId() != 0L
                && messageAcknowledgement.getCampaignName().startsWith(CLEVER_TAP_CAMPAIGN_PREFIX);
    }

    public boolean insertMessageAcknowledgments(GupshupWhatsAppIncomingCallbackRequest gupshupWhatsAppIncomingCallbackRequest){
        boolean success;
        MessageAcknowledgement messageAcknowledgement = null;
        try{
            log.info("Gupshup Incoming Callback Request {} ", gupshupWhatsAppIncomingCallbackRequest);
            HashMap<String, Object> attributeMap = new HashMap<>();
            attributeMap.put("button", gupshupWhatsAppIncomingCallbackRequest.getButton());
            attributeMap.put("name", gupshupWhatsAppIncomingCallbackRequest.getName());
            attributeMap.put("type", gupshupWhatsAppIncomingCallbackRequest.getType());
            String referenceId = UUID.randomUUID().toString();
            String text = gupshupWhatsAppIncomingCallbackRequest.getText();
            if(StringUtils.isEmpty(text)){
                text = StringUtils.isNonEmpty(gupshupWhatsAppIncomingCallbackRequest.getButton()) ? gupshupWhatsAppIncomingCallbackRequest.getButton() : "NA";
            }
            messageAcknowledgement  = MessageAcknowledgement.builder()
                .actorId(0L)
                .actorType(ActorType.NO_ACTOR)
                .mobileNumber(gupshupWhatsAppIncomingCallbackRequest.getMobile())
                .communicationChannel(Constants.WHATSAPP)
                .referenceId(referenceId)
                .messageContent(text)
                .vendorName(VendorType.GUPSHUP.name())
                .vendorMessageId(gupshupWhatsAppIncomingCallbackRequest.getReplyId())
                .state(MessageAcknowledgement.State.INCOMING_REPLY)
                .retryCount(0)
                .attributes(attributeMap)
                .actorContactId(gupshupWhatsAppIncomingCallbackRequest.getMobile())
                .vendorDeliveryTime(new Timestamp(Long.parseLong(gupshupWhatsAppIncomingCallbackRequest.getTimestamp())))
                .build();
             success = messageAcknowledgementRepository.insertMessageAcknowledgement(messageAcknowledgement);
        }catch (Exception exception){
            log.error("Error in inserting message acknowledgement {}, exception ", messageAcknowledgement, exception);
            success = false;
        }
        return success;
    }

}