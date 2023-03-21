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

import com.fasterxml.jackson.databind.ObjectMapper;
import farm.nurture.communication.engine.cache.MobileAppDetailsCache;
import farm.nurture.communication.engine.models.ActorAppToken;
import farm.nurture.communication.engine.models.MobileAppDetails;
import farm.nurture.communication.engine.repository.ActorAppTokenRepository;
import farm.nurture.core.contracts.common.enums.ActorType;
import farm.nurture.kafka.Consumer;
import farm.nurture.kafka.Event;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ActorAppTokenEventHandlerTest {
    @Spy
    private final ObjectMapper mapper = new ObjectMapper();

    @Mock
    private MobileAppDetailsCache cache;

    @Mock
    private ActorAppTokenRepository repository;

    @InjectMocks
    private ActorAppTokenEventHandler eventHandler;

    @Test
    public void testInsertActorAppToken() throws IOException {
        Short appId = 1;
        ActorAppTokenEvent appToken = new ActorAppTokenEvent(1L, ActorType.FARMER, appId, "dsdf-sdfds", true, ActorAppTokenEvent.Action.CREATE);
        String message = mapper.writeValueAsString(appToken);

        MobileAppDetails details = MobileAppDetails.builder().afsAppId((short) 1).appId(String.valueOf(appId)).id((short) 1)
                .appName("Farmer App").appType(MobileAppDetails.AppType.ANDROID).build();
        when(cache.getMobileAppDetailsByAFSAppId((short) 1)).thenReturn(details);

        Event<String, String> event = new Event<>("1", message);
        Consumer.Status status = eventHandler.handle("topic", event);

        ArgumentCaptor<ActorAppToken> captor = ArgumentCaptor.forClass(ActorAppToken.class);
        verify(repository).insertActorAppToken(captor.capture());
        ActorAppToken actualAppToken = captor.getValue();

        assertEquals(Consumer.Status.success, status);
        assertEquals(appToken.getActorId(), actualAppToken.getActorId());
        assertEquals(appToken.getActorType(), actualAppToken.getActorType());
        assertEquals(details.getId(), actualAppToken.getMobileAppDetailsId());
        assertEquals(appToken.getActive(), actualAppToken.getActive());
        assertEquals(appToken.getFcmToken(), actualAppToken.getFcmToken());
    }

    @Test
    public void testUpdateActorAppToken() throws IOException {
        Short appId = 1;
        ActorAppTokenEvent appToken = new ActorAppTokenEvent(1L, ActorType.FARMER, appId, "dsdf-sdfds-sdf", true, ActorAppTokenEvent.Action.UPDATE);
        String message = mapper.writeValueAsString(appToken);

        MobileAppDetails details = MobileAppDetails.builder().afsAppId((short) 1).appId(String.valueOf(appId)).id((short) 1)
                .appName("Farmer App").appType(MobileAppDetails.AppType.ANDROID).build();
        when(cache.getMobileAppDetailsByAFSAppId((short) 1)).thenReturn(details);

        Event<String, String> event = new Event<>("1", message);
        Consumer.Status status = eventHandler.handle("topic", event);

        ArgumentCaptor<ActorAppToken> captor = ArgumentCaptor.forClass(ActorAppToken.class);
        verify(repository).updateActorAppToken(captor.capture());
        ActorAppToken actualAppToken = captor.getValue();

        assertEquals(Consumer.Status.success, status);
        assertEquals(appToken.getActorId(), actualAppToken.getActorId());
        assertEquals(appToken.getActorType(), actualAppToken.getActorType());
        assertEquals(details.getId(), actualAppToken.getMobileAppDetailsId());
        assertEquals(appToken.getActive(), actualAppToken.getActive());
        assertEquals(appToken.getFcmToken(), actualAppToken.getFcmToken());
    }

    @Test
    public void testUpdateFailure() throws IOException {
        Short appId = 1;
        ActorAppTokenEvent appToken = new ActorAppTokenEvent(1L, ActorType.FARMER, appId, "dsdf-sdfds-sdf", true, ActorAppTokenEvent.Action.UPDATE);
        String message = mapper.writeValueAsString(appToken);

        MobileAppDetails details = MobileAppDetails.builder().afsAppId((short) 1).appId(String.valueOf(appId)).id((short) 1)
                .appName("Farmer App").appType(MobileAppDetails.AppType.ANDROID).build();
        when(cache.getMobileAppDetailsByAFSAppId((short) 1)).thenReturn(details);
        doThrow(new RuntimeException("Exception")).when(repository).updateActorAppToken(any());

        Event<String, String> event = new Event<>("1", message);
        Consumer.Status status = eventHandler.handle("topic", event);

        assertEquals(Consumer.Status.failure, status);
    }

    @Test
    public void testDeserializationFailure() {
        Event<String, String> event = new Event<>("1", "Test");
        Consumer.Status status = eventHandler.handle("topic", event);

        assertEquals(Consumer.Status.failure, status);
    }

    @Test
    public void testGetMobileAppDetailsFailure() throws IOException {
        ActorAppTokenEvent appToken = new ActorAppTokenEvent(1L, ActorType.FARMER, (short) 1, "dsdf-sdfds-sdf", true, ActorAppTokenEvent.Action.UPDATE);
        Event<String, String> event = new Event<>("1", mapper.writeValueAsString(appToken));

        when(cache.getMobileAppDetailsByAFSAppId((short) 1)).thenReturn(null);
        Consumer.Status status = eventHandler.handle("topic", event);
        assertEquals(Consumer.Status.failure, status);
    }
}