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
import farm.nurture.communication.engine.models.ActorAttributes;
import farm.nurture.core.contracts.common.enums.ActorType;
import farm.nurture.laminar.core.io.sql.dao.ReadBase;
import lombok.extern.slf4j.Slf4j;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class ActorAttributesReadBase extends ReadBase<ActorAttributes> {
    public static final String ID = "id";
    public static final String ACTOR_ID = "actor_id";
    public static final String ACTOR_TYPE = "actor_type";
    public static final String NAMESPACE = "namespace";
    public static final String ATTR_KEY = "attr_key";
    public static final String ATTR_VALUE = "attr_value";
    public static final String CREATED_AT = "created_at";
    public static final String UPDATED_AT = "updated_at";
    public static final String DELETED_AT = "deleted_at";
    private List<ActorAttributes> records = null;

    private final Metrics metrics = Metrics.getInstance();

    @Override
    protected List<ActorAttributes> populate() throws SQLException {
        if ( null == this.rs) {
            log.warn("ActorAttributes ResultSet is not initialized.");
            throw new SQLException("ActorAttributes ResultSet is not initialized.");
        }

        if ( null == records) records = new ArrayList<ActorAttributes>();
        this.rs.setFetchSize(1000);
        while (this.rs.next()) {
            recordsCount++;
            records.add(populateActorAttributes());
        }
        return records;
    }

    private ActorAttributes populateActorAttributes() {
        ActorAttributes attributes = null;
        try {
            ActorAttributes.ActorAttributesBuilder builder = ActorAttributes.builder();
            builder.id(rs.getLong(ID));
            builder.actorId(rs.getLong(ACTOR_ID));
            builder.actorType(ActorType.valueOf(rs.getString(ACTOR_TYPE)));
            builder.nameSpace(ActorAttributes.NameSpace.valueOf(rs.getString(NAMESPACE)));
            builder.attrKey(rs.getString(ATTR_KEY));
            builder.attrValue(rs.getString(ATTR_VALUE));
            builder.createdAt(rs.getTimestamp(CREATED_AT));
            builder.updatedAt(rs.getTimestamp(UPDATED_AT));
            builder.deletedAt(rs.getTimestamp(DELETED_AT));
            attributes = builder.build();

        } catch (Exception e) {
            metrics.onIncrement(MetricGroupNames.NF_CE_ACTOR_ATTRIBUTES_READ_BASE, "populate_record_failed");
            log.error("Unable to populate ActorAttributes from resultSet : {}", rs, e);
        }
        return attributes;
    }

    @Override
    protected ActorAttributes getFirstRow() throws SQLException {
        if ( null == this.rs) {
            log.warn("ActorAttributes ResultSet is not initialized.");
            throw new SQLException("ActorAttributes ResultSet is not initialized.");
        }

        ActorAttributes attributes = null;
        this.rs.setFetchSize(1);

        while (this.rs.next()) {
            recordsCount++;
            attributes = populateActorAttributes();
        }
        return attributes;
    }

    @Override
    protected int getRecordsCount() {
        return recordsCount;
    }
}
