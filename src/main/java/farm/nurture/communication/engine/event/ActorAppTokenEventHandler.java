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

package farm.nurture.communication.engine.event;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import farm.nurture.kafka.Consumer;
import farm.nurture.kafka.Event;
import farm.nurture.kafka.EventHandler;
import farm.nurture.util.http.NFException;
import farm.nurture.communication.engine.models.ActorAppToken;
import lombok.extern.slf4j.Slf4j;
import farm.nurture.communication.engine.cache.MobileAppDetailsCache;
import farm.nurture.communication.engine.event.ActorAppTokenEvent.Action;
import farm.nurture.communication.engine.metric.MetricGroupNames;
import farm.nurture.communication.engine.metric.MetricTracker;
import farm.nurture.communication.engine.metric.Metrics;
import farm.nurture.communication.engine.models.MobileAppDetails;
import farm.nurture.communication.engine.repository.ActorAppTokenRepository;

@Slf4j
@Singleton
public class ActorAppTokenEventHandler implements EventHandler<String, String> {

    @Inject
    private ObjectMapper mapper;

    @Inject
    private MobileAppDetailsCache cache;

    @Inject
    private ActorAppTokenRepository repository;

    private final Metrics metrics = Metrics.getInstance();

    @Override
    public Consumer.Status handle(String topic, Event<String, String> event) {
        log.info("Got ActorAppToken event. Key : {}, Message : {}, Timestamp : {}, Headers : {}",
                event.getPartitionKey(), event.getMessage(), event.getTimestamp(), event.getHeaders());

        boolean success = false;
        MetricTracker tracker = new MetricTracker(MetricGroupNames.NF_CE, "actor_app_token_event");

        try {
            ActorAppTokenEvent appTokenEvent = mapper.readValue(event.getMessage(), ActorAppTokenEvent.class);
            log.info("appTokenVent value after converting json to object", appTokenEvent);
            ActorAppToken appToken = populateActorAppToken(appTokenEvent);
            if(appTokenEvent.getAction() == Action.CREATE) {
                repository.insertActorAppToken(appToken);
            } else {
                repository.updateActorAppToken(appToken);
            }

            success = true;
            return Consumer.Status.success;

        } catch (JsonParseException | JsonMappingException e) {
            log.error("Exception in deserializing actor app token event : {}", event, e);
            metrics.onIncrement(MetricGroupNames.NF_CE_ACTOR_APP_TOKEN_EVENT, "invalid_data");


        } catch (Exception e) {
            log.error("Exception in processing actor app token event : {}", event, e);

        } finally {
            tracker.stop(success);
        }
        return Consumer.Status.failure;
    }

    private Short getMobileAppDetailsId(ActorAppTokenEvent appTokenEvent) {
        MobileAppDetails appDetails = cache.getMobileAppDetailsByAFSAppId(appTokenEvent.getAppId());

        if(appDetails == null) {
            log.error("Unable to fetch appDetails by afs app id : {}, appTokenEvent : {}", appTokenEvent.getAppId(), appTokenEvent);
            metrics.onIncrement(MetricGroupNames.NF_CE_ACTOR_APP_TOKEN_EVENT, "get_mobile_app_details_failed");
            throw new NFException("Unable to fetch appDetails by afs app id : " + appTokenEvent.getAppId());
        }

        return appDetails.getId();
    }

    private ActorAppToken populateActorAppToken(ActorAppTokenEvent appTokenEvent) {
        ActorAppToken appToken = ActorAppToken.builder()
                .actorId(appTokenEvent.getActorId())
                .actorType(appTokenEvent.getActorType())
                .mobileAppDetailsId(getMobileAppDetailsId(appTokenEvent))
                .fcmToken(appTokenEvent.getFcmToken())
                .active(appTokenEvent.getActive())
                .build();
        return appToken;
    }
}
