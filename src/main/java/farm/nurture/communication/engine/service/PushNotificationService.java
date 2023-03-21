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
import farm.nurture.communication.engine.event.CommunicationEventHandler;
import farm.nurture.communication.engine.event.DerivedCommunicationEvent;
import farm.nurture.communication.engine.metric.MetricGroupNames;
import farm.nurture.communication.engine.metric.MetricTracker;
import farm.nurture.communication.engine.models.MessageAcknowledgement;
import farm.nurture.communication.engine.repository.MessageAcknowledgementRepository;
import farm.nurture.core.contracts.common.enums.PushNotificationType;
import farm.nurture.core.contracts.communication.engine.CommunicationEvent;
import farm.nurture.core.contracts.communication.engine.Placeholder;
import farm.nurture.infra.util.ApplicationConfiguration;
import farm.nurture.kafka.Producer;
import farm.nurture.util.http.HttpUtils;
import farm.nurture.util.http.TimeOutConfig;
import farm.nurture.util.http.client.NFAsyncHttpClient;
import lombok.extern.slf4j.Slf4j;

import java.sql.Time;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Singleton
public class PushNotificationService {

    String KEY = "key=";
    String TITLE = "title";
    String BODY = "body";
    String HIGH = "high";

    public static final String vendor = "Firebase";

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

    public void sendPushNotification(DerivedCommunicationEvent event) {
        log.info("Sending PN from event : {}", event);
        boolean success = false;
        MetricTracker tracker = new MetricTracker(MetricGroupNames.NF_CE, "pn_service",Constants.LABEL_TEMPLATE, Constants.LABEL_LANGUAGE);

        try {
            ApplicationConfiguration config = ApplicationConfiguration.getInstance();
            String url = config.get("fmc.url");
            FCMData fcmData = getFCMData(event);

            Map<String, String> headers = new HashMap<>();
            headers.put(Constants.CONTENT_TYPE, Constants.APPLICATION_JSON);
            headers.put(Constants.AUTHORIZATION, KEY + event.getPNAttributes().getApiKey());

            String referenceId = populateMessageAcknowledgements(event);
            if(referenceId != null) {
                CommunicationEventHandler.IN_FLIGHT_COUNTER.incrementAndGet();

                nfAsyncHttpClient.sendMessage(HttpUtils.HttpMethod.POST, url, null, headers, fcmData, new PushNotificationServiceCallback(this, producer, event, messageAcknowledgementRepository,referenceId, languageCache), TimeOutConfigs.pushNotificationServiceTimeOutConfig());
                success = true;
            }
        } catch (Exception e) {
            log.error("Error in sending PN for event : {}", event, e);

        } finally {
            tracker.stop(success,event.getTemplateName(), languageCache.getLanguageById(event.getLanguageId()).getName());
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

        Object apiKey = derivedCommunicationEvent.getPNAttributes().getApiKey();
        HashMap<String, Object> attributeMap = new HashMap<>();
        attributeMap.put("api_key", apiKey);
        List<Placeholder> metaData = derivedCommunicationEvent.getMetaData();
        if(metaData!= null && metaData.size() > 0) {
            metaData.forEach(placeholder -> attributeMap.put("content_metadata_"+placeholder.getKey(), placeholder.getValue()));
        }

        MessageAcknowledgement messageAcknowledgement = MessageAcknowledgement.builder()
                .actorId(commEvent.getReceiverActor().getActorId())
                .actorType(commEvent.getReceiverActor().getActorType())
                .communicationChannel(Constants.APP_NOTIFICATION)
                .referenceId(referenceId)
                .tempateName(derivedCommunicationEvent.getTemplateName())
                .languageId(derivedCommunicationEvent.getLanguageId())
                .messageContent(derivedCommunicationEvent.getContent())
                .isUnicode(derivedCommunicationEvent.isUnicode())
                .vendorName(vendor)
                .vendorMessageId("")
                .state(MessageAcknowledgement.State.VENDOR_UNDELIVERED)
                .retryCount(derivedCommunicationEvent.getRetryCount())
                .placeHolders(derivedCommunicationEvent.getPlaceholders())
                .attributes(attributeMap)
                .parentReferenceId(derivedCommunicationEvent.getParentReferenceId())
                .campaignName(derivedCommunicationEvent.getCampaignName())
                .actorContactId(derivedCommunicationEvent.getPNAttributes().getAppToken())
                .build();

        messageAcknowledgementRepository.insertMessageAcknowledgement(messageAcknowledgement);
        return referenceId;
    }

    private FCMData getFCMData(DerivedCommunicationEvent event) {
        Map<String, String> data = new HashMap<>();
        data.put(TITLE, event.getPNAttributes().getTitle());
        data.put(BODY, event.getContent());
        List<Placeholder> metaData = event.getMetaData();
        if(metaData!= null && metaData.size() > 0) {
            metaData.forEach(placeholder -> data.put(placeholder.getKey(), placeholder.getValue()));
        }

        FCMData fcmData = new FCMData();
        fcmData.setTo(event.getPNAttributes().getAppToken());
        fcmData.setPriority(HIGH);
        if(event.getPNAttributes().getPushNotificationType() == PushNotificationType.NOTIFICATION) {
            fcmData.setNotification(data);
        } else if (event.getPNAttributes().getPushNotificationType() == PushNotificationType.DATA) {
            fcmData.setData(data);
        } else {
            fcmData.setData(data);
            fcmData.setNotification(data);
        }
        return fcmData;
    }
}
