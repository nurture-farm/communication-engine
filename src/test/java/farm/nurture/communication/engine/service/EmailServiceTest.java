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

import farm.nurture.communication.engine.cache.LanguageCache;
import farm.nurture.communication.engine.cache.TemplateCache;
import farm.nurture.communication.engine.event.DerivedCommunicationEvent;
import farm.nurture.communication.engine.repository.MessageAcknowledgementRepository;
import farm.nurture.core.contracts.common.enums.ActorType;
import farm.nurture.core.contracts.common.enums.CommunicationChannel;
import farm.nurture.core.contracts.common.enums.LanguageCode;
import farm.nurture.core.contracts.communication.engine.ActorDetails;
import farm.nurture.core.contracts.communication.engine.CommunicationEvent;
import farm.nurture.kafka.Event;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockserver.junit.jupiter.MockServerExtension;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ExtendWith(MockServerExtension.class)
@RunWith(MockitoJUnitRunner.Silent.class)
public class EmailServiceTest extends BaseServiceTest {


    @Spy
    private TemplateCache templateCache;

    @Spy
    private LanguageCache languageCache;

    @Spy
    private MessageAcknowledgementRepository messageAcknowledgementRepository;

    @InjectMocks
    private EmailService emailService;

    @Mock
    private Session session;

    @Mock
    private Transport transport;

    @Test
    public void testSendEmailFailure() throws MessagingException {

        final String content = "Welcome to nurture.retail, to login please use the OTP code: 1234";
        ActorDetails actorDetails = ActorDetails.newBuilder().setEmailId("kishan.ngm@gmail.com").setLanguageCode(LanguageCode.EN_US).build();
        CommunicationEvent communicationEvent = CommunicationEvent.newBuilder().setReceiverActorDetails(actorDetails).build();

        Event<byte[], byte[]> event = new Event<>(String.valueOf(1).getBytes(),communicationEvent.toByteArray());
        DerivedCommunicationEvent derivedCommunicationEvent = new DerivedCommunicationEvent(event, "", "Test client",
                content, true, null, null, CommunicationChannel.SMS,(short) 1, null, null, null, null, null, null, null, ActorType.NO_ACTOR);

        DerivedCommunicationEvent.EmailAttributes emailAttributes = new DerivedCommunicationEvent.EmailAttributes("kishan.ngm@gmail.com", content,null,null,null);
        derivedCommunicationEvent.setEmailAttributes(emailAttributes);

        doNothing().when(session.getTransport(Mockito.anyString()));


        emailService.sendEmail(derivedCommunicationEvent);

        verify(session, times(1));
    }
}
