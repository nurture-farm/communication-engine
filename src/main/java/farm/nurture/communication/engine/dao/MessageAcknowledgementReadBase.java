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

package farm.nurture.communication.engine.dao;

import farm.nurture.communication.engine.metric.MetricGroupNames;
import farm.nurture.communication.engine.metric.Metrics;
import farm.nurture.communication.engine.models.MessageAcknowledgement;
import farm.nurture.communication.engine.models.MessageAcknowledgement.MessageAcknowledgementBuilder;
import farm.nurture.core.contracts.common.enums.ActorType;
import farm.nurture.core.contracts.common.enums.ContactType;
import farm.nurture.infra.util.StringUtils;
import farm.nurture.laminar.core.io.sql.dao.ReadBase;
import farm.nurture.util.serializer.Serializer;
import lombok.extern.slf4j.Slf4j;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public class MessageAcknowledgementReadBase extends ReadBase<MessageAcknowledgement> {

    public static final String ID = "id";
    public static final String ACTOR_ID = "actor_id";
    public static final String ACTOR_TYPE = "actor_type";
    public static final String MOBILE_NUMBER = "mobile_number";
    public static final String COMMUNICATION_CHANNEL = "communication_channel";
    public static final String REFERENCE_ID = "reference_id";
    public static final String TEMPLATE_NAME = "template_name";
    public static final String LANGUAGE_ID = "language_id";
    public static final String MESSAGE_CONTENT = "message_content";
    public static final String IS_UNICODE = "is_unicode";
    public static final String VENDOR_NAME = "vendor_name";
    public static final String VENDOR_MESSAGE_ID = "vendor_message_id";
    public static final String STATE = "state";
    public static final String RETRY_COUNT = "retry_count";
    public static final String PLACEHOLDERS = "placeholders";
    public static final String ATTRIBUTES = "attributes";
    public static final String VENDOR_DELIVERY_TIME = "vendor_delivery_time";
    public static final String ACTOR_DELIVERY_TIME = "actor_delivery_time";
    public static final String CONTACT_TYPE = "contact_type";
    public static final String ACTOR_CONTACT_ID = "actor_contact_id";
    public static final String PARENT_REFERENCE_ID = "parent_reference_id";
    public static final String CAMPAIGN_NAME = "campaign_name";
    public static final String VERSION = "version";
    public static final String CREATED_DATE = "created_date";
    public static final String CREATED_AT = "created_at";
    public static final String UPDATED_AT = "updated_at";
    public static final String DELETED_AT = "deleted_at";
    private List<MessageAcknowledgement> records = null;

    private final Metrics metrics = Metrics.getInstance();

    @Override
    protected List<MessageAcknowledgement> populate() throws SQLException {
        if ( null == this.rs) {
            log.warn("MessageAcknowledgements ResultSet is not initialized.");
            throw new SQLException("MessageAcknowledgements ResultSet is not initialized.");
        }

        if ( null == records) records = new ArrayList<MessageAcknowledgement>();
        this.rs.setFetchSize(1000);
        while (this.rs.next()) {
            recordsCount++;
            records.add(populateMessageAcknowledgements());
        }
        return records;
    }


    private MessageAcknowledgement populateMessageAcknowledgements() {
        MessageAcknowledgement messageAcknowledgement = null;
        try {
            MessageAcknowledgementBuilder builder = MessageAcknowledgement.builder();
            builder.id(rs.getLong(ID));
            builder.actorId(rs.getLong(ACTOR_ID));
            builder.actorType(ActorType.valueOf(rs.getString(ACTOR_TYPE)));
            builder.communicationChannel(rs.getString(COMMUNICATION_CHANNEL));
            builder.referenceId(rs.getString(REFERENCE_ID));
            builder.tempateName(rs.getString(TEMPLATE_NAME));
            builder.languageId(rs.getShort(LANGUAGE_ID));
            builder.messageContent(rs.getString(MESSAGE_CONTENT));
            builder.isUnicode(rs.getBoolean(IS_UNICODE));
            builder.vendorName(rs.getString(VENDOR_NAME));
            builder.vendorMessageId(rs.getString(VENDOR_MESSAGE_ID));
            builder.state(MessageAcknowledgement.State.valueOf(rs.getString(STATE)));
            builder.retryCount(rs.getInt(RETRY_COUNT));
            populatePlaceholders(builder, rs.getLong(ID), rs.getString(PLACEHOLDERS));
            populateAttributes(builder, rs.getLong(ID), rs.getString(ATTRIBUTES));
            builder.vendorDeliveryTime(rs.getTimestamp(VENDOR_DELIVERY_TIME));
            builder.actorDeliveryTime(rs.getTimestamp(ACTOR_DELIVERY_TIME));
            if(StringUtils.isNonEmpty(rs.getString(CONTACT_TYPE))) {
                builder.contactType(ContactType.valueOf(rs.getString(CONTACT_TYPE)));
            }
            builder.actorContactId(rs.getString(ACTOR_CONTACT_ID));
            builder.parentReferenceId(rs.getString(PARENT_REFERENCE_ID));
            builder.campaignName(rs.getString(CAMPAIGN_NAME));
            builder.version(rs.getInt(VERSION));
            builder.createdDate(rs.getDate(CREATED_DATE));
            builder.createdAt(rs.getTimestamp(CREATED_AT));
            builder.updatedAt(rs.getTimestamp(UPDATED_AT));
            builder.deletedAt(rs.getTimestamp(DELETED_AT));
            messageAcknowledgement = builder.build();

        } catch (Exception e) {
            metrics.onIncrement(MetricGroupNames.NF_CE_MESSAGE_ACKNOWLEDGEMENTS_READ_BASE, "populate_record_failed");
            log.error("Unable to populate MessageAcknowledgements from resultSet : {}", rs, e);
        }
        return messageAcknowledgement;
    }

    private void populatePlaceholders(MessageAcknowledgementBuilder builder, Long id, String placeHolders) {
        try {
            if (StringUtils.isNonEmpty(placeHolders)) {
                builder.placeHolders(Serializer.DEFAULT_JSON_SERIALIZER.deserialize(placeHolders, Map.class));
            }
        } catch (Exception e) {
            log.error("Error in deserializing placeholders : {} for Id : {}", placeHolders, id, e);
            metrics.onIncrement(MetricGroupNames.NF_CE_MESSAGE_ACKNOWLEDGEMENTS_READ_BASE, "placeholders_deserialization_failed");
        }
    }

    private void populateAttributes(MessageAcknowledgementBuilder builder, Long id, String attributes) {
        try {
            if (StringUtils.isNonEmpty(attributes)) {
                builder.attributes(Serializer.DEFAULT_JSON_SERIALIZER.deserialize(attributes, Map.class));
            }
        } catch (Exception e) {
            log.error("Error in deserializing attributes : {} for Id : {}", attributes, id, e);
            metrics.onIncrement(MetricGroupNames.NF_CE_MESSAGE_ACKNOWLEDGEMENTS_READ_BASE, "attributes_deserialization_failed");
        }
    }

    @Override
    protected MessageAcknowledgement getFirstRow() throws SQLException {
        if ( null == this.rs) {
            log.warn("MessageAcknowledgements ResultSet is not initialized.");
            throw new SQLException("MessageAcknowledgements ResultSet is not initialized.");
        }

        MessageAcknowledgement messageAcknowledgement = null;
        this.rs.setFetchSize(1);

        while (this.rs.next()) {
            recordsCount++;
            messageAcknowledgement = populateMessageAcknowledgements();
        }
        return messageAcknowledgement;
    }

    @Override
    protected int getRecordsCount() {
        return recordsCount;
    }
}