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

package farm.nurture.communication.engine.utils;

import com.google.inject.Singleton;
import com.google.protobuf.Timestamp;
import farm.nurture.communication.engine.models.MessageAcknowledgement;
import farm.nurture.core.contracts.common.ActorID;
import farm.nurture.core.contracts.common.Attribs;
import farm.nurture.core.contracts.common.enums.EventType;
import farm.nurture.event.portal.proto.EventPortalGrpc;
import farm.nurture.event.portal.proto.EventRequest;
import farm.nurture.infra.util.ApplicationConfiguration;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static farm.nurture.communication.engine.Constants.*;

@Slf4j
@Singleton
public class ExecutorServiceImpl  {

    private static ExecutorService executorService;

    private static ApplicationConfiguration applicationConfiguration;

    private static ManagedChannel managedChannel;

    private static EventPortalGrpc.EventPortalFutureStub eventPortalFutureStub;

    public void init(){
        applicationConfiguration = ApplicationConfiguration.getInstance();
        executorService = Executors.newFixedThreadPool(applicationConfiguration.getInt(NUM_THREADS, 10));
        String host = applicationConfiguration.get(EVENT_PORTAL_HOST, EVENT_PORTAL_DEFAULT_HOST);
        int port = applicationConfiguration.getInt(EVENT_PORTAL_PORT, EVENT_PORTAL_DEFAULT_PORT);
        managedChannel =
                ManagedChannelBuilder.forAddress(host, port)
                        .executor(executorService)
                        .usePlaintext()
                        .build();
        eventPortalFutureStub = EventPortalGrpc.newFutureStub(managedChannel);
        applicationConfiguration = ApplicationConfiguration.getInstance();
    }

    /**
     * This method is used to send event to event portal
     * @param messageAcknowledgement
     * @param status
     */
    public void sendEvent(MessageAcknowledgement messageAcknowledgement, String status){
        log.info("Parsing message ack {} and status {} ", messageAcknowledgement, status);
        EventRequest eventRequest = EventRequest.newBuilder().setActor(
                ActorID.newBuilder().setActorType(messageAcknowledgement.getActorType()).setActorId(messageAcknowledgement.getActorId()).build()
        ).setEventName(EVENT_NAME).setEventTime(Timestamp.newBuilder().setSeconds(
                System.currentTimeMillis()/1000).build())
                .setEventType(EventType.EVENT)
                .addAllAttributes(Arrays.asList(
                        Attribs.newBuilder().setKey(LABEL_TEMPLATE).setValue(messageAcknowledgement.getTempateName()).build(),
                        Attribs.newBuilder().setKey(LABEL_CAMPAIGN).setValue(messageAcknowledgement.getCampaignName()).build(),
                        Attribs.newBuilder().setKey(LABEL_STATUS).setValue(status).build(),
                        Attribs.newBuilder().setKey(LABEL_LANGUAGE_CODE).setValue(messageAcknowledgement.getLanguageId().toString()).build()
                ))
                .build();
        log.info("Sending Event Request {} to event portal ", eventRequest);
        eventPortalFutureStub.uploadEvent(eventRequest);
    }


}
