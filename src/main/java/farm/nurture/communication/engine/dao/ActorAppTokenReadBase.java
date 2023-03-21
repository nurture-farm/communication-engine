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

import farm.nurture.communication.engine.models.ActorAppToken;
import farm.nurture.core.contracts.common.enums.ActorType;
import farm.nurture.laminar.core.io.sql.dao.ReadBase;
import lombok.extern.slf4j.Slf4j;
import farm.nurture.communication.engine.metric.MetricGroupNames;
import farm.nurture.communication.engine.metric.Metrics;
import farm.nurture.communication.engine.models.ActorAppToken.ActorAppTokenBuilder;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class ActorAppTokenReadBase extends ReadBase<ActorAppToken> {

    public static final String ID = "id";
    public static final String ACTOR_ID = "actor_id";
    public static final String ACTOR_TYPE = "actor_type";
    public static final String MOBILE_APP_DETAILS_ID = "mobile_app_details_id";
    public static final String FCM_TOKEN = "fcm_token";
    public static final String ACTIVE = "active";
    public static final String CREATED_AT = "created_at";
    public static final String UPDATED_AT = "updated_at";
    public static final String DELETED_AT = "deleted_at";
    private List<ActorAppToken> records = null;

    private final Metrics metrics = Metrics.getInstance();

    @Override
    protected List<ActorAppToken> populate() throws SQLException {
        if ( null == this.rs) {
            log.warn("ActorAppToken ResultSet is not initialized.");
            throw new SQLException("ActorAppToken ResultSet is not initialized.");
        }

        if ( null == records) records = new ArrayList<ActorAppToken>();
        this.rs.setFetchSize(1000);
        while (this.rs.next()) {
            recordsCount++;
            records.add(populateActorAppToken());
        }
        return records;
    }


    private ActorAppToken populateActorAppToken() {
        ActorAppToken appToken = null;
        try {
            ActorAppTokenBuilder builder = ActorAppToken.builder();
            builder.id(rs.getLong(ID));
            builder.actorId(rs.getLong(ACTOR_ID));
            builder.actorType(ActorType.valueOf(rs.getString(ACTOR_TYPE)));
            builder.mobileAppDetailsId(rs.getShort(MOBILE_APP_DETAILS_ID));
            builder.fcmToken(rs.getString(FCM_TOKEN));
            builder.active(rs.getBoolean(ACTIVE));
            builder.createdAt(rs.getTimestamp(CREATED_AT));
            builder.updatedAt(rs.getTimestamp(UPDATED_AT));
            builder.deletedAt(rs.getTimestamp(DELETED_AT));
            appToken = builder.build();

        } catch (Exception e) {
            metrics.onIncrement(MetricGroupNames.NF_CE_ACTOR_APP_TOKEN_READ_BASE, "populate_record_failed");
            log.error("Unable to populate ActorAppToken from resultSet : {}", rs, e);
        }
        return appToken;
    }

    @Override
    protected ActorAppToken getFirstRow() throws SQLException {
        if ( null == this.rs) {
            log.warn("ActorAppToken ResultSet is not initialized.");
            throw new SQLException("ActorAppToken ResultSet is not initialized.");
        }

        ActorAppToken appToken = null;
        this.rs.setFetchSize(1);

        while (this.rs.next()) {
            recordsCount++;
            appToken = populateActorAppToken();
        }
        return appToken;
    }

    @Override
    protected int getRecordsCount() {
        return recordsCount;
    }
}
