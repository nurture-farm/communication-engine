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

import com.github.mustachejava.Mustache;
import com.google.protobuf.Timestamp;
import farm.nurture.communication.engine.cache.*;
import farm.nurture.communication.engine.models.ActorAppToken;
import farm.nurture.communication.engine.models.ActorCommunicationDetails;
import farm.nurture.communication.engine.models.Language;
import farm.nurture.communication.engine.models.MobileAppDetails;
import farm.nurture.communication.engine.repository.ActorAppTokenRepository;
import farm.nurture.communication.engine.repository.ActorCommunicationDetailsRepository;
import farm.nurture.communication.engine.service.PushNotificationService;
import farm.nurture.communication.engine.service.SMSService;
import farm.nurture.core.contracts.common.ActorID;
import farm.nurture.core.contracts.common.enums.ActorType;
import farm.nurture.core.contracts.common.enums.CommunicationChannel;
import farm.nurture.core.contracts.communication.engine.CommunicationEvent;
import farm.nurture.kafka.Consumer;
import farm.nurture.kafka.Event;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.Writer;
import java.time.Instant;
import java.util.Arrays;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CommunicationEventHandlerTest {

    @Mock
    private ActorCommunicationDetailsRepository actorCommDetailRepository;

    @Mock
    private ActorAppTokenRepository actorAppTokenRepository;

    @Mock
    private LanguageCache languageCache;

    @Mock
    private MobileAppDetailsCache appDetailsCache;

    @Mock
    private TemplateCache templateCache;

    @Mock
    private TemplateCacheValue templateCacheValue;

    @Mock
    private Mustache mustache;

    @Mock
    private Writer writer;

    @Mock
    private MobileAppDetails mobileAppDetails;

    @Mock
    private SMSService smsService;

    @Mock
    private PushNotificationService pushNotificationService;

    @Mock
    private AFSService afsService;

    @Captor
    ArgumentCaptor<Long> actorIdArgumentCaptor;

    @Captor
    ArgumentCaptor<ActorType> actorTypeArgumentCaptor;

    @InjectMocks
    private CommunicationEventHandler communicationEventHandler;

    @Test
    public void testEventExpiry() {

        Instant time = Instant.now();
        Timestamp currTimeStamp = Timestamp.newBuilder().setNanos(time.getNano()).build();

        ActorID actorID = ActorID.newBuilder().setActorId(376013).setActorType(ActorType.FARMER).build();
        CommunicationEvent communicationEvent = CommunicationEvent.newBuilder().setExpiry(currTimeStamp).setReceiverActor(actorID).build();

        Event<byte[], byte[]> event = new Event<>(String.valueOf(1).getBytes(),communicationEvent.toByteArray());
        Consumer.Status status = communicationEventHandler.handle("communication_events", event);

        assertEquals(Consumer.Status.failure, status);
    }

    @Test
    public void testMissingCommDetails() {

        Long actorId = -1L;
        ActorID actorID = ActorID.newBuilder().setActorId(actorId).setActorType(ActorType.FARMER).build();
        CommunicationEvent communicationEvent = CommunicationEvent.newBuilder().setReceiverActor(actorID).build();
        when(actorCommDetailRepository.getByActorIdAndActorType(actorId,ActorType.FARMER)).thenReturn(null);
        when(afsService.getActorCommunicationDetails("/"+ActorType.FARMER.name()+"/"+actorId.toString())).thenReturn(null);

        Event<byte[], byte[]> event = new Event<>(String.valueOf(1).getBytes(),communicationEvent.toByteArray());
        Consumer.Status status = communicationEventHandler.handle("communication_events", event);

        assertEquals(Consumer.Status.failure, status);
    }

    @Test
    public void testMissingActorToken() {

        Long actorId = -1L;
        ActorID actorID = ActorID.newBuilder().setActorId(actorId).setActorType(ActorType.FARMER).build();
        CommunicationEvent communicationEvent = CommunicationEvent.newBuilder().setReceiverActor(actorID).addChannel(CommunicationChannel.APP_NOTIFICATION).build();

        ActorCommunicationDetails actorCommunicationDetails = new ActorCommunicationDetails();
        when(actorCommDetailRepository.getByActorIdAndActorType(actorId,ActorType.FARMER)).thenReturn(actorCommunicationDetails);
        when(actorAppTokenRepository.getByActorAndMobileApp(actorId,ActorType.FARMER, Arrays.asList((short) 6, (short) 7))).thenReturn(null);
        when(afsService.getActorAppToken("/" + ActorType.FARMER.name() + "/" + actorId.toString())).thenReturn(null);
        when(appDetailsCache.getMobileAppDetailsById(null)).thenReturn(null);

        Event<byte[], byte[]> event = new Event<>(String.valueOf(1).getBytes(),communicationEvent.toByteArray());
        Consumer.Status status = communicationEventHandler.handle("communication_events", event);

        assertEquals(Consumer.Status.failure, status);
    }

    @Test
    public void testMissingTemplate() {

        Long actorId = -1L;
        ActorID actorID = ActorID.newBuilder().setActorId(actorId).setActorType(ActorType.FARMER).build();
        CommunicationEvent communicationEvent = CommunicationEvent.newBuilder().setReceiverActor(actorID).addChannel(CommunicationChannel.SMS).build();

        ActorCommunicationDetails actorCommunicationDetails = new ActorCommunicationDetails();
        when(actorCommDetailRepository.getByActorIdAndActorType(actorId,ActorType.FARMER)).thenReturn(actorCommunicationDetails);
        when(languageCache.getLanguageByCode("hi-in")).thenReturn(Language.builder().id((short) 1).build());

        Event<byte[], byte[]> event = new Event<>(String.valueOf(1).getBytes(),communicationEvent.toByteArray());
        Consumer.Status status = communicationEventHandler.handle("communication_events", event);
        assertEquals(Consumer.Status.failure, status);
    }

    @Test
    public void testCommunicationChannelSMS() {
        Long actorId = -1L;
        ActorID actorID = ActorID.newBuilder().setActorId(actorId).setActorType(ActorType.FARMER).build();
        CommunicationEvent communicationEvent = CommunicationEvent.newBuilder().setReceiverActor(actorID)
                .addChannel(CommunicationChannel.SMS).build();
        ActorCommunicationDetails actorCommunicationDetails = new ActorCommunicationDetails();

        when(actorCommDetailRepository.getByActorIdAndActorType(actorId, ActorType.FARMER)).thenReturn(actorCommunicationDetails);
        when(languageCache.getLanguageByCode("hi-in")).thenReturn(Language.builder().id((short) 1).unicode(true).build());
        when(templateCache.getCompiledTemplateByCacheKey(any(TemplateCacheKey.class))).thenReturn(templateCacheValue);
        when(templateCacheValue.getCompiledTemplate()).thenReturn(mustache);
        when(mustache.execute(any(Writer.class), any(Map.class))).thenReturn(writer);

        Event<byte[], byte[]> event = new Event<>(String.valueOf(1).getBytes(), communicationEvent.toByteArray());
        Consumer.Status status = communicationEventHandler.handle("communication_events", event);
        assertEquals(Consumer.Status.success, status);
    }

    @Test
    public void testCommunicationChannelAPP() {
        Long actorId = -1L;
        ActorID actorID = ActorID.newBuilder().setActorId(actorId).setActorType(ActorType.FARMER).build();
        CommunicationEvent communicationEvent = CommunicationEvent.newBuilder().setReceiverActor(actorID)
                .addChannel(CommunicationChannel.APP_NOTIFICATION).build();
        ActorCommunicationDetails actorCommunicationDetails = new ActorCommunicationDetails();
        ActorAppToken appToken = ActorAppToken.builder().mobileAppDetailsId((short) 6).actorId(actorId).actorType(ActorType.FARMER).build();

        when(actorCommDetailRepository.getByActorIdAndActorType(actorId, ActorType.FARMER)).thenReturn(actorCommunicationDetails);
        when(languageCache.getLanguageByCode("hi-in")).thenReturn(Language.builder().id((short) 1).unicode(true).build());
        when(templateCache.getCompiledTemplateByCacheKey(any(TemplateCacheKey.class))).thenReturn(templateCacheValue);
        when(templateCacheValue.getCompiledTemplate()).thenReturn(mustache);
        when(mustache.execute(any(Writer.class), any(Map.class))).thenReturn(writer);
        when(appDetailsCache.getMobileAppDetailsById(appToken.getMobileAppDetailsId())).thenReturn(mobileAppDetails);
        when(actorAppTokenRepository.getByActorAndMobileApp(actorId, ActorType.FARMER, Arrays.asList((short) 6, (short) 7)))
                .thenReturn(Arrays.asList(appToken));

        Event<byte[], byte[]> event = new Event<>(String.valueOf(1).getBytes(), communicationEvent.toByteArray());
        Consumer.Status status = communicationEventHandler.handle("communication_events", event);
        assertEquals(Consumer.Status.success, status);
    }

    @Test
    public void testCommunicationChannelOthers() {
        Long actorId = -1L;
        ActorID actorID = ActorID.newBuilder().setActorId(actorId).setActorType(ActorType.FARMER).build();
        CommunicationEvent communicationEvent = CommunicationEvent.newBuilder().setReceiverActor(actorID)
                .addChannel(CommunicationChannel.EMAIL).build();
        ActorCommunicationDetails actorCommunicationDetails = new ActorCommunicationDetails();

        when(actorCommDetailRepository.getByActorIdAndActorType(actorId, ActorType.FARMER)).thenReturn(actorCommunicationDetails);
        when(languageCache.getLanguageByCode("hi-in")).thenReturn(Language.builder().id((short) 1).unicode(true).build());
        when(templateCache.getCompiledTemplateByCacheKey(any(TemplateCacheKey.class))).thenReturn(templateCacheValue);
        when(templateCacheValue.getCompiledTemplate()).thenReturn(mustache);
        when(mustache.execute(any(Writer.class), any(Map.class))).thenReturn(writer);

        Event<byte[], byte[]> event = new Event<>(String.valueOf(1).getBytes(), communicationEvent.toByteArray());
        Consumer.Status status = communicationEventHandler.handle("communication_events", event);
        assertEquals(Consumer.Status.failure, status);
    }

}


