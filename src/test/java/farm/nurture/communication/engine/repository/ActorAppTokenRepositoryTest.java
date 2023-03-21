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

import farm.nurture.communication.engine.H2Extension;
import farm.nurture.communication.engine.models.ActorAppToken;
import farm.nurture.core.contracts.common.enums.ActorType;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@Slf4j
@ExtendWith({H2Extension.class})
class ActorAppTokenRepositoryTest {

    private ActorAppTokenRepository actorAppTokenRepository = new ActorAppTokenRepository();

    @Test
    void testInsertActorAppToken() {
        final long actorId = 123L;
        final ActorType actorType = ActorType.FARMER;
        final short mobileAppDetailsId = 1;
        final String fcmToken = "TOKEN";
        ActorAppToken actorAppToken = ActorAppToken.builder().fcmToken(fcmToken).actorId(actorId)
                .actorType(actorType).mobileAppDetailsId(mobileAppDetailsId).active(true).build();
        actorAppTokenRepository.insertActorAppToken(actorAppToken);

        List<ActorAppToken> actorAppTokenList = actorAppTokenRepository.getByActorAndMobileApp(actorId, actorType, Arrays.asList(mobileAppDetailsId));
        assertEquals(1, actorAppTokenList.size());
        ActorAppToken token = actorAppTokenList.get(0);
        assertNotNull(token.getId());
        assertEquals(actorId, token.getActorId());
        assertEquals(actorType, token.getActorType());
        assertEquals(mobileAppDetailsId, token.getMobileAppDetailsId());
        assertEquals(fcmToken, token.getFcmToken());
        assertTrue(token.getActive());
    }

    @Test
    void testUpdateExistingActorAppToken() {
        final long actorId = 123L;
        final ActorType actorType = ActorType.FARMER;
        final short mobileAppDetailsId = 1;
        final String fcmToken = "UPDATED_TOKEN";
        ActorAppToken actorAppToken = ActorAppToken.builder().fcmToken(fcmToken).actorId(actorId)
                .actorType(actorType).mobileAppDetailsId(mobileAppDetailsId).active(true).build();
        actorAppTokenRepository.updateActorAppToken(actorAppToken);

        List<ActorAppToken> actorAppTokenList = actorAppTokenRepository.getByActorAndMobileApp(actorId, actorType, Arrays.asList(mobileAppDetailsId));
        assertEquals(1, actorAppTokenList.size());
        ActorAppToken token = actorAppTokenList.get(0);
        assertNotNull(token.getId());
        assertEquals(actorId, token.getActorId());
        assertEquals(actorType, token.getActorType());
        assertEquals(mobileAppDetailsId, token.getMobileAppDetailsId());
        assertEquals(fcmToken, token.getFcmToken());
        assertTrue(token.getActive());
    }

    @Test
    void testUpdateNonExistingActorAppToken() {
        final long actorId = 456L;
        final ActorType actorType = ActorType.OPERATOR;
        final short mobileAppDetailsId = 2;
        final String fcmToken = "NEW_TOKEN";
        ActorAppToken actorAppToken = ActorAppToken.builder().fcmToken(fcmToken).actorId(actorId)
                .actorType(actorType).mobileAppDetailsId(mobileAppDetailsId).active(true).build();
        actorAppTokenRepository.updateActorAppToken(actorAppToken);

        List<ActorAppToken> actorAppTokenList = actorAppTokenRepository.getByActorAndMobileApp(actorId, actorType, Arrays.asList(mobileAppDetailsId));
        assertEquals(1, actorAppTokenList.size());
        ActorAppToken token = actorAppTokenList.get(0);
        assertNotNull(token.getId());
        assertEquals(actorId, token.getActorId());
        assertEquals(actorType, token.getActorType());
        assertEquals(mobileAppDetailsId, token.getMobileAppDetailsId());
        assertEquals(fcmToken, token.getFcmToken());
        assertTrue(token.getActive());
    }

    @Test
    public void testExceptionInInsertActorAppToken() {
        final long actorId = 456L;
        final ActorType actorType = ActorType.EXTENSION_MANAGER;
        final short mobileAppDetailsId = 2;
        ActorAppToken actorAppToken = Mockito.mock(ActorAppToken.class);
        when(actorAppToken.getActorId()).thenThrow(new RuntimeException("Exception"));

        actorAppTokenRepository.insertActorAppToken(actorAppToken);

        List<ActorAppToken> actorAppTokenList = actorAppTokenRepository.getByActorAndMobileApp(actorId, actorType, Arrays.asList(mobileAppDetailsId));
        assertEquals(0, actorAppTokenList.size());
    }

    @Test
    public void testExceptionInGetByActorAndMobileApp() {
        final long actorId = 456L;
        final short mobileAppDetailsId = 2;
        ActorAppTokenRepository actorAppTokenRepository1 = Mockito.spy(actorAppTokenRepository);
        ActorAppToken appToken = Mockito.mock(ActorAppToken.class);
        when(appToken.getActorId()).thenReturn(actorId);
        when(appToken.getActorType()).thenReturn(ActorType.SUPPORT_AGENT);
        when(appToken.getMobileAppDetailsId()).thenReturn(mobileAppDetailsId);
        when(appToken.getFcmToken()).thenThrow(new RuntimeException("Exception"));
        when(actorAppTokenRepository1.getByActorAndMobileApp(actorId, ActorType.SUPPORT_AGENT, Arrays.asList(mobileAppDetailsId))).thenReturn(Arrays.asList(appToken));
        actorAppTokenRepository1.updateActorAppToken(appToken);
        List<ActorAppToken> actorAppTokens = actorAppTokenRepository.getByActorAndMobileApp(actorId, ActorType.SUPPORT_AGENT, Arrays.asList(mobileAppDetailsId));
        assertEquals(actorAppTokens.size(),0);
    }
}