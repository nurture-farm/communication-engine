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
import com.google.protobuf.InvalidProtocolBufferException;
import farm.nurture.communication.engine.Constants;
import farm.nurture.communication.engine.TimeOutConfigs;
import farm.nurture.communication.engine.cache.LanguageCache;
import farm.nurture.communication.engine.dto.HttpClientRequest;
import farm.nurture.communication.engine.event.CommunicationEventHandler;
import farm.nurture.communication.engine.event.DerivedCommunicationEvent;
import farm.nurture.communication.engine.metric.MetricGroupNames;
import farm.nurture.communication.engine.metric.MetricTracker;
import farm.nurture.communication.engine.models.MessageAcknowledgement;
import farm.nurture.communication.engine.repository.MessageAcknowledgementRepository;
import farm.nurture.communication.engine.vendor.GupShupVendor;
import farm.nurture.communication.engine.vendor.KarixVendor;
import farm.nurture.communication.engine.vendor.Vendor;
import farm.nurture.communication.engine.vendor.VendorType;
import farm.nurture.core.contracts.communication.engine.CommunicationEvent;
import farm.nurture.core.contracts.communication.engine.Placeholder;
import farm.nurture.infra.util.ApplicationConfiguration;
import farm.nurture.kafka.Producer;
import farm.nurture.util.http.TimeOutConfig;
import farm.nurture.util.http.client.NFAsyncHttpClient;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;

@Slf4j
@Singleton
public class SMSService {

    @Inject
    private NFAsyncHttpClient nfAsyncHttpClient;

    @Inject
    private TimeOutConfig timeOutConfig;

    @Inject
    private Producer producer;

    @Inject
    private MessageAcknowledgementRepository messageAcknowledgementRepository;

    @Inject
    private LanguageCache languageCache;

    @Inject
    KarixVendor karixVendor;

    @Inject
    GupShupVendor gupshupVendor;

    public void sendSms(DerivedCommunicationEvent event) {
        log.info("Sending SMS from event : {}", event);

        boolean success = false;
        MetricTracker tracker = new MetricTracker(MetricGroupNames.NF_CE, "sms_service", Constants.LABEL_TEMPLATE, Constants.LABEL_LANGUAGE,Constants.LABEL_VENDOR);
        Vendor vendor;
        try {
            if(event.getVendor() == VendorType.GUPSHUP){
                vendor = gupshupVendor;
            }else {
                vendor = karixVendor;
            }
            HttpClientRequest sendRequest = vendor.requestForSendSms(event);

            String referenceId = populateMessageAcknowledgements(event);
            if(referenceId != null) {
                CommunicationEventHandler.IN_FLIGHT_COUNTER.incrementAndGet();

                nfAsyncHttpClient.sendMessage(sendRequest.getMethod(), sendRequest.getUrl(), sendRequest.getRequestParams(), sendRequest.getHeaders(), sendRequest.getRequestBody(),
                        new SMSServiceCallback(this, producer, event, messageAcknowledgementRepository, referenceId, languageCache),  TimeOutConfigs.smsServiceTimeOutConfig());
                success = true;
            }

        } catch (Exception e) {
            log.error("Error in sending SMS for event : {}", event, e);

        } finally {
            tracker.stop(success, event.getTemplateName(), languageCache.getLanguageById(event.getLanguageId()).getName(), event.getVendor().name());
        }
    }

    private String populateMessageAcknowledgements(DerivedCommunicationEvent derivedCommunicationEvent) {

        CommunicationEvent commEvent = null;
        String referenceId = null;
        try {
            commEvent = CommunicationEvent.parseFrom((byte[]) derivedCommunicationEvent.getOriginalEvent().getMessage());
            referenceId = commEvent.getReferenceId();
        } catch (InvalidProtocolBufferException e) {
            log.error("Unable to deserialize protobuf in SMS service", e);
            return referenceId;
        }

        HashMap<String, Object> attributeMap = new HashMap<>();
        List<Placeholder> metaData = derivedCommunicationEvent.getMetaData();
        if(metaData!= null && metaData.size() > 0) {
            metaData.forEach(placeholder -> attributeMap.put("content_metadata_"+placeholder.getKey(), placeholder.getValue()));
        }

        MessageAcknowledgement messageAcknowledgement = MessageAcknowledgement.builder()
                .actorId(commEvent.getReceiverActor().getActorId())
                .actorType(commEvent.getReceiverActor().getActorType())
                .mobileNumber(derivedCommunicationEvent.getSmsAttributes().getMobileNumber())
                .communicationChannel(Constants.SMS)
                .referenceId(referenceId)
                .tempateName(derivedCommunicationEvent.getTemplateName())
                .languageId(derivedCommunicationEvent.getLanguageId())
                .messageContent(derivedCommunicationEvent.getContent())
                .isUnicode(derivedCommunicationEvent.isUnicode())
                .vendorName(derivedCommunicationEvent.getVendor().name())
                .vendorMessageId("")
                .state(MessageAcknowledgement.State.VENDOR_UNDELIVERED)
                .retryCount(derivedCommunicationEvent.getRetryCount())
                .placeHolders(derivedCommunicationEvent.getPlaceholders())
                .parentReferenceId(derivedCommunicationEvent.getParentReferenceId())
                .attributes(attributeMap)
                .campaignName(derivedCommunicationEvent.getCampaignName())
                .actorContactId(derivedCommunicationEvent.getSmsAttributes().getMobileNumber())
                .build();
        messageAcknowledgementRepository.insertMessageAcknowledgement(messageAcknowledgement);
        return referenceId;
    }



}
