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
import farm.nurture.communication.engine.event.DerivedCommunicationEvent.SMSAttributes;
import farm.nurture.communication.engine.repository.MessageAcknowledgementRepository;
import farm.nurture.communication.engine.vendor.VendorType;
import farm.nurture.core.contracts.common.ActorID;
import farm.nurture.core.contracts.common.enums.ActorType;
import farm.nurture.core.contracts.common.enums.CommunicationChannel;
import farm.nurture.core.contracts.communication.engine.CommunicationEvent;
import farm.nurture.kafka.Event;
import farm.nurture.kafka.Producer;
import farm.nurture.util.http.HttpUtils;
import farm.nurture.util.http.client.NFAsyncHttpClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.junit.jupiter.MockServerExtension;
import org.mockserver.junit.jupiter.MockServerSettings;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockserver.model.HttpRequest.request;

@ExtendWith(MockitoExtension.class)
@ExtendWith(MockServerExtension.class)
@MockServerSettings(ports = {1080})
public class SMSServiceTest extends BaseServiceTest {

    public static final String GATEWAY_API = "/GatewayAPI/rest";
    public static final String USERNAME = "test_user";
    public static final String PASSWORD = "test_password";
    private ClientAndServer client;

    @Spy
    private Producer producer;

    @Spy
    private MessageAcknowledgementRepository messageAcknowledgementRepository;

    @Spy
    private NFAsyncHttpClient nfAsyncHttpClient = buildAsyncHttpClient();

    @InjectMocks
    private SMSService smsService;


    public SMSServiceTest(ClientAndServer client) {
        this.client = client;
    }

    @AfterEach
    public void reset() {
        smsService = null;
        nfAsyncHttpClient = null;
    }

    @Test
    public void testSendSMSFailure() {
        final long actorId = 1l;
        final String content = "Failed message";
        ActorID actorID = ActorID.newBuilder().setActorId(actorId).setActorType(ActorType.FARMER).build();
        CommunicationEvent communicationEvent = CommunicationEvent.newBuilder().setReceiverActor(actorID).build();

        Event<byte[], byte[]> event = new Event<>(String.valueOf(1).getBytes(),communicationEvent.toByteArray());
        DerivedCommunicationEvent derivedCommunicationEvent = new DerivedCommunicationEvent(event, "","Test client",
                content, true, null, null, CommunicationChannel.SMS, (short) 1, null, null, null, null, null, null, null, ActorType.NO_ACTOR);
        SMSAttributes smsAttributes = new DerivedCommunicationEvent.SMSAttributes("9999999999");
        derivedCommunicationEvent.setSmsAttributes(smsAttributes);
        derivedCommunicationEvent.setVendor(VendorType.GUPSHUP);
        final HttpRequest httpRequest = request().withMethod("GET").withPath(GATEWAY_API)
                .withHeader(Constants.CONTENT_TYPE, Constants.APPLICATION_JSON)
                .withQueryStringParameter("msg", content).withQueryStringParameter("send_to", "9999999999")
                .withQueryStringParameter("unicode", "1").withQueryStringParameter("msg_type", "Unicode_Text")
                .withQueryStringParameter("method", "sendMessage").withQueryStringParameter("v", "1.1").withQueryStringParameter("priority", "8")
                .withQueryStringParameter("userid", USERNAME).withQueryStringParameter("password", PASSWORD);
        client.when(httpRequest).respond(HttpResponse.response().withBody("error | 553 | Template does not exist").withStatusCode(500));

        doThrow(new RuntimeException("Connection")).when(nfAsyncHttpClient).sendMessage(eq(HttpUtils.HttpMethod.GET), any(), any(Map.class),
                any(Map.class), eq(null), any(SMSServiceCallback.class));


        smsService.sendSms(derivedCommunicationEvent);

        verify(nfAsyncHttpClient, times(1)).sendMessage(eq(HttpUtils.HttpMethod.GET), any(), any(Map.class), any(Map.class), eq(null), any(SMSServiceCallback.class));
    }

    @Test
    public void testSendSMSError() {
        final long actorId = 1l;
        final String content = "Error in message";
        ActorID actorID = ActorID.newBuilder().setActorId(actorId).setActorType(ActorType.FARMER).build();
        CommunicationEvent communicationEvent = CommunicationEvent.newBuilder().setReceiverActor(actorID).setReferenceId("Ref1").build();

        Event<byte[], byte[]> event = new Event<>(String.valueOf(1).getBytes(),communicationEvent.toByteArray());
        DerivedCommunicationEvent derivedCommunicationEvent = new DerivedCommunicationEvent(event, "", "Test client",
                content, true, null, null, CommunicationChannel.SMS,(short) 1, null, null, null, null, null, null, null, ActorType.NO_ACTOR);
        SMSAttributes smsAttributes = new DerivedCommunicationEvent.SMSAttributes("9999999999");
        derivedCommunicationEvent.setVendor(VendorType.GUPSHUP);
        derivedCommunicationEvent.setSmsAttributes(smsAttributes);

        final HttpRequest httpRequest = request().withMethod("GET").withPath(GATEWAY_API)
                .withHeader(Constants.CONTENT_TYPE, Constants.APPLICATION_JSON)
                .withQueryStringParameter("msg", content).withQueryStringParameter("send_to", "9999999999")
                .withQueryStringParameter("unicode", "1").withQueryStringParameter("msg_type", "Unicode_Text")
                .withQueryStringParameter("method", "sendMessage").withQueryStringParameter("v", "1.1").withQueryStringParameter("priority", "8")
                .withQueryStringParameter("userid", USERNAME).withQueryStringParameter("password", PASSWORD);
        client.when(httpRequest).respond(HttpResponse.response().withBody("error | 253 | Template does not exist").withStatusCode(200));
        smsService.sendSms(derivedCommunicationEvent);

        verify(nfAsyncHttpClient, times(1)).sendMessage(eq(HttpUtils.HttpMethod.GET), any(), any(Map.class), any(Map.class), eq(null), any(SMSServiceCallback.class));
    }

    @Test
    public void testSendSMSSuccessForGupshup() {

        final long actorId = 1l;
        final String content = "Welcome to nurture.retail, to login please use the OTP code: 1234";
        ActorID actorID = ActorID.newBuilder().setActorId(actorId).setActorType(ActorType.FARMER).build();
        CommunicationEvent communicationEvent = CommunicationEvent.newBuilder().setReceiverActor(actorID).build();

        Event<byte[], byte[]> event = new Event<>(String.valueOf(1).getBytes(),communicationEvent.toByteArray());
        DerivedCommunicationEvent derivedCommunicationEvent = new DerivedCommunicationEvent(event, "", "Test client",
                content, true, null, null, CommunicationChannel.SMS,(short) 1, null, null, null, null, null, null, null, ActorType.NO_ACTOR);
        derivedCommunicationEvent.setVendor(VendorType.KARIX);

        SMSAttributes smsAttributes = new DerivedCommunicationEvent.SMSAttributes("9993098893");
        derivedCommunicationEvent.setSmsAttributes(smsAttributes);

        final HttpRequest httpRequest = request().withMethod("GET").withPath(GATEWAY_API)
                .withHeader(Constants.CONTENT_TYPE, Constants.APPLICATION_JSON)
                .withQueryStringParameter("msg", content).withQueryStringParameter("send_to", "9999999999")
                .withQueryStringParameter("unicode", "1").withQueryStringParameter("msg_type", "Unicode_Text")
                .withQueryStringParameter("method", "sendMessage").withQueryStringParameter("v", "1.1").withQueryStringParameter("priority", "8")
                .withQueryStringParameter("userid", USERNAME).withQueryStringParameter("password", PASSWORD);
        client.when(httpRequest).respond(HttpResponse.response().withBody("success | 200 | message delivered").withStatusCode(200));

        smsService.sendSms(derivedCommunicationEvent);

        verify(nfAsyncHttpClient, times(1)).sendMessage(eq(HttpUtils.HttpMethod.GET), any(), any(Map.class), any(Map.class), eq(null), any(SMSServiceCallback.class));
    }

}
