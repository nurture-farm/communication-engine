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

package farm.nurture.communication.engine;

import farm.nurture.core.contracts.common.ActorID;
import farm.nurture.core.contracts.common.enums.ActorType;
import farm.nurture.core.contracts.common.enums.CommunicationChannel;
import farm.nurture.core.contracts.communication.engine.CommunicationEvent;
import farm.nurture.core.contracts.communication.engine.Placeholder;
import farm.nurture.kafka.Event;
import farm.nurture.kafka.Producer;
import farm.nurture.kafka.config.KafkaProducerConfig;
import farm.nurture.kafka.impl.KafkaProducer;
import lombok.extern.slf4j.Slf4j;

import java.util.Properties;

@Slf4j
public class TestGenericProducer {
    public static void main(String[] args) throws Exception {
        log.info("Starting producer for topics : communication_events");

        String servers = args[0];
        String topicName = args[1];
        Integer actorId = Integer.valueOf(args[2]);
        ActorType actorType = ActorType.valueOf(args[3]);
        String templateName = args[4];
        String communicationChanel = args[5];

        Properties props = new Properties();
        props.put("bootstrap.servers", servers);
        props.put("acks", "all");
        props.put("compression.type", "none");
        props.put("max.in.flight.requests.per.connection", 5);
        props.put("batch.size", 16384);
        props.put("linger.ms", 5);
        props.put("key.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer");

        KafkaProducerConfig config = new KafkaProducerConfig(props);
        Producer<byte[], byte[]> producer = new KafkaProducer<>(config);

        ActorID actorID = ActorID.newBuilder().setActorId(actorId).setActorType(actorType).build();

        CommunicationEvent event = CommunicationEvent.newBuilder()
                .setReceiverActor(actorID).setTemplateName(templateName).addChannel(CommunicationChannel.valueOf(communicationChanel))
                .addPlaceholder(Placeholder.newBuilder().setKey("app_link").setValue("https://www.google.com/").build())
                .addPlaceholder(Placeholder.newBuilder().setKey("token_id").setValue("100").build())
                .addPlaceholder(Placeholder.newBuilder().setKey("booking_id").setValue("200").build())
                .addPlaceholder(Placeholder.newBuilder().setKey("farmer_name").setValue("ABC Test").build())
                .addPlaceholder(Placeholder.newBuilder().setKey("farmer_mobile_number").setValue("1234567890").build())
                .addPlaceholder(Placeholder.newBuilder().setKey("invoice_link").setValue("https://www.google.com/").build())
                .addPlaceholder(Placeholder.newBuilder().setKey("name").setValue("LMN Test").build())
                .addPlaceholder(Placeholder.newBuilder().setKey("number").setValue("1345545466").build())
                .addPlaceholder(Placeholder.newBuilder().setKey("OTP").setValue("1234").build())
                .build();
        byte[] arr = event.toByteArray();
        producer.send(topicName, new Event<>(String.valueOf(1).getBytes(), event.toByteArray(), null, null));
    }
}
