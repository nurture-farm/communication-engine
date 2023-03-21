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
import farm.nurture.communication.engine.models.ActorCommunicationDetails;
import farm.nurture.communication.engine.models.Language;
import farm.nurture.communication.engine.repository.ActorCommunicationDetailsRepository;
import farm.nurture.infra.util.StringUtils;
import farm.nurture.kafka.Consumer;
import farm.nurture.kafka.Event;
import farm.nurture.kafka.EventHandler;
import lombok.extern.slf4j.Slf4j;
import farm.nurture.communication.engine.cache.LanguageCache;
import farm.nurture.communication.engine.metric.MetricGroupNames;
import farm.nurture.communication.engine.metric.MetricTracker;
import farm.nurture.communication.engine.metric.Metrics;

@Slf4j
@Singleton
public class ActorCommunicationDetailsEventHandler implements EventHandler<String, String> {

    @Inject
    private ObjectMapper mapper;

    @Inject
    private LanguageCache languageCache;

    @Inject
    private ActorCommunicationDetailsRepository repository;

    private final Metrics metrics = Metrics.getInstance();

    @Override
    public Consumer.Status handle(String topic, Event<String, String> event) {
        log.info("Got ActorCommunicationDetails event. Key : {}, Message : {}, Timestamp : {}, Headers : {}",
                event.getPartitionKey(), event.getMessage(), event.getTimestamp(), event.getHeaders());

        boolean success = false;
        MetricTracker tracker = new MetricTracker(MetricGroupNames.NF_CE, "actor_comm_details_event");

        try {
            ActorCommunicationDetailsEvent commDetails = mapper.readValue(event.getMessage(), ActorCommunicationDetailsEvent.class);

            if(commDetails.getAction() == ActorCommunicationDetailsEvent.Action.CREATE && StringUtils.isEmpty(commDetails.getMobileNumber())) {
                log.error("Actor mobile number can not be null : {}", commDetails);
                metrics.onIncrement(MetricGroupNames.NF_CE_ACTOR_COMM_DETAILS_EVENT, "invalid_mobile_number");
                return Consumer.Status.failure;
            }

            ActorCommunicationDetails details = populateActorCommunicationDetails(commDetails);
            if(commDetails.getAction() == ActorCommunicationDetailsEvent.Action.CREATE) {
                repository.insertActorCommunicationDetails(details);
            } else {
                repository.updateActorCommunicationDetails(details);
            }

            success = true;
            return Consumer.Status.success;

        } catch (JsonParseException | JsonMappingException e) {
            log.error("Exception in deserializing actor communication details event : {}", event, e);
            metrics.onIncrement(MetricGroupNames.NF_CE_ACTOR_COMM_DETAILS_EVENT, "invalid_data");

        } catch (Exception e) {
            log.error("Exception in processing actor communication details event : {}", event, e);

        } finally {
            tracker.stop(success);
        }
        return Consumer.Status.failure;
    }

    private Short getLanguageId(ActorCommunicationDetailsEvent commDetails) {
        Short languageId = null;
        if(commDetails.getLanguageCode() != null) {
            Language language = languageCache.getLanguageByCode(commDetails.getLanguageCode());

            if(language == null) {
                log.error("Unable to fetch language by code : {}, commDetails : {}", commDetails.getLanguageCode(), commDetails);
                metrics.onIncrement(MetricGroupNames.NF_CE_ACTOR_COMM_DETAILS_EVENT, "get_language_failed");

            } else {
                languageId = language.getId();
            }
        }
        return languageId;
    }

    private ActorCommunicationDetails populateActorCommunicationDetails(ActorCommunicationDetailsEvent commDetails) {
        ActorCommunicationDetails details = ActorCommunicationDetails.builder()
                .actorId(commDetails.getActorId())
                .actorType(commDetails.getActorType())
                .mobileNumber(commDetails.getMobileNumber())
                .languageId(getLanguageId(commDetails))
                .active(commDetails.getActive())
                .build();
        return details;
    }
}
