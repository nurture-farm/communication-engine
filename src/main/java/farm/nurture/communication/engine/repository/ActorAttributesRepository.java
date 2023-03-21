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

import com.google.inject.Singleton;
import farm.nurture.communication.engine.dao.ActorAttributesReadBase;
import farm.nurture.communication.engine.metric.MetricGroupNames;
import farm.nurture.communication.engine.metric.MetricTracker;
import farm.nurture.communication.engine.metric.Metrics;
import farm.nurture.communication.engine.models.ActorAttributes;
import farm.nurture.core.contracts.common.enums.ActorType;
import farm.nurture.laminar.core.io.sql.dao.WriteBase;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;

@Slf4j
@Singleton
public class ActorAttributesRepository {
    private final Metrics metrics = Metrics.getInstance();

    private static final String getByActorAndAttributeKeySql = "SELECT * FROM actor_attributes where actor_id = ? and actor_type = ? and namespace = ? and attr_key = ?";

    private static final String insertActorAttributesSql = "INSERT INTO actor_attributes(actor_id, actor_type, namespace, attr_key, attr_value) " +
            "VALUES(?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE attr_value = ?";

    private static final String updateActorAttributesSql = "UPDATE actor_attributes set deleted_at = NULL, attr_value = ? " +
            "where actor_id = ? and actor_type = ? and namespace = ? and attr_key = ?";

    private static final String deleteUpdateActorAttributesSql = "UPDATE actor_attributes set deleted_at = now() " +
            "where actor_id = ? and actor_type = ? and namespace = ? and attr_key = ?";

    public ActorAttributes getByActorAndAttributeKey(long actorId, ActorType actorType, ActorAttributes.NameSpace nameSpace, String attrKey) {
        boolean success = false;
        MetricTracker tracker = new MetricTracker(MetricGroupNames.NF_CE_MYSQL, "select_actor_attributes");
        ActorAttributesReadBase readBase = new ActorAttributesReadBase();
        ActorAttributes attributes = null;
        try {
            Object[] arrayOfKeys = {actorId, actorType.name(), nameSpace.name(), attrKey};
            attributes = readBase.selectByUniqueKey(getByActorAndAttributeKeySql, arrayOfKeys);
            success = true;

        } catch (Exception e) {
            log.error("Error in fetching ActorAttributes for actorId: {}, actorType : {}, nameSpace : {}, attrKey : {}", actorId, actorType, nameSpace, attrKey, e);
        } finally {
            tracker.stop(success);
        }
        return attributes;
    }

    public boolean insertActorAttributes(ActorAttributes actorAttributes) {
        boolean success = false;
        MetricTracker tracker = new MetricTracker(MetricGroupNames.NF_CE_MYSQL, "insert_actor_attributes");

        try {
            WriteBase writeBase = new WriteBase();
            writeBase.execute(insertActorAttributesSql, Arrays.asList(actorAttributes.getActorId(), actorAttributes.getActorType().name(), actorAttributes.getNameSpace().name(),
                    actorAttributes.getAttrKey(), actorAttributes.getAttrValue(), actorAttributes.getAttrValue()));
            success = true;

        } catch (Exception e) {
            log.error("Error in inserting ActorAttributes : {}", actorAttributes, e);

        } finally {
            tracker.stop(success);
        }
        return success;
    }

    public boolean updateActorAttributes(ActorAttributes actorAttributes) {
        ActorAttributes attributes = getByActorAndAttributeKey(actorAttributes.getActorId(), actorAttributes.getActorType(), actorAttributes.getNameSpace(), actorAttributes.getAttrKey());
        if(attributes == null) {
            return insertActorAttributes(actorAttributes);
        }

        boolean success = false;
        MetricTracker tracker = new MetricTracker(MetricGroupNames.NF_CE_MYSQL, "update_actor_attributes");

        try {
            WriteBase writeBase = new WriteBase();
            writeBase.execute(updateActorAttributesSql, Arrays.asList(actorAttributes.getAttrValue(), actorAttributes.getActorId(), actorAttributes.getActorType().name(),
                    actorAttributes.getNameSpace().name(), actorAttributes.getAttrKey()));
            success = true;

        } catch (Exception e) {
            log.error("Error in updating ActorAttributes : {}, using existing attributes: {}", actorAttributes, attributes, e);

        } finally {
            tracker.stop(success);
        }
        return success;
    }

    public boolean deleteActorAttributes(ActorAttributes actorAttributes) {
        ActorAttributes attributes = getByActorAndAttributeKey(actorAttributes.getActorId(), actorAttributes.getActorType(), actorAttributes.getNameSpace(), actorAttributes.getAttrKey());
        if(attributes == null) {
            return false;
        }

        boolean success = false;
        MetricTracker tracker = new MetricTracker(MetricGroupNames.NF_CE_MYSQL, "delete_actor_attributes");

        try {
            WriteBase writeBase = new WriteBase();
            writeBase.execute(deleteUpdateActorAttributesSql, Arrays.asList(actorAttributes.getActorId(), actorAttributes.getActorType().name(),
                    actorAttributes.getNameSpace().name(), actorAttributes.getAttrKey()));
            success = true;

        } catch (Exception e) {
            log.error("Error in deleting ActorAttributes : {}, using existing attributes: {}", actorAttributes, attributes, e);

        } finally {
            tracker.stop(success);
        }
        return success;
    }
}
