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

import farm.nurture.communication.engine.models.ActorCommunicationDetails;
import farm.nurture.core.contracts.common.enums.ActorType;
import farm.nurture.laminar.core.io.sql.dao.ReadBase;
import lombok.extern.slf4j.Slf4j;
import farm.nurture.communication.engine.metric.MetricGroupNames;
import farm.nurture.communication.engine.metric.Metrics;
import farm.nurture.communication.engine.models.ActorCommunicationDetails.ActorCommunicationDetailsBuilder;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class ActorCommunicationDetailsReadBase extends ReadBase<ActorCommunicationDetails> {

    public static final String ID = "id";
    public static final String ACTOR_ID = "actor_id";
    public static final String ACTOR_TYPE = "actor_type";
    public static final String MOBILE_NUMBER = "mobile_number";
    public static final String LANGUAGE_ID = "language_id";
    public static final String ACTIVE = "active";
    public static final String CREATED_AT = "created_at";
    public static final String UPDATED_AT = "updated_at";
    public static final String DELETED_AT = "deleted_at";
    private List<ActorCommunicationDetails> records = null;

    private final Metrics metrics = Metrics.getInstance();

    @Override
    protected List<ActorCommunicationDetails> populate() throws SQLException {
        if ( null == this.rs) {
            log.warn("ActorCommunicationDetails ResultSet is not initialized.");
            throw new SQLException("ActorCommunicationDetails ResultSet is not initialized.");
        }

        if ( null == records) records = new ArrayList<ActorCommunicationDetails>();
        this.rs.setFetchSize(1000);
        while (this.rs.next()) {
            recordsCount++;
            records.add(populateActorCommunicationDetails());
        }
        return records;
    }

    private ActorCommunicationDetails populateActorCommunicationDetails() {
        ActorCommunicationDetails details = null;
        try {
            ActorCommunicationDetailsBuilder builder = ActorCommunicationDetails.builder();
            builder.id(rs.getLong(ID));
            builder.actorId(rs.getLong(ACTOR_ID));
            builder.actorType(ActorType.valueOf(rs.getString(ACTOR_TYPE)));
            builder.mobileNumber(rs.getString(MOBILE_NUMBER));
            builder.languageId(rs.getShort(LANGUAGE_ID));
            builder.active(rs.getBoolean(ACTIVE));
            builder.createdAt(rs.getTimestamp(CREATED_AT));
            builder.updatedAt(rs.getTimestamp(UPDATED_AT));
            builder.deletedAt(rs.getTimestamp(DELETED_AT));
            details = builder.build();

        } catch (Exception e) {
            metrics.onIncrement(MetricGroupNames.NF_CE_ACTOR_COMMUNICATION_DETAILS_READ_BASE, "populate_record_failed");
            log.error("Unable to populate ActorCommunicationDetails from resultSet : {}", rs, e);
        }
        return details;
    }

    @Override
    protected ActorCommunicationDetails getFirstRow() throws SQLException {
        if ( null == this.rs) {
            log.warn("ActorCommunicationDetails ResultSet is not initialized.");
            throw new SQLException("ActorCommunicationDetails ResultSet is not initialized.");
        }

        ActorCommunicationDetails details = null;
        this.rs.setFetchSize(1);

        while (this.rs.next()) {
            recordsCount++;
            details = populateActorCommunicationDetails();
        }
        return details;
    }

    @Override
    protected int getRecordsCount() {
        return recordsCount;
    }
}
