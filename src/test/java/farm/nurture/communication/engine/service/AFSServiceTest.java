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
import farm.nurture.communication.engine.cache.LanguageCache;
import farm.nurture.communication.engine.cache.MobileAppDetailsCache;
import farm.nurture.communication.engine.models.ActorAppToken;
import farm.nurture.communication.engine.models.ActorCommunicationDetails;
import farm.nurture.communication.engine.models.Language;
import farm.nurture.communication.engine.models.MobileAppDetails;
import farm.nurture.communication.engine.repository.ActorAppTokenRepository;
import farm.nurture.communication.engine.repository.ActorCommunicationDetailsRepository;
import farm.nurture.core.contracts.common.enums.ActorType;
import farm.nurture.util.http.client.NFHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.junit.jupiter.MockServerExtension;
import org.mockserver.junit.jupiter.MockServerSettings;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.MediaType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.mockserver.model.HttpRequest.request;

@ExtendWith(MockitoExtension.class)
@ExtendWith(MockServerExtension.class)
@MockServerSettings(ports = {1080})
public class AFSServiceTest extends BaseServiceTest {
    public static final String GATEWAY_API = "/GatewayAPI/rest";

    private ClientAndServer client;

    @Spy
    private NFHttpClient nfHttpClient;

    @Mock
    private MobileAppDetailsCache mobileAppDetailsCache;

    @Spy
    private ActorCommunicationDetailsRepository actorCommunicationDetailsRepository = new ActorCommunicationDetailsRepository();

    @Spy
    private ActorAppTokenRepository actorAppTokenRepository = new ActorAppTokenRepository();

    @Mock
    private LanguageCache languageCache;

    @InjectMocks
    private AFSService afsService;

    public AFSServiceTest(ClientAndServer client) {
        this.client = client;
        this.nfHttpClient = new NFHttpClient(HttpClientBuilder.create().build());
    }

    @Test
    public void testGetActorCommunicationDetails() throws IOException {
        final String actorDetails = "Info";
        Path path = Paths.get("src/test/resources/responseBodyForActorCommunicationDetails.json");
        String jsonData = Files.readString(path, StandardCharsets.US_ASCII);
        final HttpRequest httpRequest = request().withMethod("GET").withPath(GATEWAY_API + "/afs/api/actorInfo")
                .withHeader(Constants.API_KEY, "test_api_key");
        ActorCommunicationDetails communicationDetails=ActorCommunicationDetails.builder().actorId(123456l).actorType(ActorType.FARMER)
                .mobileNumber("9999999999").languageId((short)1).active(true).build();

        client.when(httpRequest).respond(HttpResponse.response().withBody(jsonData).withStatusCode(200).withContentType(new MediaType(Constants.APPLICATION_JSON,"contentType")));
        when(languageCache.getLanguageByCode("en-us")).thenReturn(Language.builder().id((short) 1).build());
        Mockito.doNothing().when(actorCommunicationDetailsRepository).insertActorCommunicationDetails(communicationDetails);

        ActorCommunicationDetails details = afsService.getActorCommunicationDetails(actorDetails);

        assertEquals(details.getLanguageId(), (short) 1);
        assertEquals(details.getActorId(), 123456);
        assertEquals(details.getMobileNumber(), "9999999999");
        assertEquals(details.getActorType(), ActorType.FARMER);
        assertEquals(details.getActive(), true);
    }

    @Test
    public void testGetActorAppToken() throws IOException {
        final String actorDetails = "Info";
        Path path = Paths.get("src/test/resources/responseBodyForActorToken.json");
        String jsonData = Files.readString(path, StandardCharsets.US_ASCII);

        final HttpRequest httpRequest = request().withMethod("GET").withPath(GATEWAY_API + "/afs/api/fcm-tokenInfo")
                .withHeader(Constants.API_KEY, "test_api_key");
        ActorAppToken actorAppToken = ActorAppToken.builder().actorId(123456l).actorType(ActorType.FARMER).fcmToken("fcm_token").active(true).build();
        client.when(httpRequest).respond(HttpResponse.response().withBody(jsonData).withStatusCode(200).withContentType(new MediaType(Constants.APPLICATION_JSON,"contentType")));
        when(mobileAppDetailsCache.getMobileAppDetailsByAFSAppId((short) 1)).thenReturn(MobileAppDetails.builder().appId("5").build());
        Mockito.doNothing().when(actorAppTokenRepository).insertActorAppToken(actorAppToken);

        ActorAppToken appToken = afsService.getActorAppToken(actorDetails);

        assertEquals(appToken.getActorId(), 123456);
        assertEquals(appToken.getFcmToken(), "fcm_token");
        assertEquals(appToken.getActorType(), ActorType.FARMER);
        assertEquals(appToken.getActive(), true);
    }
}
