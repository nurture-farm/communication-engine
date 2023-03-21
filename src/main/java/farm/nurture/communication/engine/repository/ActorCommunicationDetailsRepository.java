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
import farm.nurture.communication.engine.models.ActorCommunicationDetails;
import farm.nurture.core.contracts.common.enums.ActorType;
import farm.nurture.infra.util.StringUtils;
import farm.nurture.laminar.core.io.sql.dao.WriteBase;
import lombok.extern.slf4j.Slf4j;
import farm.nurture.communication.engine.dao.ActorCommunicationDetailsReadBase;
import farm.nurture.communication.engine.metric.MetricGroupNames;
import farm.nurture.communication.engine.metric.MetricTracker;
import farm.nurture.communication.engine.metric.Metrics;

import java.util.Arrays;

@Slf4j
@Singleton
public class ActorCommunicationDetailsRepository {

    private final Metrics metrics = Metrics.getInstance();

    private static final String getByMobileNumberSql = "SELECT * FROM actor_communication_details where actor_type = 'FARMER' and " +
            "active = 1 and mobile_number = ? ORDER BY actor_id DESC limit 1";

    private static final String getByActorIdAndActorTypeSql = "SELECT * FROM actor_communication_details where actor_id = ? and actor_type = ?";

    private static final String insertActorCommunicationDetailsSql = "INSERT INTO actor_communication_details(actor_id, actor_type, mobile_number, language_id, active) " +
            "VALUES(?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE mobile_number = ?, language_id = ?, active = ?";

    private static final String updateActorCommunicationDetailsSql = "UPDATE actor_communication_details set mobile_number = ?, language_id = ?, active = ? " +
            "where actor_id = ? and actor_type = ?";

    public ActorCommunicationDetails getByActorIdAndActorType(long actorId, ActorType actorType) {
        boolean success = false;
        MetricTracker tracker = new MetricTracker(MetricGroupNames.NF_CE_MYSQL, "select_comm_details_by_actor");
        ActorCommunicationDetailsReadBase readBase = new ActorCommunicationDetailsReadBase();
        ActorCommunicationDetails details = null;
        try {
            details = readBase.selectByUniqueKey(getByActorIdAndActorTypeSql, Arrays.asList(actorId, actorType.name()).toArray());
            success = true;

        } catch (Exception e) {
            log.error("Error in fetching ActorCommunicationDetails for actorId: {}, actorType : {}", actorId, actorType, e);

        } finally {
            tracker.stop(success);
        }
        return details;
    }

    public ResponseObject getByMobileNumberType(String mobileNumber) {
        boolean success = false;
        MetricTracker tracker = new MetricTracker(MetricGroupNames.NF_CE_MYSQL, "select_comm_details_by_actor");
        ActorCommunicationDetailsReadBase readBase = new ActorCommunicationDetailsReadBase();
        ActorCommunicationDetails details = null;
        try {
            details = readBase.selectByUniqueKey(getByMobileNumberSql, Arrays.asList(mobileNumber).toArray());
            success = true;

        } catch (Exception e) {
            log.error("Error in fetching ActorCommunicationDetails for mobileNumber: {}", mobileNumber, e);

        } finally {
            tracker.stop(success);
        }
        return new ResponseObject(details, success);
    }

    public void insertActorCommunicationDetails(ActorCommunicationDetails details) {
        boolean success = false;
        MetricTracker tracker = new MetricTracker(MetricGroupNames.NF_CE_MYSQL, "insert_actor_comm_details");

        try {
            WriteBase writeBase = new WriteBase();
            writeBase.execute(insertActorCommunicationDetailsSql, Arrays.asList(details.getActorId(), details.getActorType().name(), details.getMobileNumber(),
                    details.getLanguageId(), details.getActive(), details.getMobileNumber(), details.getLanguageId(), details.getActive()));
            success = true;

        } catch (Exception e) {
            log.error("Error in inserting ActorCommunicationDetails : {}", details, e);

        } finally {
            tracker.stop(success);
        }
    }

    public void updateActorCommunicationDetails(ActorCommunicationDetails details) {
        ActorCommunicationDetails commDetails = getByActorIdAndActorType(details.getActorId(), details.getActorType());
        if(commDetails == null) {
            insertActorCommunicationDetails(details);
            return;
        }

        boolean success = false;
        MetricTracker tracker = new MetricTracker(MetricGroupNames.NF_CE_MYSQL, "update_actor_comm_details");

        try {
            WriteBase writeBase = new WriteBase();
            String mobileNumber = StringUtils.isEmpty(details.getMobileNumber()) ? commDetails.getMobileNumber() : details.getMobileNumber();
            Short languageId = details.getLanguageId() == null ? commDetails.getLanguageId() : details.getLanguageId();
            Boolean active = details.getActive() == null ? commDetails.getActive() : details.getActive();

            writeBase.execute(updateActorCommunicationDetailsSql, Arrays.asList(mobileNumber, languageId, active, details.getActorId(), details.getActorType().name()));
            success = true;

        } catch (Exception e) {
            log.error("Error in updating ActorCommunicationDetails : {}, existing ActorCommunicationDetails: {}", details, commDetails, e);

        } finally {
            tracker.stop(success);
        }
    }

    public class ResponseObject {
        public ActorCommunicationDetails actorCommunicationDetails;

        public boolean success;

        public ResponseObject(ActorCommunicationDetails actorCommunicationDetails, boolean success) {
            this.actorCommunicationDetails = actorCommunicationDetails;
            this.success = success;
        }
    }

}
