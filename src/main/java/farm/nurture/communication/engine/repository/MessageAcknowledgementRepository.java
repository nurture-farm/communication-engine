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

package farm.nurture.communication.engine.repository;

import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import farm.nurture.communication.engine.Constants;
import farm.nurture.communication.engine.dao.MessageAcknowledgementReadBase;
import farm.nurture.communication.engine.dto.CommunicationServiceCallbackRequest;
import farm.nurture.communication.engine.helper.Utils;
import farm.nurture.communication.engine.kafka.KafkaProducerWrapperService;
import farm.nurture.communication.engine.metric.MetricGroupNames;
import farm.nurture.communication.engine.metric.MetricTracker;
import farm.nurture.communication.engine.metric.Metrics;
import farm.nurture.communication.engine.models.MessageAcknowledgement;
import farm.nurture.core.contracts.common.ActorID;
import farm.nurture.core.contracts.common.Attribs;
import farm.nurture.core.contracts.common.enums.*;
import farm.nurture.infra.util.ApplicationConfiguration;
import farm.nurture.infra.util.StringUtils;
import farm.nurture.laminar.core.io.sql.dao.WriteBase;
import farm.nurture.util.serializer.Serializer;
import lombok.extern.slf4j.Slf4j;

import java.sql.Timestamp;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

@Slf4j
@Singleton
public class MessageAcknowledgementRepository {

    @Inject
    private KafkaProducerWrapperService kafkaProducerWrapperService;

    private static final String partitionPlaceHolder = "@partition@";

    private static final String getGetMessageAcknowledgementEnableByReferenceIdAndCommunicationChannelSql="SELECT * FROM message_acknowledgements where reference_id = ? AND communication_channel = ?";

    private static final String getMessageAcknowledgementEnableByVendorMessageIdSql = "SELECT * FROM message_acknowledgements where communication_channel = ? AND vendor_message_id = ?";

    private static final String getGetMessageAcknowledgementEnableByReferenceIdAndCommunicationChannelAndStateSql = "SELECT * FROM message_acknowledgements where reference_id = ? AND communication_channel = ? AND vendor_message_id = ?";

    private static final String updateMessageAcknowledgementVenodrDeliverySql = "UPDATE message_acknowledgements PARTITION ("+partitionPlaceHolder+") SET state = ?, vendor_delivery_time = ?, vendor_message_id = ?, attributes = ?, version = version + 1 where reference_id = ? AND communication_channel = ? AND vendor_message_id = '' and version = ?";

    private static final String updateMessageAcknowledgementActorDeliverySql = "UPDATE message_acknowledgements SET state = ?, actor_delivery_time = ?, attributes = ?, version = version + 1 where communication_channel = ? AND vendor_name = ? AND vendor_message_id = ? AND version = ?";

    private static final String insertMessageAcknowledgementSql = "INSERT INTO message_acknowledgements(actor_id, actor_type, communication_channel, reference_id, template_name, language_id, message_content, is_unicode, vendor_name, vendor_message_id, state, retry_count, placeholders, attributes, vendor_delivery_time, actor_delivery_time, contact_type, actor_contact_id, parent_reference_id, campaign_name, created_date) " +
            "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE vendor_message_id = ?, state = ? , retry_count = ?, attributes = ?, vendor_delivery_time = ?, actor_delivery_time = ?, vendor_name = ? ";

    private static final String getMessageAcknowledgementSql = "SELECT * FROM message_acknowledgements where ";

    //"communication_channel = ? and created_at >= ? and created_at <= ? and template_name like ? order by created_at";

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final DateTimeFormatter timestampFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private String getFormattedTimestamp(long milliseconds) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(milliseconds), Constants.DEFAULT_ZONE_ID).format(timestampFormatter);
    }

    private String getFormattedDate(long milliseconds) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(milliseconds), Constants.DEFAULT_ZONE_ID).format(formatter);
    }

    public List<MessageAcknowledgement> getGetMessageAcknowledgementEnableByReferenceIdAndCommunicationChannel(String referenceId, String communicationChannel) {
        boolean success = false;
        MetricTracker tracker = new MetricTracker(MetricGroupNames.NF_CE_MYSQL, "select_message_acknowledgements_by_reference_id");
        MessageAcknowledgementReadBase readBase = new MessageAcknowledgementReadBase();
        List<MessageAcknowledgement> acknowledgements = new ArrayList<>();
        try {
            acknowledgements = readBase.execute(getGetMessageAcknowledgementEnableByReferenceIdAndCommunicationChannelSql,
                    Arrays.asList(referenceId, communicationChannel).toArray());
            success = true;

        } catch (Exception e) {
            log.error("Error in fetching MessageAcknowledgement for referenceId : {} and communicationChannel : {}", referenceId, communicationChannel, e);

        } finally {
            tracker.stop(success);
        }
        return acknowledgements;
    }

    public ResponseObject getMessageAcknowledgementByVendorMessageId(String communicationChannel, String vendorMessageId) {
        boolean success = false;
        MetricTracker tracker = new MetricTracker(MetricGroupNames.NF_CE_MYSQL, "select_message_acknowledgements_by_vendor_message_id");
        MessageAcknowledgementReadBase readBase = new MessageAcknowledgementReadBase();
        MessageAcknowledgement messageAcknowledgement = null;
        try {
            messageAcknowledgement = readBase.selectByUniqueKey(getMessageAcknowledgementEnableByVendorMessageIdSql, Arrays.asList(communicationChannel, vendorMessageId).toArray());
            success = true;

        } catch (Exception e) {
            log.error("Error in fetching MessageAcknowledgement for vendorMessageId : {}", vendorMessageId, e);

        } finally {
            tracker.stop(success);
        }
        return new ResponseObject(messageAcknowledgement, success);
    }

    public ResponseObject getGetMessageAcknowledgementEnableByReferenceIdAndCommunicationChannelAndStateSql(String referenceId, String communicationChannel, String vendorMessageId) {
        boolean success = false;
        MetricTracker tracker = new MetricTracker(MetricGroupNames.NF_CE_MYSQL, "select_message_acknowledgements_by_reference_id_and_state");
        MessageAcknowledgementReadBase readBase = new MessageAcknowledgementReadBase();
        MessageAcknowledgement messageAcknowledgement = null;
        try {
            messageAcknowledgement = readBase.selectByUniqueKey(getGetMessageAcknowledgementEnableByReferenceIdAndCommunicationChannelAndStateSql, Arrays.asList(referenceId, communicationChannel, vendorMessageId).toArray());
            success = true;

        } catch (Exception e) {
            log.error("Error in fetching MessageAcknowledgement for referenceId : {}, communicationChannel: {}, vendorMessageId: {}",  referenceId, communicationChannel, vendorMessageId, e);

        } finally {
            tracker.stop(success);
        }
        return new ResponseObject(messageAcknowledgement, success);
    }

    public boolean insertMessageAcknowledgement(MessageAcknowledgement messageAcknowledgement) {

        boolean success = false;
        MetricTracker tracker = new MetricTracker(MetricGroupNames.NF_CE_MYSQL, "insert_message_acknowledgements");
        Map<String, Object> placeHoldersMap = messageAcknowledgement.getPlaceHolders();
        String placeHolders = placeHoldersMap == null || placeHoldersMap.isEmpty() ? null :
                Serializer.DEFAULT_JSON_SERIALIZER.serialize(placeHoldersMap);
        String contactType = null;
        String actorContactId;
        if (CommunicationChannel.APP_NOTIFICATION.name().equals(messageAcknowledgement.getCommunicationChannel())) {
            contactType = ContactType.FCM_TOKEN.name();
        } else if (CommunicationChannel.EMAIL.name().equals(messageAcknowledgement.getCommunicationChannel())) {
            contactType = ContactType.EMAIL_ID.name();
        } else if (CommunicationChannel.SMS.name().equals(messageAcknowledgement.getCommunicationChannel()) ||
                CommunicationChannel.WHATSAPP.name().equals(messageAcknowledgement.getCommunicationChannel())) {
            contactType = ContactType.MOBILE_NUMBER.name();
        }
        actorContactId = messageAcknowledgement.getActorContactId();
        try {
            WriteBase writeBase = new WriteBase();
            String attributes = messageAcknowledgement.getAttributes() == null || messageAcknowledgement.getAttributes().isEmpty() ? null :
                    Serializer.DEFAULT_JSON_SERIALIZER.serialize(messageAcknowledgement.getAttributes());
            String currentDate = getFormattedDate(Instant.now().toEpochMilli());
            log.info("Insert into message ack sql ");
            writeBase.insert(insertMessageAcknowledgementSql,
                    Arrays.asList(messageAcknowledgement.getActorId(), messageAcknowledgement.getActorType().name(), messageAcknowledgement.getCommunicationChannel(),
                    messageAcknowledgement.getReferenceId(), messageAcknowledgement.getTempateName(), messageAcknowledgement.getLanguageId(), messageAcknowledgement.getMessageContent(), messageAcknowledgement.getIsUnicode(),
                    messageAcknowledgement.getVendorName(), messageAcknowledgement.getVendorMessageId(), messageAcknowledgement.getState().toString(), messageAcknowledgement.getRetryCount(), placeHolders, attributes,
                    messageAcknowledgement.getVendorDeliveryTime(), messageAcknowledgement.getActorDeliveryTime(), contactType , actorContactId, messageAcknowledgement.getParentReferenceId(), messageAcknowledgement.getCampaignName(),
                    currentDate, messageAcknowledgement.getVendorMessageId(), messageAcknowledgement.getState().toString(),
                    messageAcknowledgement.getRetryCount(), attributes, messageAcknowledgement.getVendorDeliveryTime(),
                            messageAcknowledgement.getActorDeliveryTime(), messageAcknowledgement.getVendorName() ));
            log.info("Insertion into message acknowledgement successful");
            success = true;

        } catch (Exception e) {
            log.error("Error in inserting MessageAcknowledgement : {}", messageAcknowledgement, e);

        } finally {
            tracker.stop(success);
        }
        return success;
    }

    public boolean updateMessageAcknowledgementVenodrDelivery(MessageAcknowledgement.State newState, Timestamp vendorDeliveryTime, String vendorMessageId, String referenceId,
                                                              Map<String, String> attributesMap, String communicationChannel) {
        boolean success = false;
        MetricTracker tracker = new MetricTracker(MetricGroupNames.NF_CE_MYSQL, "update_message_acknowledgements_vendor_delivery");
        MessageAcknowledgementReadBase readBase = new MessageAcknowledgementReadBase();
        try {
            WriteBase writeBase = new WriteBase();
            log.info("Value for MessageAcknowledgement in updateMessageAcknowledgementVenodrDelivery, communicationChannel : {}, vendorMessageId : {}, newState : {}",
                    communicationChannel, vendorMessageId,  newState);
            MessageAcknowledgementRepository.ResponseObject responseObject = getGetMessageAcknowledgementEnableByReferenceIdAndCommunicationChannelAndStateSql(referenceId,
                    communicationChannel, "");
            log.info("Value for MessageAcknowledgement in updateMessageAcknowledgementVenodrDelivery : {}", responseObject.messageAcknowledgement);
            Map<String, Object> existingAttributes = Utils.mergeAttributes(attributesMap, responseObject.messageAcknowledgement.getAttributes());

            String attributes = existingAttributes == null || existingAttributes.isEmpty() ? null :
                    Serializer.DEFAULT_JSON_SERIALIZER.serialize(existingAttributes);

            String query = updateMessageAcknowledgementVenodrDeliverySql.replace(partitionPlaceHolder, "p"+responseObject.messageAcknowledgement.getCreatedDate().toString().replace("-",""));

            if (responseObject.messageAcknowledgement != null) {
                writeBase.execute(query, Arrays.asList(newState.toString(), vendorDeliveryTime, vendorMessageId, attributes, referenceId, communicationChannel,responseObject.messageAcknowledgement.getVersion()));
                responseObject.messageAcknowledgement.setAttributes(existingAttributes);
                if(newState!= MessageAcknowledgement.State.CUSTOMER_DELIVERED && newState!= MessageAcknowledgement.State.VENDOR_DELIVERED) {
                    sendEventToProducer(getAcknowledgement(responseObject.messageAcknowledgement, null, newState, null, vendorDeliveryTime));
                }
            }
            success = true;

        } catch (Exception e) {
            log.error("Error in updating MessageAcknowledgement vendor delivery, vendormessageId : {}", vendorMessageId, e);

        } finally {
            tracker.stop(success);
        }
        return success;
    }

    public boolean updateMessageAcknowledgementActorDelivery(MessageAcknowledgement.State newState, Timestamp actorDeliveryTime,
                                                             String communicationChannel, CommunicationServiceCallbackRequest request,
                                                             Map<String, Object> map, int version) {
        boolean success = false;
        Map<String, Object> attributeValueMap = null;
        if (map != null) attributeValueMap = new HashMap<>(map);
        else attributeValueMap = new HashMap<>();
        attributeValueMap.put("status", request.getStatus());
        attributeValueMap.put("cause", request.getCause());
        attributeValueMap.put("Errorcode", request.getErrCode());
        attributeValueMap.put("No of frags", request.getNoOfFrags());
        MetricTracker tracker = new MetricTracker(MetricGroupNames.NF_CE_MYSQL, "update_message_acknowledgements_actor_delivery");

        try {
            WriteBase writeBase = new WriteBase();
            writeBase.execute(updateMessageAcknowledgementActorDeliverySql, Arrays.asList(newState.toString(), actorDeliveryTime,
                    Serializer.DEFAULT_JSON_SERIALIZER.serialize(attributeValueMap), communicationChannel,
                    request.getVendorName(), request.getExternalId(), version));
            success = true;

        } catch (Exception e) {
            log.error("Error in updating MessageAcknowledgement actor delivery, vendormessageId : {}", request.getExternalId(), e);

        } finally {
            tracker.stop(success);
        }
        return success;
    }

    public class ResponseObject {
        public MessageAcknowledgement messageAcknowledgement;

        public boolean success;

        public ResponseObject(MessageAcknowledgement messageAcknowledgement, boolean success) {
            this.messageAcknowledgement = messageAcknowledgement;
            this.success = success;
        }
    }

    public List<MessageAcknowledgement> getMessageAcknowledgementByDuration(List<CommunicationChannel> communicationChannel, Timestamp startTime, Timestamp endTime, String templateLike, String mobileNumber, String referenceId, Integer limit, Integer offset, ResponseOrderType orderType) {
        boolean success = false;
        MetricTracker tracker = new MetricTracker(MetricGroupNames.NF_CE_MYSQL, "select_message_acknowledgements_by_duration_sql");
        MessageAcknowledgementReadBase readBase = new MessageAcknowledgementReadBase();
        List<MessageAcknowledgement> messageAcknowledgements = null;
        List<Object> params = new ArrayList<>();
        String query = getMessageAcknowledgementSql;
        query = getMessageAcknowledgementQuery(params, query, communicationChannel, startTime, endTime, templateLike, mobileNumber, referenceId, limit, offset, orderType);
        try {
            messageAcknowledgements = readBase.execute(query, params);
            success = true;

        } catch (Exception e) {
            log.error("Error in fetching MessageAcknowledgement for duration : {}", endTime, e);

        } finally {
            tracker.stop(success);
        }
        return messageAcknowledgements;
    }

    private String getMessageAcknowledgementQuery(List<Object> params, String query, List<CommunicationChannel> communicationChannel, Timestamp startTime, Timestamp endTime, String templateLike, String mobileNumber, String referenceId, Integer limit, Integer offset, ResponseOrderType orderType) {

        boolean isFirst = true;
        Timestamp defaultTime = new java.sql.Timestamp(0);
        if(communicationChannel.size()>0) {
            query += "communication_channel in (";
            StringBuilder queryBuilder = new StringBuilder(query);
            for(CommunicationChannel channel: communicationChannel) {
                if(!isFirst) queryBuilder.append(",");
                queryBuilder.append("'").append(channel.name()).append("'");
                isFirst = false;
            }
            query = queryBuilder.toString();
            query += ")";
        }
        if(!startTime.equals(defaultTime)) {
            if(!isFirst) query += " and ";
            query += "created_at >= ?"; //ADD partition check
            params.add(startTime);
            isFirst = false;
        }
        if(!endTime.equals(defaultTime)) {
            if(!isFirst) query += " and ";
            query += "created_at <= ?";
            params.add(endTime);
            isFirst = false;
        }
        if(StringUtils.isNonEmpty(templateLike)) {
            if(!isFirst) query += " and ";
            query += "template_name like ?";
            params.add(templateLike);
            isFirst = false;
        }
        if(StringUtils.isNonEmpty(mobileNumber)) {
            if(!isFirst) query += " and ";
            query += "actor_contact_id = ?";
            params.add(mobileNumber);
            isFirst = false;
        }
        if(StringUtils.isNonEmpty(referenceId)) {
            if(!isFirst) query += " and ";
            query += "reference_id = ?";
            params.add(referenceId);
            isFirst = false;
        }
        query += " order by created_at";
        if(orderType==ResponseOrderType.NO_RESPONSE_ORDER || orderType==ResponseOrderType.ASCENDING) {
            query += " asc";
        } else {
            query += " desc";
        }
        if(limit>0 && offset>=0) {
            query += " limit ?,?";
            params.add(limit*offset);
            params.add(limit);
        }
        return query;
    }

    Retryer<Boolean> pushToKafkaRetryer = RetryerBuilder.<Boolean>newBuilder().retryIfRuntimeException()
            .withWaitStrategy(WaitStrategies.exponentialWait(100, TimeUnit.MILLISECONDS))
            .withStopStrategy(StopStrategies.stopAfterAttempt(2)).build();

    public boolean sendEventToProducer(MessageAcknowledgement messageAcknowledgement) {

        boolean success = false;
        String topic = ApplicationConfiguration.getInstance().get("kafka.communication.message.acknowledgements.topic",
                "communication_message_acknowledgements");

        try {
            Callable<Boolean> pushToKafkaCallable = new Callable<Boolean>() {
                public Boolean call() throws Exception {
                    byte[] kafkaMessage = acknowledgement(messageAcknowledgement).toByteArray();
                    kafkaProducerWrapperService.pushByteArrayMessage(kafkaMessage, topic, null, 3);
                    return true;
                }
            };

            pushToKafkaRetryer.call(pushToKafkaCallable);
            success = true;

        } catch (Exception e) {
            log.error("Unable to push event in kafka producer for topic : {}", topic, e);
            Metrics.getInstance().onIncrement(MetricGroupNames.NF_CE_KAFKA_PRODUCER, "send_message_akg_event_failed");
        }
        return success;
    }

    private farm.nurture.core.contracts.communication.engine.MessageAcknowledgement acknowledgement(MessageAcknowledgement messageAcknowledgement) {
        log.info("Creating Kafka message for msgAkg using MessageAcknowledgement : {}", messageAcknowledgement);

        farm.nurture.core.contracts.communication.engine.MessageAcknowledgement.Builder builder = farm.nurture.core.contracts.communication.engine.MessageAcknowledgement.newBuilder();
        if (messageAcknowledgement.getId() != null) {
            builder.setId(messageAcknowledgement.getId());
        }
        if (messageAcknowledgement.getActorId() != null && ActorType.NO_ACTOR != messageAcknowledgement.getActorType()) {
            builder.setActor(ActorID.newBuilder().setActorId(messageAcknowledgement.getActorId()).setActorType(ActorType.valueOf(messageAcknowledgement.getActorType().name())).build());
        }
        builder.setChannel(CommunicationChannel.valueOf(messageAcknowledgement.getCommunicationChannel()));
        if (CommunicationChannel.APP_NOTIFICATION.name().equals(messageAcknowledgement.getCommunicationChannel())) {
            builder.setContactType(ContactType.FCM_TOKEN);
            builder.setActorContactId(messageAcknowledgement.getActorContactId());
        } else if (CommunicationChannel.EMAIL.name().equals(messageAcknowledgement.getCommunicationChannel())) {
            builder.setContactType(ContactType.EMAIL_ID);
            builder.setActorContactId(messageAcknowledgement.getActorContactId());
        } else if (CommunicationChannel.SMS.name().equals(messageAcknowledgement.getCommunicationChannel()) ||
                CommunicationChannel.WHATSAPP.name().equals(messageAcknowledgement.getCommunicationChannel())) {
            builder.setContactType(ContactType.MOBILE_NUMBER);
            builder.setActorContactId(messageAcknowledgement.getActorContactId());
        }
        if (!StringUtils.isEmpty(messageAcknowledgement.getReferenceId())) {
            builder.setReferenceId(messageAcknowledgement.getReferenceId());
        }
        if (!StringUtils.isEmpty(messageAcknowledgement.getTempateName())) {
            builder.setTemplateName(messageAcknowledgement.getTempateName());
        }
        if (!StringUtils.isEmpty(messageAcknowledgement.getVendorName())) {
            builder.setVendorName(messageAcknowledgement.getVendorName());
        }
        if (!StringUtils.isEmpty(messageAcknowledgement.getVendorMessageId())) {
            builder.setVendorMessageId(messageAcknowledgement.getVendorMessageId());
        }
        builder.setState(CommunicationState.valueOf(messageAcknowledgement.getState().name()));
        if (messageAcknowledgement.getRetryCount() != null) {
            builder.setRetryCount(messageAcknowledgement.getRetryCount());
        }
        if (messageAcknowledgement.getVendorDeliveryTime() != null) {
            builder.setVendorDeliveryTime(com.google.protobuf.Timestamp.newBuilder().setNanos(messageAcknowledgement.getVendorDeliveryTime().getNanos()).build());
        }
        if (messageAcknowledgement.getActorDeliveryTime() != null) {
            builder.setActorDeliveryTime(com.google.protobuf.Timestamp.newBuilder().setNanos(messageAcknowledgement.getActorDeliveryTime().getNanos()).build());
        }
        if (messageAcknowledgement.getCreatedAt() != null) {
            builder.setCreatedAt(com.google.protobuf.Timestamp.newBuilder().setNanos(messageAcknowledgement.getCreatedAt().getNanos()).build());
        }
        if (messageAcknowledgement.getUpdatedAt() != null) {
            builder.setUpdatedAt(com.google.protobuf.Timestamp.newBuilder().setNanos(messageAcknowledgement.getUpdatedAt().getNanos()).build());
        }
        if (!StringUtils.isEmpty(messageAcknowledgement.getParentReferenceId())) {
            builder.setParentReferenceId(messageAcknowledgement.getParentReferenceId());
        }
        if (!StringUtils.isEmpty(messageAcknowledgement.getCampaignName())) {
            builder.setCampaignName(messageAcknowledgement.getCampaignName());
        }
        if (messageAcknowledgement.getAttributes() != null) {
            for (Map.Entry<String, Object> entry : messageAcknowledgement.getAttributes().entrySet()) {
                if(entry.getValue() != null) {
                    builder.addAttributes(Attribs.newBuilder().setKey(entry.getKey()).setValue(String.valueOf(entry.getValue())));
                }
            }
        }

        return builder.build();
    }

    public MessageAcknowledgement getAcknowledgement(MessageAcknowledgement messageAcknowledgement, Timestamp actorDeliveryTime,
                                                     MessageAcknowledgement.State state, CommunicationServiceCallbackRequest request, Timestamp vendorDeliveryTime) {
        log.info("Creating updated MessageAcknowledgement using oldMessageAcknowledgement : {}, ActorDeliveryTime : {}, " +
                        "MessageAcknowledgement.State : {}, CommunicationServiceCallbackRequest : {}, VendorDeliveryTime : {}", messageAcknowledgement,
                actorDeliveryTime, state, request, vendorDeliveryTime);

        MessageAcknowledgement.MessageAcknowledgementBuilder builder = MessageAcknowledgement.builder();
        if (!StringUtils.isEmpty(messageAcknowledgement.getVendorName())) {
            builder.vendorName(messageAcknowledgement.getVendorName());
        }
        if (messageAcknowledgement.getRetryCount() != null) {
            builder.retryCount(messageAcknowledgement.getRetryCount());
        }
        if (vendorDeliveryTime != null) {
            builder.vendorDeliveryTime(vendorDeliveryTime);
        } else if (messageAcknowledgement.getVendorDeliveryTime() != null) {
            builder.vendorDeliveryTime(messageAcknowledgement.getVendorDeliveryTime());
        }
        if (actorDeliveryTime != null) {
            builder.actorDeliveryTime(actorDeliveryTime);
        } else if (messageAcknowledgement.getActorDeliveryTime() != null) {
            builder.actorDeliveryTime(messageAcknowledgement.getActorDeliveryTime());
        }
        builder.actorId(messageAcknowledgement.getActorId());
        builder.actorType(messageAcknowledgement.getActorType());
        if (!StringUtils.isEmpty(messageAcknowledgement.getReferenceId())) {
            builder.referenceId(messageAcknowledgement.getReferenceId());
        }
        builder.communicationChannel(messageAcknowledgement.getCommunicationChannel());
        if (!StringUtils.isEmpty(messageAcknowledgement.getTempateName())) {
            builder.tempateName(messageAcknowledgement.getTempateName());
        }
        if (!StringUtils.isEmpty(messageAcknowledgement.getVendorMessageId())) {
            builder.vendorMessageId(messageAcknowledgement.getVendorMessageId());
        }
        if (state != null) {
            builder.state(state);
        }
        if (CommunicationChannel.APP_NOTIFICATION.name().equals(messageAcknowledgement.getCommunicationChannel())) {
            builder.contactType(ContactType.FCM_TOKEN);
            builder.actorContactId(messageAcknowledgement.getActorContactId());
        } else if (CommunicationChannel.EMAIL.name().equals(messageAcknowledgement.getCommunicationChannel())) {
            builder.contactType(ContactType.EMAIL_ID);
            builder.actorContactId(messageAcknowledgement.getActorContactId());
        } else if (CommunicationChannel.SMS.name().equals(messageAcknowledgement.getCommunicationChannel()) ||
                CommunicationChannel.WHATSAPP.name().equals(messageAcknowledgement.getCommunicationChannel())) {
            builder.contactType(ContactType.MOBILE_NUMBER);
            builder.actorContactId(messageAcknowledgement.getActorContactId());
        }
        if (!StringUtils.isEmpty(messageAcknowledgement.getParentReferenceId())) {
            builder.parentReferenceId(messageAcknowledgement.getParentReferenceId());
        }
        if (!StringUtils.isEmpty(messageAcknowledgement.getCampaignName())) {
            builder.campaignName(messageAcknowledgement.getCampaignName());
        }
        if (messageAcknowledgement.getUpdatedAt() != null) {
            builder.updatedAt(messageAcknowledgement.getUpdatedAt());
        }
        if (messageAcknowledgement.getCreatedAt() != null) {
            builder.createdAt(messageAcknowledgement.getCreatedAt());
        }
        if (messageAcknowledgement.getDeletedAt() != null) {
            builder.deletedAt(messageAcknowledgement.getDeletedAt());
        }
        if (messageAcknowledgement.getId() != null) {
            builder.id(messageAcknowledgement.getId());
        }

        Map<String, Object> attributeValueMap;
        if (messageAcknowledgement.getAttributes() != null) {
            attributeValueMap = new HashMap<>(messageAcknowledgement.getAttributes());
        } else attributeValueMap = new HashMap<>();
        if (request != null) {
            attributeValueMap.put("status", request.getStatus());
            attributeValueMap.put("cause", request.getCause());
            attributeValueMap.put("Errorcode", request.getErrCode());
            attributeValueMap.put("No of frags", request.getNoOfFrags());
        }
        builder.attributes(attributeValueMap);

        return builder.build();
    }
}