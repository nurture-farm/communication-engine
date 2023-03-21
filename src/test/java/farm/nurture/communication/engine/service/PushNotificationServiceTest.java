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

package farm.nurture.communication.engine.service;

import farm.nurture.communication.engine.Constants;
import farm.nurture.communication.engine.event.DerivedCommunicationEvent;
import farm.nurture.core.contracts.common.ActorID;
import farm.nurture.core.contracts.common.enums.ActorType;
import farm.nurture.core.contracts.common.enums.CommunicationChannel;
import farm.nurture.core.contracts.communication.engine.CommunicationEvent;
import farm.nurture.kafka.Event;
import farm.nurture.kafka.Producer;
import farm.nurture.util.http.HttpUtils;
import farm.nurture.util.http.client.NFAsyncHttpClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.junit.jupiter.MockServerExtension;
import org.mockserver.junit.jupiter.MockServerSettings;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.MediaType;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockserver.model.HttpRequest.request;

@ExtendWith(MockitoExtension.class)
@ExtendWith(MockServerExtension.class)
@MockServerSettings(ports = {1080})
public class PushNotificationServiceTest extends BaseServiceTest {
    public static final String GATEWAY_API = "/GatewayAPI/send";

    private ClientAndServer client;

    @Mock
    private Producer producer;

    @Spy
    private NFAsyncHttpClient nfAsyncHttpClient = buildAsyncHttpClient();

    @InjectMocks
    private PushNotificationService pushNotificationService;

    public PushNotificationServiceTest(ClientAndServer client) {
        this.client = client;
    }

//    @Test
//    public void testPushNotificationSuccess() {
//
//        final long actorId = 1l;
//        final String content = "For testing purpose";
//        ActorID actorID = ActorID.newBuilder().setActorId(actorId).setActorType(ActorType.FARMER).build();
//        CommunicationEvent communicationEvent = CommunicationEvent.newBuilder().setReceiverActor(actorID).build();
//
//        Event<byte[], byte[]> event = new Event<>(String.valueOf(1).getBytes(),communicationEvent.toByteArray());
//        DerivedCommunicationEvent derivedCommunicationEvent = new DerivedCommunicationEvent(event, "Test client1",
//                content, true, null, null, CommunicationChannel.APP_NOTIFICATION);
//        DerivedCommunicationEvent.PNAttributes pnAttributes = new DerivedCommunicationEvent.PNAttributes("testTitle1", "appToken", "apiKey1");
//        derivedCommunicationEvent.setPNAttributes(pnAttributes);
//
//        final HttpRequest httpRequest = request().withMethod("POST").withPath(GATEWAY_API)
//                .withHeader(Constants.CONTENT_TYPE, Constants.APPLICATION_JSON)
//                .withHeader(Constants.AUTHORIZATION, "key=" + derivedCommunicationEvent.getPNAttributes().getApiKey());
//
//        client.when(httpRequest).respond(HttpResponse.response().withBody("{\"success\": 1}").withStatusCode(200).withContentType(new MediaType(Constants.APPLICATION_JSON,"contentType1")));
//
//        pushNotificationService.sendPushNotification(derivedCommunicationEvent);
//
//        verify(nfAsyncHttpClient, times(1)).sendMessage(eq(HttpUtils.HttpMethod.POST), eq("http://localhost:1080/GatewayAPI/send"),
//                eq(null), any(Map.class), any(FCMData.class), any(PushNotificationServiceCallback.class));
//    }

//    @Test
//    public void testPushNotificationFailure() {
//
//        final long actorId = 1l;
//        final String content = "Failed message";
//        ActorID actorID = ActorID.newBuilder().setActorId(actorId).setActorType(ActorType.FARMER).build();
//        CommunicationEvent communicationEvent = CommunicationEvent.newBuilder().setReceiverActor(actorID).build();
//
//        Event<byte[], byte[]> event = new Event<>(String.valueOf(1).getBytes(),communicationEvent.toByteArray());
//        DerivedCommunicationEvent derivedCommunicationEvent = new DerivedCommunicationEvent(event, "Test client2",
//                content, true, null, null, CommunicationChannel.APP_NOTIFICATION);
//        DerivedCommunicationEvent.PNAttributes pnAttributes = new DerivedCommunicationEvent.PNAttributes("testTitle2", "appToken", "apiKey2");
//        derivedCommunicationEvent.setPNAttributes(pnAttributes);
//
//        final HttpRequest httpRequest = request().withMethod("POST").withPath(GATEWAY_API)
//                .withHeader(Constants.CONTENT_TYPE, Constants.APPLICATION_JSON)
//                .withHeader(Constants.AUTHORIZATION, "key=" + derivedCommunicationEvent.getPNAttributes().getApiKey());
//
//        client.when(httpRequest).respond(HttpResponse.response().withBody("{\"Failed\": 553}").withStatusCode(500).withContentType(new MediaType(Constants.APPLICATION_JSON,"contentType2")));
//
//        pushNotificationService.sendPushNotification(derivedCommunicationEvent);
//
//        verify(nfAsyncHttpClient, times(1)).sendMessage(eq(HttpUtils.HttpMethod.POST), eq("http://localhost:1080/GatewayAPI/send"),
//                eq(null), any(Map.class), any(FCMData.class), any(PushNotificationServiceCallback.class));
//    }

//    @Test
//    public void testPushNotificationError() {
//        final long actorId = 1l;
//        final String content = "Error in message";
//        ActorID actorID = ActorID.newBuilder().setActorId(actorId).setActorType(ActorType.FARMER).build();
//        CommunicationEvent communicationEvent = CommunicationEvent.newBuilder().setReceiverActor(actorID).build();
//
//        Event<byte[], byte[]> event = new Event<>(String.valueOf(1).getBytes(),communicationEvent.toByteArray());
//        DerivedCommunicationEvent derivedCommunicationEvent = new DerivedCommunicationEvent(event, "Test client3",
//                content, true, null, null, CommunicationChannel.APP_NOTIFICATION);
//        DerivedCommunicationEvent.PNAttributes pnAttributes = new DerivedCommunicationEvent.PNAttributes("testTitle3", "appToken", "apiKey3");
//        derivedCommunicationEvent.setPNAttributes(pnAttributes);
//
//        final HttpRequest httpRequest = request().withMethod("POST").withPath(GATEWAY_API)
//                .withHeader(Constants.CONTENT_TYPE, Constants.APPLICATION_JSON)
//                .withHeader(Constants.AUTHORIZATION, "key=" + derivedCommunicationEvent.getPNAttributes().getApiKey());
//
//        client.when(httpRequest).respond(HttpResponse.response().withBody("{\"error\": 443}").withStatusCode(400).withContentType(new MediaType(Constants.APPLICATION_JSON,"contentType3")));
//
//        pushNotificationService.sendPushNotification(derivedCommunicationEvent);
//
//        verify(nfAsyncHttpClient, times(1)).sendMessage(eq(HttpUtils.HttpMethod.POST), eq("http://localhost:1080/GatewayAPI/send"),
//                eq(null), any(Map.class), any(FCMData.class), any(PushNotificationServiceCallback.class));
//    }
}
