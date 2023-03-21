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
import farm.nurture.communication.engine.dao.ActorAppTokenReadBase;
import farm.nurture.communication.engine.models.ActorAppToken;
import farm.nurture.core.contracts.common.enums.ActorType;
import farm.nurture.laminar.core.io.sql.dao.WriteBase;
import lombok.extern.slf4j.Slf4j;
import farm.nurture.communication.engine.metric.MetricGroupNames;
import farm.nurture.communication.engine.metric.MetricTracker;
import farm.nurture.communication.engine.metric.Metrics;
import static java.util.stream.Collectors.joining;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Singleton
public class ActorAppTokenRepository {

    private final Metrics metrics = Metrics.getInstance();

    private static final String getByActorAndMobileAppSql = "SELECT * FROM actor_app_tokens where actor_id = ? and actor_type = ? and active = 1 and mobile_app_details_id in (";

    private static final String insertActorAppTokenSql = "INSERT INTO actor_app_tokens(actor_id, actor_type, mobile_app_details_id, fcm_token, active) " +
            "VALUES(?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE fcm_token = ?, active = ?";

    private static final String updateActorAppTokenSql = "UPDATE actor_app_tokens set fcm_token = ?, active = ? " +
            "where actor_id = ? and actor_type = ? and mobile_app_details_id = ?";

    public List<ActorAppToken> getByActorAndMobileApp(long actorId, ActorType actorType, List<Short> mobileAppDetailsIds) {
        boolean success = false;
        MetricTracker tracker = new MetricTracker(MetricGroupNames.NF_CE_MYSQL, "select_token_by_actor_app");
        ActorAppTokenReadBase readBase = new ActorAppTokenReadBase();
        List<ActorAppToken> appTokens = null;
        StringBuilder query = new StringBuilder(getByActorAndMobileAppSql);
        query.append(mobileAppDetailsIds.stream().map(id -> String.valueOf(id)).collect(joining(",")));
        query.append(")");
        try {
            appTokens = readBase.execute(query.toString(), Arrays.asList(actorId, actorType.name()));
            success = true;

        } catch (Exception e) {
            log.error("Error in fetching ActorAppToken for actorId: {}, actorType : {}, mobileAppDetailsIds : {}", actorId, actorType, mobileAppDetailsIds, e);

        } finally {
            tracker.stop(success);
        }
        return appTokens;
    }

    public void insertActorAppToken(ActorAppToken appToken) {
        boolean success = false;
        MetricTracker tracker = new MetricTracker(MetricGroupNames.NF_CE_MYSQL, "insert_actor_app_token");

        try {
            WriteBase writeBase = new WriteBase();
            writeBase.execute(insertActorAppTokenSql, Arrays.asList(appToken.getActorId(), appToken.getActorType().name(), appToken.getMobileAppDetailsId(),
                    appToken.getFcmToken(), appToken.getActive(), appToken.getFcmToken(), appToken.getActive()));
            success = true;

        } catch (Exception e) {
            log.error("Error in inserting ActorAppToken : {}", appToken, e);

        } finally {
            tracker.stop(success);
        }
    }

    public void updateActorAppToken(ActorAppToken appToken) {
        List<ActorAppToken> tokens = getByActorAndMobileApp(appToken.getActorId(), appToken.getActorType(), Arrays.asList(appToken.getMobileAppDetailsId()));
        ActorAppToken token = tokens == null || tokens.isEmpty() ? null : tokens.get(0);
        if(token == null) {
            insertActorAppToken(appToken);
            return;
        }

        boolean success = false;
        MetricTracker tracker = new MetricTracker(MetricGroupNames.NF_CE_MYSQL, "update_actor_app_token");

        try {
            WriteBase writeBase = new WriteBase();
            Boolean isActive = appToken.getActive() == null ? token.getActive() : appToken.getActive();
            writeBase.execute(updateActorAppTokenSql, Arrays.asList(appToken.getFcmToken(), isActive, appToken.getActorId(), appToken.getActorType().name(),
                    appToken.getMobileAppDetailsId()));
            success = true;

        } catch (Exception e) {
            log.error("Error in updating ActorAppToken : {}, existing ActorAppToken: {}", appToken, token, e);

        } finally {
            tracker.stop(success);
        }
    }
}
