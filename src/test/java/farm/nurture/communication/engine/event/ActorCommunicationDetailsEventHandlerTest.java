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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import farm.nurture.communication.engine.cache.LanguageCache;
import farm.nurture.communication.engine.models.ActorCommunicationDetails;
import farm.nurture.communication.engine.models.Language;
import farm.nurture.communication.engine.repository.ActorCommunicationDetailsRepository;
import farm.nurture.core.contracts.common.enums.ActorType;
import farm.nurture.kafka.Consumer;
import farm.nurture.kafka.Event;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ActorCommunicationDetailsEventHandlerTest {

    private static final String mobileNumber="9999999999";
    private static final long actorId = 1l;
    private static final String languageCode = "en-us";
    private static final String partitionKey = "1";
    private static final String topic = "topic";
    private static final Short languageId = 0;
    private static final boolean activeStatus = true;
    private static final String languageName = "English";

    @Spy
    private final ObjectMapper mapper = new ObjectMapper();

    @Mock
    private LanguageCache languageCache;

    @Mock
    private ActorCommunicationDetailsRepository repository;

    @InjectMocks
    private ActorCommunicationDetailsEventHandler eventHandler;

    @ParameterizedTest
    @EnumSource(ActorCommunicationDetailsEvent.Action.class)
    public void testActorCommunicationDetails(ActorCommunicationDetailsEvent.Action detailsEventAction) throws JsonProcessingException {
        ActorCommunicationDetailsEvent communicationDetailsEvent = new ActorCommunicationDetailsEvent(actorId, ActorType.FARMER, mobileNumber,
                languageCode, activeStatus, detailsEventAction);
        String message = mapper.writeValueAsString(communicationDetailsEvent);
        Language language = Language.builder().code(languageCode).name(languageName).unicode(false).id(languageId).build();
        when(languageCache.getLanguageByCode(languageCode)).thenReturn(language);

        Event<String, String> event = new Event<>(partitionKey, message);
        Consumer.Status status = eventHandler.handle(topic, event);

        ArgumentCaptor<ActorCommunicationDetails> captor = ArgumentCaptor.forClass(ActorCommunicationDetails.class);
        if (detailsEventAction != ActorCommunicationDetailsEvent.Action.CREATE) {
            verify(repository).updateActorCommunicationDetails(captor.capture());
        } else {
            verify(repository).insertActorCommunicationDetails(captor.capture());
        }
        ActorCommunicationDetails actorCommunicationDetails = captor.getValue();

        assertEquals(Consumer.Status.success, status);
        assertEquals(communicationDetailsEvent.getActorId(), actorCommunicationDetails.getActorId());
        assertEquals(communicationDetailsEvent.getActorType(), actorCommunicationDetails.getActorType());
        assertEquals(communicationDetailsEvent.getMobileNumber(), actorCommunicationDetails.getMobileNumber());
        assertEquals(actorCommunicationDetails.getLanguageId(), languageId);
        assertEquals(communicationDetailsEvent.getActive(), actorCommunicationDetails.getActive());
    }

    @Test
    public void testCreateFailure() throws JsonProcessingException {
        ActorCommunicationDetailsEvent communicationDetailsEvent = new ActorCommunicationDetailsEvent(actorId, ActorType.FARMER, "",
                languageCode, activeStatus, ActorCommunicationDetailsEvent.Action.CREATE);
        String message = mapper.writeValueAsString(communicationDetailsEvent);
        Event<String, String> event = new Event<>(partitionKey, message);
        Consumer.Status status = eventHandler.handle(topic, event);

        assertEquals(Consumer.Status.failure, status);
    }

    @Test
    public void testUpdateFailure() throws JsonProcessingException {
        ActorCommunicationDetailsEvent communicationDetailsEvent = new ActorCommunicationDetailsEvent(actorId, ActorType.FARMER, mobileNumber,
                languageCode, activeStatus, ActorCommunicationDetailsEvent.Action.UPDATE);
        String message = mapper.writeValueAsString(communicationDetailsEvent);
        Language language = Language.builder().code(languageCode).name(languageName).unicode(false).id(languageId).build();
        when(languageCache.getLanguageByCode(languageCode)).thenReturn(language);
        doThrow(new RuntimeException("Exception")).when(repository).updateActorCommunicationDetails(ActorCommunicationDetails.builder()
                .actorId(actorId).actorType(ActorType.FARMER).mobileNumber(mobileNumber).languageId(languageId).active(activeStatus).build());

        Event<String, String> event = new Event<>(partitionKey, message);
        Consumer.Status status = eventHandler.handle(topic, event);

        assertEquals(Consumer.Status.failure, status);
    }

    @ParameterizedTest
    @EnumSource(ActorCommunicationDetailsEvent.Action.class)
    public void testNullLanguageId(ActorCommunicationDetailsEvent.Action detailsEventAction) throws JsonProcessingException {
        ActorCommunicationDetailsEvent communicationDetailsEvent = new ActorCommunicationDetailsEvent(actorId, ActorType.FARMER, mobileNumber,
                languageCode, activeStatus, detailsEventAction);
        String message = mapper.writeValueAsString(communicationDetailsEvent);
        when(languageCache.getLanguageByCode(languageCode)).thenReturn(null);

        Event<String, String> event = new Event<>(partitionKey, message);
        Consumer.Status status = eventHandler.handle(topic, event);

        ArgumentCaptor<ActorCommunicationDetails> captor = ArgumentCaptor.forClass(ActorCommunicationDetails.class);
        if (detailsEventAction != ActorCommunicationDetailsEvent.Action.CREATE) {
            verify(repository).updateActorCommunicationDetails(captor.capture());
        } else {
            verify(repository).insertActorCommunicationDetails(captor.capture());
        }
        ActorCommunicationDetails actorCommunicationDetails = captor.getValue();

        assertEquals(Consumer.Status.success, status);
        assertEquals(communicationDetailsEvent.getActorId(), actorCommunicationDetails.getActorId());
        assertEquals(communicationDetailsEvent.getActorType(), actorCommunicationDetails.getActorType());
        assertEquals(communicationDetailsEvent.getMobileNumber(), actorCommunicationDetails.getMobileNumber());
        assertNull(actorCommunicationDetails.getLanguageId());
        assertEquals(communicationDetailsEvent.getActive(), actorCommunicationDetails.getActive());
    }

    @Test
    public void testJsonException() throws IOException {
        ActorCommunicationDetailsEvent communicationDetailsEvent = new ActorCommunicationDetailsEvent(actorId, ActorType.FARMER, "",
                languageCode, activeStatus, ActorCommunicationDetailsEvent.Action.CREATE);
        String message = mapper.writeValueAsString(communicationDetailsEvent);
        Event<String, String> event = new Event<>(partitionKey, message);

        doThrow(new JsonMappingException("Exception")).when(mapper).readValue(event.getMessage(), ActorCommunicationDetailsEvent.class);

        Consumer.Status status = eventHandler.handle(topic, event);

        assertEquals(Consumer.Status.failure, status);
    }
}
