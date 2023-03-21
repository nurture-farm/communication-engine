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

import com.google.common.base.Charsets;
import farm.nurture.communication.engine.Constants;
import farm.nurture.communication.engine.cache.LanguageCache;
import farm.nurture.communication.engine.dto.KarixSmsResponse;
import farm.nurture.communication.engine.event.CommunicationEventHandler;
import farm.nurture.communication.engine.event.DerivedCommunicationEvent;
import farm.nurture.communication.engine.metric.MetricGroupNames;
import farm.nurture.communication.engine.metric.Metrics;
import farm.nurture.communication.engine.models.MessageAcknowledgement;
import farm.nurture.communication.engine.repository.MessageAcknowledgementRepository;
import farm.nurture.communication.engine.vendor.VendorType;
import farm.nurture.core.contracts.common.enums.CommunicationChannel;
import farm.nurture.infra.metrics.IMetricCounter;
import farm.nurture.infra.util.ApplicationConfiguration;
import farm.nurture.kafka.Producer;
import farm.nurture.util.http.client.BaseHttpResponseException;
import farm.nurture.util.serializer.Serializer;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

@Slf4j
public class SMSServiceCallback implements FutureCallback<HttpResponse> {

    private SMSService smsService;

    private Producer producer;

    private DerivedCommunicationEvent event;

    private MessageAcknowledgementRepository messageAcknowledgementRepository;

    private String referenceId;

    private LanguageCache languageCache;

    Metrics metrics = Metrics.getInstance();


    public SMSServiceCallback(SMSService smsService, Producer producer, DerivedCommunicationEvent event, MessageAcknowledgementRepository messageAcknowledgementRepository, String referenceId, LanguageCache languageCache) {
        this.smsService = smsService;
        this.producer = producer;
        this.event = event;
        this.messageAcknowledgementRepository = messageAcknowledgementRepository;
        this.referenceId = referenceId;
        this.languageCache = languageCache;
    }

    private void processError(HttpEntity entity, int statusCode) throws IOException {
        IMetricCounter counter = metrics.getIMetricCounter(MetricGroupNames.NF_CE_SMS_SERVICE, "api_exception", "status_code");
        counter.increment(String.valueOf(statusCode));

        String responseBody = null;
        if (entity != null) {
            responseBody = EntityUtils.toString(entity, Charsets.UTF_8);
        }

        log.error("Got error in sending SMS for event : {}, status code: {}, responseBody: {}", event, statusCode, responseBody);
        if (BaseHttpResponseException.Family.familyOf(statusCode) != BaseHttpResponseException.Family.CLIENT_ERROR) {
            retryEvent();
        }
    }

    private void retryEvent() {
        event.setRetryCount(event.getRetryCount() + 1);

        ApplicationConfiguration config = ApplicationConfiguration.getInstance();
        int maxRetries = config.getInt("event.max.retries", 3);
        String dlq = config.get("kafka.communication.event.dl.topic");

        if (event.getRetryCount() < maxRetries) {
            if (event.getVendor() == VendorType.GUPSHUP) {
                event.setVendor(VendorType.KARIX);
            } else {
                event.setVendor(VendorType.GUPSHUP);
            }
            this.smsService.sendSms(event);
        } else {
            log.error("Max retries exceeded for event : {}. Putting in DLQ", event);
            this.producer.send(dlq, event.getOriginalEvent());
        }
    }

    @Override
    public void completed(HttpResponse response) {
        CommunicationEventHandler.IN_FLIGHT_COUNTER.decrementAndGet();
        try {
            int statusCode = response.getStatusLine().getStatusCode();
            HttpEntity entity = response.getEntity();

            if (BaseHttpResponseException.Family.familyOf(statusCode) == BaseHttpResponseException.Family.SUCCESSFUL) {

                String result = EntityUtils.toString(entity);

                if (event.getVendor() == VendorType.GUPSHUP) {
                    if (result.contains("error")) {
                        processErrorResponse(result);
                    } else {
                        String vendorMessageId = result.substring(result.lastIndexOf('|') + 2).trim();
                        processSuccessResponse(vendorMessageId, result);
                    }
                } else {
                    KarixSmsResponse karixSmsResponse = Serializer.DEFAULT_JSON_SERIALIZER.deserialize(result, KarixSmsResponse.class);
                    if (karixSmsResponse.getStatus().getCode()!=200) {
                        processErrorResponse(result);
                    }else {
                        String vendorMessageId = karixSmsResponse.getAckid();
                        processSuccessResponse(vendorMessageId, result);
                    }
                }

            } else {
                log.error("Error in sending SMS for event : {}", event);
                this.processError(entity, statusCode);
            }

        } catch (Exception e) {
            log.error("Exception in processing SMSServiceCallback for event : {}, error : {}", event, e.getMessage(), e);
        }
    }

    private void processSuccessResponse(String vendorMessageId, String result) {

        messageAcknowledgementRepository.updateMessageAcknowledgementVenodrDelivery(MessageAcknowledgement.State.VENDOR_DELIVERED, Timestamp.valueOf(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())), vendorMessageId, referenceId, null, CommunicationChannel.SMS.toString());
        log.info("Successfully delivered SMS for event : {}, response : {}", event, result);
        metrics.onIncrement(MetricGroupNames.NF_CE_SMS_SERVICE, "api_successful", Constants.LABEL_TEMPLATE_LANGUAGE_STATE_LIST, event.getTemplateName(),
                languageCache.getLanguageById(event.getLanguageId()).getName(), MessageAcknowledgement.State.VENDOR_DELIVERED.name(),
                event.getVendor().name());
    }

    private void processErrorResponse(String result) {

        Map<String, String> attributes = Map.of("error", result);
        messageAcknowledgementRepository.updateMessageAcknowledgementVenodrDelivery(MessageAcknowledgement.State.VENDOR_UNDELIVERED, null, "", referenceId, attributes, CommunicationChannel.SMS.toString());
        metrics.onIncrement(MetricGroupNames.NF_CE_SMS_SERVICE, "api_error_response", Constants.LABEL_TEMPLATE_LANGUAGE_STATE_LIST, event.getTemplateName(), languageCache.getLanguageById(event.getLanguageId()).getName(), MessageAcknowledgement.State.VENDOR_DELIVERED.name(),
                event.getVendor().name());
        logError(result);
        retryEvent();
    }

    private void logError(String result) {
        log.error("Error in sending SMS for event : {}, error : {}", event, result);

        String[] splits = result.split("\\|");
        String errorCode = splits.length > 1 ? splits[1].trim() : "";
        String[] labels = {"error_code", Constants.LABEL_TEMPLATE, Constants.LABEL_LANGUAGE, Constants.LABEL_STATE};
        IMetricCounter counter = metrics.getIMetricCounter(MetricGroupNames.NF_CE_SMS_SERVICE, "api_error", labels);
        counter.increment(errorCode, event.getTemplateName(), languageCache.getLanguageById(event.getLanguageId()).getName(), MessageAcknowledgement.State.VENDOR_UNDELIVERED.name());
    }

    @Override
    public void failed(Exception e) {
        CommunicationEventHandler.IN_FLIGHT_COUNTER.decrementAndGet();
        log.error("SMSService call failed for event : {}, Error : {}", event, e.getMessage(), e);
        metrics.onIncrement(MetricGroupNames.NF_CE_SMS_SERVICE, "api_failed");
        retryEvent();
    }

    @Override
    public void cancelled() {
        CommunicationEventHandler.IN_FLIGHT_COUNTER.decrementAndGet();
        log.error("SMSService call cancelled for event : {}", event);
        metrics.onIncrement(MetricGroupNames.NF_CE_SMS_SERVICE, "api_cancelled");
    }
}
