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

import com.google.inject.internal.util.$Strings;
import farm.nurture.core.contracts.common.ActorID;
import farm.nurture.core.contracts.common.enums.*;
import farm.nurture.core.contracts.communication.engine.ActorDetails;
import farm.nurture.core.contracts.communication.engine.CommunicationEvent;
import farm.nurture.core.contracts.communication.engine.Media;
import farm.nurture.core.contracts.communication.engine.Placeholder;
import farm.nurture.kafka.Event;
import farm.nurture.kafka.Producer;
import farm.nurture.kafka.config.KafkaProducerConfig;
import farm.nurture.kafka.impl.KafkaProducer;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

@Slf4j
public class TestProducer {

    public static String IMAGE_URL = "https://afs-static-content.s3.ap-south-1.amazonaws.com/push_notification_campaign/trade_price.png";

    public static String VIDEO_URL = "https://www.buildquickbots.com/whatsapp/media/sample/video/sample01.mp4";
    public static String DOCUMENT_URL = "https://afs-static-content.s3.ap-south-1.amazonaws.com/whatsapp_campaigns/pop4.pdf";
    public static List<Placeholder> WHATSAPP_TEXT_PLACEHOLDER_LIST =
                  Arrays.asList(Placeholder.newBuilder().setKey("1").setValue("10").build(),
                        Placeholder.newBuilder().setKey("name").setValue("chalu pandey").build(),
                        Placeholder.newBuilder().setKey("2").setValue("10").build(),
                        Placeholder.newBuilder().setKey("name_mera").setValue("saaransh").build()
              );
    public static void main(String[] args) throws Exception {
        log.info("Starting producer for topics : communication_events");
        UUID uid = UUID.fromString("38400000-8cf0-11bd-b23e-10b96e4ef00d");
        log.info("UUID generated is: ", UUID.randomUUID().toString());

        Properties props = new Properties();
        props.put("bootstrap.servers", "localhost:9092");
        props.put("acks", "all");
        props.put("compression.type", "none");
        props.put("max.in.flight.requests.per.connection", 5);
        props.put("batch.size", 16384);
        props.put("linger.ms", 5);
        props.put("key.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer");

        KafkaProducerConfig config = new KafkaProducerConfig(props);
        Producer<byte[], byte[]> producer = new KafkaProducer<>(config);

//        Placeholder content = Placeholder.newBuilder().setKey("OTP").setValue("123456").build();
//        Placeholder content2 = Placeholder.newBuilder().setKey("sms_hash").setValue("8feru67O7").build();
//        Placeholder contentMetaData1 = Placeholder.newBuilder().setKey("image").setValue("https://www.gstatic.com/webp/gallery3/2.png").build();
//        Placeholder contentMetaData4 = Placeholder.newBuilder().setKey("someKey").setValue("someValue").build();

//        Placeholder contentMetaData2 = Placeholder.newBuilder().setKey("attachment_fileurl_1").setValue("https://afs-static-content.s3.ap-south-1.amazonaws.com/Operators_Controller_Issues_Guide_Punjabi.pdf").build();
//        Placeholder contentMetaData3 = Placeholder.newBuilder().setKey("attachment_filename_2").setValue("punjabi.pdf").build();
//        Placeholder contentMetaData6 = Placeholder.newBuilder().setKey("attachment_fileurl_1").setValue("https://afs-static-content.s3.ap-south-1.amazonaws.com/Gujrati_Operators_Controller_Issues_Guide.pdf").build();
//        Placeholder contentMetaData7 = Placeholder.newBuilder().setKey("attachment_filename_1").setValue("gujarati.pdf").build();

//
        ActorID actorID = ActorID.newBuilder().setActorId(413428).setActorType(ActorType.FARMER).build();
        ActorDetails actorDetails = ActorDetails.newBuilder()
//                .setAppId(AppID.NF_FARMER)
//                .setAppType(AppType.ANDROID)
//                .setFcmToken("dfP7q7McS_mvL5tV42ygZI:APA91bELR0ScSnYZ8A7JPzY1FZPvU2MpZ5d_z1zbAcHHhRXI3aoaeq9wxnnJHnMgsMxyUUCDZyas2LA43crdbClAHH8ne6RARKZ3M2NndkG0Bfy-1DNSnH2obx4bc2TAK6giqINFT3LD")
                .setMobileNumber("9993098893")
//                .setEmailId("kishan.ngm@gmail.com")
                .setLanguageCode(LanguageCode.EN_US).build();

        producer.send("communication_events", new Event<>(String.valueOf(1).getBytes(), test1(actorDetails).toByteArray(), null, null));
//        producer.send("communication_events", new Event<>(String.valueOf(1).getBytes(), test2(actorDetails).toByteArray(), null, null));
//        producer.send("communication_events", new Event<>(String.valueOf(1).getBytes(), test3(actorDetails).toByteArray(), null, null));
//        producer.send("communication_events", new Event<>(String.valueOf(1).getBytes(), test4(actorDetails).toByteArray(), null, null));
//        producer.send("communication_events", new Event<>(String.valueOf(1).getBytes(), test5(actorDetails).toByteArray(), null, null));
//        producer.send("communication_events", new Event<>(String.valueOf(1).getBytes(), test7(actorDetails).toByteArray(), null, null));
//        producer.send("communication_events", new Event<>(String.valueOf(1).getBytes(), test9(actorDetails).toByteArray(), null, null));
//        producer.send("communication_events", new Event<>(String.valueOf(1).getBytes(), test10(actorDetails).toByteArray(), null, null));
//        producer.send("communication_events", new Event<>(String.valueOf(1).getBytes(), test11(actorDetails).toByteArray(), null, null));
//        producer.send("communication_events", new Event<>(String.valueOf(1).getBytes(), test12(actorDetails).toByteArray(), null, null));
//        producer.send("communication_events", new Event<>(String.valueOf(1).getBytes(), test14(actorDetails).toByteArray(), null, null));
//        producer.send("communication_events", new Event<>(String.valueOf(1).getBytes(), test17(actorDetails).toByteArray(), null, null));
//        producer.send("communication_events", new Event<>(String.valueOf(1).getBytes(), test20(actorDetails).toByteArray(), null, null));
//        producer.send("communication_events", new Event<>(String.valueOf(1).getBytes(), test21(actorDetails).toByteArray(), null, null));

//        Placeholder content = Placeholder.newBuilder().setKey("OTP").setValue("1345").build();
//        Placeholder content2 = Placeholder.newBuilder().setKey("app_link").setValue("https://www.google.com").build();
//        ActorID actorID = ActorID.newBuilder().setActorId(413428).setActorType(ActorType.FARMER).build();

//        CommunicationEvent event = CommunicationEvent.newBuilder()
//                .setReceiverActor(actorID).setTemplateName("farmer_registration").addChannel(CommunicationChannel.APP_NOTIFICATION)
//                .addPlaceholder(content).addPlaceholder(content2).build();
//        byte[] arr = event.toByteArray();
//        producer.send("communication_events", new Event<>(String.valueOf(1).getBytes(), event.toByteArray(), null, null));
//
//        Placeholder content3 = Placeholder.newBuilder().setKey("bookingId").setValue("2047").build();
////        Placeholder content4 = Placeholder.newBuilder().setKey("serviceId").setValue("45").build();
//
//        CommunicationEvent event2 = CommunicationEvent.newBuilder()
////                .setReceiverActor(actorID).setTemplateName("BOOKING_COMPLETE").addChannel(CommunicationChannel.NOTIFICATION_INBOX)
//                .addPlaceholder(content3).setReferenceId("uy234234bb33jphnfyndmas").build();
//        byte[] arr2 = event2.toByteArray();
//        producer.send("communication_events", new Event<>(String.valueOf(1).getBytes(), arr2, null, null));

//        Placeholder content = Placeholder.newBuilder().setKey("otp_value").setValue("1345").build();
//        Placotp_valueeholder content2 = Placeholder.newBuilder().setKey("app_link").setValue("https://www.google.com").build();
//        ActorID actorID = ActorID.newBuilder().build();
//        ActorDetails actorDetails = ActorDetails.newBuilder().setEmailId("kishan.ngm@gmail.com").setLanguageCode(LanguageCode.EN_US).build();

//        CommunicationEvent event = CommunicationEvent.newBuilder()
//                .setReceiverActor(actorID)
//                .setTemplateName("farmer_registration").addChannel(CommunicationChannel.SMS)
//                .addPlaceholder(content).addPlaceholder(content2).build();
//        byte[] arr = event.toByteArray();
//        producer.send("communication_events", new Event<>(String.valueOf(1).getBytes(), event.toByteArray(), null, null));

//        String title = "Hannaford Farm Exchange Email Verification";
//        CommunicationEvent event = CommunicationEvent.newBuilder()
//                .setReceiverActor(actorID)
//                .setTemplateName("hannaford_farm_verification").addChannel(CommunicationChannel.EMAIL)
//                .setContentTitle(title)
//                .setReceiverActorDetails(actorDetails)
//                .addPlaceholder(content)//.addPlaceholder(content2)
//                .build();
//        byte[] arr = event.toByteArray();
//        producer.send("communication_events", new Event<>(String.valueOf(1).getBytes(), event.toByteArray(), null, null));

//        Placeholder content = Placeholder.newBuilder().setKey("otp_value").setValue("1345").build();
//        ActorDetails actorDetails = ActorDetails.newBuilder().setMobileNumber("9591373075").setLanguageCode(LanguageCode.EN_US).build();
////        String title = "Hannaford Farm Exchange Email Verification";
//        CommunicationEvent event = CommunicationEvent.newBuilder()
////                .setReceiverActor(actorID)
//                .setTemplateName("farmer_booking_reject").addChannel(CommunicationChannel.SMS)
////                .setContentTitle(title)
//                .setReceiverActorDetails(actorDetails)
////                .addPlaceholder(content)//.addPlaceholder(content2)
//                .build();
//        byte[] arr = event.toByteArray();
//        producer.send("communication_events", new Event<>(String.valueOf(1).getBytes(), event.toByteArray(), null, null));


//        CommunicationEvent event = CommunicationEvent.newBuilder()
//                .setReceiverActor(actorID).setTemplateName("test").addChannel(CommunicationChannel.SMS)
//                .addPlaceholder(content).setContentTitle("nurture.farm OTP").build();
//        event.toByteArray();
//        producer.send("communication_events", new Event<>(String.valueOf(1).getBytes(), event.toByteArray(), null, null));
//
//        CommunicationEvent event = CommunicationEvent.newBuilder()
//                .setReceiverActor(actorID).setTemplateName("test").addChannel(CommunicationChannel.APP_NOTIFICATION)
//                .addPlaceholder(content).setContentTitle("nurture.farm OTP").build();
//        event.toByteArray();
//        producer.send("communication_events", new Event<>(String.valueOf(1).getBytes(), event.toByteArray(), null, null));
    }
    private static CommunicationEvent test1(ActorDetails actorDetails)
    {
        CommunicationEvent event = CommunicationEvent.newBuilder()
                .setReceiverActorDetails(actorDetails)
                .setTemplateName("test123")
                .addChannel(CommunicationChannel.WHATSAPP)
                .setVendor(CommunicationVendor.GUPSHUP)
                .setReferenceId(UUID.randomUUID().toString()).build();
        return event;
    }
    private static CommunicationEvent test2(ActorDetails actorDetails)
    {
        CommunicationEvent event = CommunicationEvent.newBuilder()
                .setReceiverActorDetails(actorDetails)
                .setTemplateName("test2")
                .addChannel(CommunicationChannel.WHATSAPP)
                .setVendor(CommunicationVendor.KARIX)
                .addAllPlaceholder(WHATSAPP_TEXT_PLACEHOLDER_LIST)
                .setReferenceId(UUID.randomUUID().toString()).build();

        return event;
    }
    private static CommunicationEvent test3(ActorDetails actorDetails)
    {
        CommunicationEvent event = CommunicationEvent.newBuilder()
                .setReceiverActorDetails(actorDetails)
                .setTemplateName("test3")
                .addChannel(CommunicationChannel.WHATSAPP)
                .setVendor(CommunicationVendor.KARIX)
                .setReferenceId(UUID.randomUUID().toString()).build();

        return event;
    }  private static CommunicationEvent test4(ActorDetails actorDetails)
    {
        CommunicationEvent event = CommunicationEvent.newBuilder()
                .setReceiverActorDetails(actorDetails)
                .setTemplateName("test4")
                .addAllPlaceholder(Arrays.asList(Placeholder.newBuilder().setKey("name").setValue("chalu pandey").build()))
                .addChannel(CommunicationChannel.WHATSAPP)
                .setVendor(CommunicationVendor.KARIX)
                .setReferenceId(UUID.randomUUID().toString()).build();

        return event;
    }
    private static CommunicationEvent test5(ActorDetails actorDetails)
    {
        CommunicationEvent event = CommunicationEvent.newBuilder()
                .setReceiverActorDetails(actorDetails)
                .setTemplateName("test5")
                .addChannel(CommunicationChannel.WHATSAPP)
                .setVendor(CommunicationVendor.KARIX)
                .setReferenceId(UUID.randomUUID().toString()).build();

        return event;
    }
    private static CommunicationEvent test9(ActorDetails actorDetails)
    {
        CommunicationEvent event = CommunicationEvent.newBuilder()
                .setReceiverActorDetails(actorDetails)
                .setTemplateName("test9")
                .addChannel(CommunicationChannel.WHATSAPP)
                .setVendor(CommunicationVendor.KARIX)
                .setReferenceId(UUID.randomUUID().toString()).build();

        return event;
    } private static CommunicationEvent test10(ActorDetails actorDetails)
    {
        CommunicationEvent event = CommunicationEvent.newBuilder()
                .setReceiverActorDetails(actorDetails)
                .setTemplateName("test10")
                .addChannel(CommunicationChannel.WHATSAPP)
                .setVendor(CommunicationVendor.KARIX)
                .setReferenceId(UUID.randomUUID().toString()).build();

        return event;
    }
    private static CommunicationEvent test7(ActorDetails actorDetails)
    {
        CommunicationEvent event = CommunicationEvent.newBuilder()
                .setReceiverActorDetails(actorDetails)
                .setTemplateName("test7")
                .addChannel(CommunicationChannel.WHATSAPP)
                .setVendor(CommunicationVendor.KARIX)
                .setReferenceId(UUID.randomUUID().toString()).build();

        return event;
    }private static CommunicationEvent test11(ActorDetails actorDetails)
    {
        CommunicationEvent event = CommunicationEvent.newBuilder()
                .setReceiverActorDetails(actorDetails)
                .setTemplateName("test11")
                .addChannel(CommunicationChannel.WHATSAPP)
                .setVendor(CommunicationVendor.KARIX)
                .addAllPlaceholder(WHATSAPP_TEXT_PLACEHOLDER_LIST)
                .setReferenceId(UUID.randomUUID().toString()).build();

        return event;
    }private static CommunicationEvent test12(ActorDetails actorDetails)
    {
        CommunicationEvent event = CommunicationEvent.newBuilder()
                .setReceiverActorDetails(actorDetails)
                .setTemplateName("test12")
                .addChannel(CommunicationChannel.WHATSAPP)
                .setVendor(CommunicationVendor.KARIX)
                .addAllPlaceholder(WHATSAPP_TEXT_PLACEHOLDER_LIST)
                .setReferenceId(UUID.randomUUID().toString()).build();

        return event;
    }private static CommunicationEvent test14(ActorDetails actorDetails)
    {
        CommunicationEvent event = CommunicationEvent.newBuilder()
                .setReceiverActorDetails(actorDetails)
                .setTemplateName("test14")
                .addChannel(CommunicationChannel.WHATSAPP)
                .setVendor(CommunicationVendor.KARIX)
                .addAllPlaceholder(WHATSAPP_TEXT_PLACEHOLDER_LIST)
                .setMedia(Media.newBuilder()
                        .setMediaType(MediaType.VIDEO)
                        .setMediaAccessType(MediaAccessType.PUBLIC_URL)
                        .setMediaInfo(VIDEO_URL))
                .setReferenceId(UUID.randomUUID().toString()).build();

        return event;
    }
    private static CommunicationEvent test17(ActorDetails actorDetails)
    {
        CommunicationEvent event = CommunicationEvent.newBuilder()
                .setReceiverActorDetails(actorDetails)
                .setTemplateName("test17")
                .addChannel(CommunicationChannel.WHATSAPP)
                .setVendor(CommunicationVendor.KARIX)
                .setMedia(Media.newBuilder()
                        .setMediaType(MediaType.IMAGE)
                        .setMediaAccessType(MediaAccessType.PUBLIC_URL)
                        .setMediaInfo(IMAGE_URL))
                .setReferenceId(UUID.randomUUID().toString()).build();

        return event;
    }
    private static CommunicationEvent test20(ActorDetails actorDetails)
    {
        CommunicationEvent event = CommunicationEvent.newBuilder()
                .setReceiverActorDetails(actorDetails)
                .setTemplateName("test20")
                .addChannel(CommunicationChannel.WHATSAPP)
                .setVendor(CommunicationVendor.KARIX)
                .addAllPlaceholder(WHATSAPP_TEXT_PLACEHOLDER_LIST)
                .setMedia(Media.newBuilder()
                        .setMediaType(MediaType.DOCUMENT)
                        .setMediaAccessType(MediaAccessType.PUBLIC_URL)
                        .setDocumentName("file")
                        .setMediaInfo(DOCUMENT_URL))
                .setReferenceId(UUID.randomUUID().toString()).build();

        return event;
    }
    private static CommunicationEvent test21(ActorDetails actorDetails)
    {
        CommunicationEvent event = CommunicationEvent.newBuilder()
                .setReceiverActorDetails(actorDetails)
                .setTemplateName("test21")
                .addChannel(CommunicationChannel.WHATSAPP)
                .setVendor(CommunicationVendor.KARIX)
                .addAllPlaceholder(WHATSAPP_TEXT_PLACEHOLDER_LIST)
                .setMedia(Media.newBuilder()
                        .setMediaType(MediaType.DOCUMENT)
                        .setMediaAccessType(MediaAccessType.PUBLIC_URL)
                        .setDocumentName("file")
                        .setMediaInfo(DOCUMENT_URL))
                .setReferenceId(UUID.randomUUID().toString()).build();
        return event;
    }


}
