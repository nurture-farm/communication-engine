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

import com.google.gson.Gson;
import farm.nurture.communication.engine.dto.InteractiveAttributes;
import farm.nurture.communication.engine.models.MessageAcknowledgement;
import farm.nurture.communication.engine.repository.MessageAcknowledgementRepository;
import farm.nurture.core.contracts.common.ActorID;
import farm.nurture.core.contracts.common.Attribs;
import farm.nurture.core.contracts.common.RequestHeaders;
import farm.nurture.core.contracts.common.enums.*;
import farm.nurture.core.contracts.communication.engine.*;
import farm.nurture.infra.util.ApplicationConfiguration;
import farm.nurture.laminar.core.io.sql.dao.DbConfig;
import farm.nurture.laminar.core.io.sql.dao.PoolFactory;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;


@Slf4j
public class TestCommunication {

    private static final String LOCAL_URL = "127.0.0.1:8010";
    private static final String DEV_URL = "internal-a2d376e6948514a73a514e3584160823-409853754.ap-south-1.elb.amazonaws.com";
    private static final String STAGE_URL = "internal-a49872e2b408d43d28ce6000e575e84b-437549400.ap-south-1.elb.amazonaws.com";
    private static final String PROD_URL = "internal-a85f3a2f550f84f578459537c8ae3304-113299092.ap-south-1.elb.amazonaws.com";
    private static ManagedChannel channel;
    private static CommunicationEnginePlatformGrpc.CommunicationEnginePlatformBlockingStub platformBlockingStub;
    private static CommunicationEngineGrpc.CommunicationEngineBlockingStub blockingStub;
    private static String SMS_TEMPLATE_NAME = "notify_me_msg";
    private static String WHATSAPP_TEXT_TEMPLATE_NAME = "dev_test_footer_5";
    private static String WHATSAPP_IMAGE_TEMPLATE_NAME = "soil_turn_reminder";
    private static String WHATSAPP_VIDEO_TEMPLATE_NAME = "dsr_signup";
    private static String WHATSAPP_DOC_TEMPLATE_NAME =   "awd_pop_2_campaign_whatsapp"; // "pop1_awd";
    private static String PUSH_NOTIFICATION_TEMPLATE_NAME = "notify_me_msg";
    private static String TEST_MOBILE_NUMBER = "9993098893";
    private static String IMAGE_URL = "https://afs-static-content.s3.ap-south-1.amazonaws.com/push_notification_campaign/trade_price.png";
    private static String DOCUMENT_URL = "https://afs-static-content.s3.ap-south-1.amazonaws.com/whatsapp_campaigns/pop4.pdf"; //"https://www.buildquickbots.com/whatsapp/media/sample/pdf/sample01.pdf";
    private static String VIDEO_URL = "https://www.buildquickbots.com/whatsapp/media/sample/video/sample01.mp4";
    private static ActorDetails actorDetails = ActorDetails.newBuilder()
            .setMobileNumber(TEST_MOBILE_NUMBER)
            .setLanguageCode(LanguageCode.EN_US)
            .build();
    private static ActorID actorID = ActorID.newBuilder().setActorId(6646919).setActorType(ActorType.FARMER).build();

    private static final MessageAcknowledgementRepository messageAcknowledgementRepository = new MessageAcknowledgementRepository();

    private static List<Placeholder> SMS_PLACEHOLDER_LIST = Arrays.asList(
            Placeholder.newBuilder().setKey("PRODUCT_NAME").setValue("Pusa Spray").build());
         //   Placeholder.newBuilder().setKey("farmer_first_name").setValue("XX").build(),
          //  Placeholder.newBuilder().setKey("points_earned").setValue("100").build());
    private static List<Placeholder> WHATSAPP_TEXT_PLACEHOLDER_LIST =
            Arrays.asList(
                    Placeholder.newBuilder().setKey("farmer_first_name").setValue("Saaransh").build()
                    //,
                //    Placeholder.newBuilder().setKey("2").setValue("1234").build(),
                //    Placeholder.newBuilder().setKey("3").setValue("xyz form").build()
            );
    /**
     * {
     *     "content_metadata_click_action": "FLUTTER_NOTIFICATION_CLICK",
     *     "content_metadata_messageCode": "1006",
     *     "content_metadata_tracking_data": "{\"campaignName\":\"TEST_CAMPAIGN\"}",
     *     "api_key": "AAAAbpLXgEE:APA91bF4Ki4rYP9wlPBO7y5iOOXx6zixoXTUA2HSsRjrxPFxxkwzDtbhSTGg--DjAt1ZTAxzRAjGyesPAH-K2295aZ1720EMEhgkimYzORY8oIg8Hm-GPvd4kPXmUmoItnqdQTplsPG7",
     *     "content_metadata_image": "https://afs-qa-client.s3.ap-south-1.amazonaws.com/push_notification_campaign/Cotton+Field+Banner-Punjabi-02.jpg",
     *     "content_metadata_cta": "/showDetails?requestUrl=farm-selection?farmSelectionServiceType%3DBOOKING&title=Fetching+farms&enableCaching=false&displayIn=bottomSheet&campaign=pn"
     * }
     */
    private static List<Placeholder> WHATSAPP_IMAGE_PLACEHOLDER_LIST = Collections.singletonList(
            Placeholder.newBuilder().setKey("link").setValue("https://nrf.page.link/ppapd").build());
    private static List<Placeholder> PUSH_NOTIFICATION_LIST = Arrays.asList(
            Placeholder.newBuilder().setKey("PRODUCT_NAME").setValue("Pusa Spray").build(),
           Placeholder.newBuilder().setKey("link").setValue("https://nrf.page.link/weather").build()
           // Placeholder.newBuilder().setKey("cta").setValue("/showDetails?requestUrl=farm-selection?farmSelectionServiceType%3DBOOKING&title=Fetching+farms&enableCaching=false&displayIn=bottomSheet&campaign=pn").build(),
           // Placeholder.newBuilder().setKey("messageCode").setValue("1006").build(),
          //  Placeholder.newBuilder().setKey("click_action").setValue("FLUTTER_NOTIFICATION_CLICK").build(),
          // Placeholder.newBuilder().setKey("image").setValue("https://afs-static-content.s3.ap-south-1.amazonaws.com/push_notification_campaign/Cotton%2BField%2BBanner-Hindi-01-min-min_1_11zon.jpeg").build()
          //  Placeholder.newBuilder().setKey("image_EN_US").setValue("https://afs-static-content.s3.ap-south-1.amazonaws.com/push_notification_campaign/pn_quiz_eng.jpeg").build(),
       //     Placeholder.newBuilder().setKey("image_HI_IN").setValue("https://afs-static-content.s3.ap-south-1.amazonaws.com/push_notification_campaign/pn_quiz_hindi.jpeg").build()
    );

    static {
        channel = ManagedChannelBuilder.forTarget(STAGE_URL).usePlaintext().build();
        platformBlockingStub = CommunicationEnginePlatformGrpc.newBlockingStub(channel);
        blockingStub = CommunicationEngineGrpc.newBlockingStub(channel);
    }


    @Before
    public void init() {
        initializeDatabase();
        new DIModule();
    }

    private void initializeDatabase() {
        ApplicationConfiguration appConfig = ApplicationConfiguration.getInstance();
        DbConfig dbConfig = new DbConfig();
        dbConfig.connectionUrl = appConfig.get("dbprod.connection.url");
        dbConfig.login = appConfig.get("dbprod.username");
        dbConfig.password = appConfig.get("dbprod.password");
        dbConfig.driverClass = appConfig.get("dbprod.driver.class");
        dbConfig.poolName = appConfig.get("dbprod.connection.pool.name", "communication_rw");
        dbConfig.idleConnections = appConfig.getInt("dbprod.idle.connections", 2);
        dbConfig.maxConnections = appConfig.getInt("dbprod.max.connections", 10);
        dbConfig.incrementBy = appConfig.getInt("dbprod.connection.increment.by", 2);
        dbConfig.testConnectionOnBorrow = true;
        PoolFactory.getInstance().setup(dbConfig);
    }

    @Test
    public void testCommunication() throws Exception {

        List<ResponseResult> responseResultList = new ArrayList<>();
        try {
            /*responseResultList.add(new ResponseResult(smsGupshupTest(), CommunicationChannel.SMS.name(), "Sms Gupshup Test"));
            responseResultList.add(new ResponseResult(smsKarixTest(), CommunicationChannel.SMS.name(), "Sms Karix Test"));


            responseResultList.add(new ResponseResult(whatsAppTextGupshupTest(), CommunicationChannel.WHATSAPP.name(), "Test Whatsapp Text Gupshup"));
            responseResultList.add(new ResponseResult(whatsAppTextKarixTest(), CommunicationChannel.WHATSAPP.name(), "Test Whatsapp Text Karix"));
           responseResultList.add(new ResponseResult(whatsAppImageGupshupTest(), CommunicationChannel.WHATSAPP.name(), "Test Whatsapp Image Gupshup"));
            responseResultList.add(new ResponseResult(whatsAppImageKarixTest(), CommunicationChannel.WHATSAPP.name(), "Test Whatsapp Image Karix"));
            responseResultList.add(new ResponseResult(whatsAppDocumentGupshupTest(), CommunicationChannel.WHATSAPP.name(), "Test Whatsapp Doc Gupshup"));
            responseResultList.add(new ResponseResult(whatsAppDocumentKarixTest(), CommunicationChannel.WHATSAPP.name(), "Test Whatsapp Doc Karix"));
            responseResultList.add(new ResponseResult(whatsAppVideoGupshupTest(), CommunicationChannel.WHATSAPP.name(), "Test Whatsapp Video Gupshup"));
            responseResultList.add(new ResponseResult(whatsAppVideoKarixTest(), CommunicationChannel.WHATSAPP.name(), "Test Whatsapp Video Karix"));

           responseResultList.add(new ResponseResult(pushNotificationTest(), CommunicationChannel.APP_NOTIFICATION.name(), "Test Push Notification"));

            Thread.sleep(500);
            responseResultList.forEach(TestCommunication::testCommunication);*/
            testCreateTemplate();
        } finally {
            channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        }

    }

    /**
     * SMS TEST FOR GUPSHUP VENDOR
     */
    private static String smsGupshupTest() {
        CommunicationEvent event = getCommunicationBuilder(actorDetails, SMS_TEMPLATE_NAME, CommunicationChannel.SMS,
                SMS_PLACEHOLDER_LIST, CommunicationVendor.GUPSHUP).build();
        return sendCommunication(event);
    }


    /**
     * SMS TEST FOR KARIX VENDOR
     **/
    private static String smsKarixTest() {
        CommunicationEvent event = getCommunicationBuilder(actorDetails, SMS_TEMPLATE_NAME, CommunicationChannel.SMS,
                SMS_PLACEHOLDER_LIST, CommunicationVendor.KARIX).build();
        return sendCommunication(event);
    }

    /**
     * WHATSAPP TEXT TEST FOR GUPSHUP VENDOR
     **/
    private static String whatsAppTextGupshupTest() {
        CommunicationEvent event = getCommunicationBuilder(actorDetails, WHATSAPP_TEXT_TEMPLATE_NAME, CommunicationChannel.WHATSAPP, WHATSAPP_TEXT_PLACEHOLDER_LIST,
                CommunicationVendor.GUPSHUP).build();
        return sendCommunication(event);
    }

    /**
     * WHATSAPP TEXT TEST FOR KARIX VENDOR
     **/
    private static String whatsAppTextKarixTest() {
        CommunicationEvent event = getCommunicationBuilder(actorDetails, WHATSAPP_TEXT_TEMPLATE_NAME, CommunicationChannel.WHATSAPP, WHATSAPP_TEXT_PLACEHOLDER_LIST,
                CommunicationVendor.KARIX).build();
        return sendCommunication(event);
    }

    /**
     * WHATSAPP IMAGE TEST FOR GUPSHUP VENDOR
     **/

    private static String whatsAppImageGupshupTest() {
        CommunicationEvent.Builder builder = getCommunicationBuilder(actorDetails, WHATSAPP_IMAGE_TEMPLATE_NAME,
                CommunicationChannel.WHATSAPP, WHATSAPP_IMAGE_PLACEHOLDER_LIST, CommunicationVendor.GUPSHUP);

        CommunicationEvent event = builder.setMedia(Media.newBuilder()
                .setMediaType(MediaType.IMAGE)
                .setMediaAccessType(MediaAccessType.PUBLIC_URL)
                .setMediaInfo(IMAGE_URL)).build();
        return sendCommunication(event);
    }

    /**
     * WHATSAPP IMAGE TEST FOR KARIX VENDOR
     **/
    private static String whatsAppImageKarixTest() {
        CommunicationEvent.Builder builder = getCommunicationBuilder(actorDetails, WHATSAPP_IMAGE_TEMPLATE_NAME,
                CommunicationChannel.WHATSAPP, WHATSAPP_IMAGE_PLACEHOLDER_LIST, CommunicationVendor.KARIX);

        CommunicationEvent event = builder.setMedia(Media.newBuilder()
                .setMediaType(MediaType.IMAGE)
                .setMediaAccessType(MediaAccessType.PUBLIC_URL)
                .setMediaInfo(IMAGE_URL)).build();
        return sendCommunication(event);
    }



    /**
     * WHATSAPP DOC TEST FOR GUPSHUP VENDOR
     **/
    private static String whatsAppDocumentGupshupTest() {
        ActorDetails actorDetails = ActorDetails.newBuilder()
                .setMobileNumber(TEST_MOBILE_NUMBER)
                .setLanguageCode(LanguageCode.TE)
                .build();
        CommunicationEvent.Builder builder = getCommunicationBuilder(actorDetails, WHATSAPP_DOC_TEMPLATE_NAME,
                CommunicationChannel.WHATSAPP, null, CommunicationVendor.GUPSHUP);
        CommunicationEvent event = builder.setMedia(Media.newBuilder()
                .setMediaType(MediaType.DOCUMENT)
                .setMediaAccessType(MediaAccessType.PUBLIC_URL)
                .setDocumentName("file")
                .setMediaInfo(DOCUMENT_URL)).build();
        return sendCommunication(event);
    }

    /**
     * WHATSAPP DOC TEST FOR KARIX VENDOR
     **/
    private static String whatsAppDocumentKarixTest() {
        CommunicationEvent.Builder builder = getCommunicationBuilder(actorDetails, WHATSAPP_DOC_TEMPLATE_NAME,
                CommunicationChannel.WHATSAPP, null, CommunicationVendor.KARIX);
        CommunicationEvent event = builder.setMedia(Media.newBuilder()
                .setMediaType(MediaType.DOCUMENT)
                .setDocumentName("")
                .setMediaAccessType(MediaAccessType.PUBLIC_URL)
                .setMediaInfo(DOCUMENT_URL)).build();
        return sendCommunication(event);
    }

    /**
     * WHATSAPP VIDEO TEST FOR GUPSHUP VENDOR
     **/
    private static String whatsAppVideoGupshupTest() {
        ActorDetails actorDetails = ActorDetails.newBuilder()
                .setMobileNumber(TEST_MOBILE_NUMBER)
                .setLanguageCode(LanguageCode.TE)
                .build();
        CommunicationEvent.Builder builder = getCommunicationBuilder(actorDetails, WHATSAPP_VIDEO_TEMPLATE_NAME,
                CommunicationChannel.WHATSAPP, null, CommunicationVendor.GUPSHUP);
        CommunicationEvent event = builder.setMedia(Media.newBuilder()
                .setMediaType(MediaType.VIDEO)
                .setMediaAccessType(MediaAccessType.PUBLIC_URL)
                .setMediaInfo(VIDEO_URL)).build();
        return sendCommunication(event);
    }

    /**
     * WHATSAPP VIDEO TEST FOR KARIX VENDOR
     **/
    private static String whatsAppVideoKarixTest() {
        CommunicationEvent.Builder builder = getCommunicationBuilder(actorDetails, WHATSAPP_VIDEO_TEMPLATE_NAME,
                CommunicationChannel.WHATSAPP, null, CommunicationVendor.KARIX);
        CommunicationEvent event = builder.setMedia(Media.newBuilder()
                .setMediaType(MediaType.VIDEO)
                .setMediaAccessType(MediaAccessType.PUBLIC_URL)
                .setMediaInfo(VIDEO_URL)).build();
        return sendCommunication(event);
    }

    /**
     * PUSH NOTIFICATION TEST FOR FIRBASE VENDOR
     **/
    private static String pushNotificationTest() {
       ActorDetails actorDetails = ActorDetails.newBuilder()
                .setFcmToken("fpGeHifXSQOXYIJXjDkdjf:APA91bGP6f4pP9wQACOsvA0qkow-NoPS71orndY3yzW6rA-1_uJNbjiWBvtRM_dIThhDkMomYxW3hUO9sCxMZ3LX4hvYdEN0Z6nrH8wXL2CQOw7wFU2_uA5hnC2nHAVDii25jypt7AUw")
               .setAppId(AppID.NF_RETAILER)
               .setAppType(AppType.ANDROID)
                .setLanguageCode(LanguageCode.EN_US)
                .build();
        CommunicationEvent event = CommunicationEvent.newBuilder()
              .setReceiverActorDetails(actorDetails).
                addAllContentMetadata(PUSH_NOTIFICATION_LIST)
                .addChannel(CommunicationChannel.APP_NOTIFICATION)
                .setTemplateName(PUSH_NOTIFICATION_TEMPLATE_NAME)
                .setChannelAttributes(
                        CommunicationChannelAttributes.newBuilder().
                                setPushNotificationType(PushNotificationType.NO_PUSH_NOTIFICATION_TYPE).build())
                .build();
        return sendCommunication(event);
    }

    private static String sendCommunication(CommunicationEvent event) {
        blockingStub = CommunicationEngineGrpc.newBlockingStub(channel);
        CommunicationResponse resp = blockingStub.sendCommunication(event);
        return resp.getReferenceId();
    }

    private static CommunicationEvent.Builder getCommunicationBuilder(ActorDetails actorDetails, String templateName,
                                                                      CommunicationChannel communicationChannel, List<Placeholder> placeholderList, CommunicationVendor communicationVendor) {
        CommunicationEvent.Builder builder = CommunicationEvent.newBuilder()
                .setReceiverActorDetails(actorDetails)
                .setTemplateName(templateName)
                .addChannel(communicationChannel)
                .setVendor(communicationVendor);
        if (placeholderList != null) {
            builder.addAllPlaceholder(placeholderList);
        }
        return builder;
    }


    private static void testCommunication(ResponseResult responseResult) {
        boolean actualValue = checkCommunication(responseResult);
        try{
            assertEquals(true, actualValue, "FAILED - " + responseResult.getMethodName());
            log.info("PASSED - {}", responseResult.getMethodName());
        }
        catch(Throwable exception){
            log.error("FAILED - {}", responseResult.getMethodName(), exception);
        }
    }

    private static boolean checkCommunication(ResponseResult responseResult) {
        List<farm.nurture.communication.engine.models.MessageAcknowledgement> messageAcknowledgementList =
                messageAcknowledgementRepository.getGetMessageAcknowledgementEnableByReferenceIdAndCommunicationChannel(responseResult.getReferenceId(),
                        responseResult.getCommunicationChannel());
        if (messageAcknowledgementList == null || messageAcknowledgementList.size() != 0) {
            farm.nurture.communication.engine.models.MessageAcknowledgement messageAcknowledgement = messageAcknowledgementList.get(0);
            return messageAcknowledgement.getState() != MessageAcknowledgement.State.VENDOR_UNDELIVERED;
        }
        return false;
    }

    private static void testOptIn(String mobileNumber) {
        System.out.println("mobile Number " + mobileNumber.substring(1, mobileNumber.length() - 1));

        OptInRequest optInRequest = OptInRequest.newBuilder()
                .setRequestHeaders(RequestHeaders.newBuilder().setLanguageCode(LanguageCode.EN_US).build())
                .setMobileNumber(mobileNumber.substring(1, mobileNumber.length() - 1))
                .setActor(ActorID.newBuilder().setActorType(ActorType.FARMER).setActorId(41348))
                .setSourceSystem(SourceSystem.FARM_APP)
                .setNameSpace(NameSpace.FARM)
                .build();

        platformBlockingStub = CommunicationEnginePlatformGrpc.newBlockingStub(channel);
        OptInRespone resp = platformBlockingStub.optInUser(optInRequest);
        System.out.println(resp);
    }

    private static void testCreateTemplate(){
//        InteractiveAttributes interactiveAttributes = InteractiveAttributes.builder()
//                . headers("Test Header {{h1}}").header_examples("h1")
//                .footer("Stop using our services")
//                .button_category("QuickReply")
//                .buttons(Arrays.asList(InteractiveAttributes.Button.builder().text("Agree").build()
//                        , InteractiveAttributes.Button.builder().text("Disagree").build(),
//                        InteractiveAttributes.Button.builder().text("Not Sure").build()
//                ))
//                .build();

//
//        InteractiveAttributes interactiveAttributes = InteractiveAttributes.builder()
//             //   . headers("Test Header")
//             //   .footer("Stop using our services")
//                .button_category("CallToAction")
//                .buttons(Arrays.asList(
//                        InteractiveAttributes.Button.builder().text("Call Customer Care")
//                                .type("phone_number").phone_number("+919993098893")
//                                .build()
//                        , InteractiveAttributes.Button.builder().text("Visit Nurture Farm")
//                                .type("url").urlType("static").url("https://www.nurture.farm")
//                                .build()
//                )).build();

        InteractiveAttributes interactiveAttributes = InteractiveAttributes.builder()
              //  . headers("Dev Header {{h1}}")
               // .header_examples("h1")
                .footer("To unsubscribe our service, Type STOP")
                .button_category("CallToAction")
                .buttons(Arrays.asList(InteractiveAttributes.Button.builder().text("Call Customer Care")
                                .type("phone_number").phone_number("+919993098893")
                                .build(),
                        InteractiveAttributes.Button.builder().text("Visit Nurture Farm")
                                .type("url").urlType("static").url("https://nurture.farm/")
                                .build()))
                                .build();
        String interAttrb  =  new Gson().toJson(interactiveAttributes);
        /*Test-6 normal template with quick reply button */


        /*Test media template with image */
        AddTemplateRequest test10 = AddTemplateRequest.newBuilder().
                setChannel(CommunicationChannel.WHATSAPP)
                .setContent("this is test template {{farmer_first_name}} and {{link}}")
                .setLanguageCode(LanguageCode.EN_US)
                .setName("test_media_template_image_karix_one")
                .setTemplateContentType(TemplateContentType.STRING)
                .setOwner("honey@nurture.farm")
                .setVertical("NURTURE_FARM")
                .addAllAttribs(Arrays.asList(
                        Attribs.newBuilder().setKey("0").setValue("farmer_first_name").build(),
                        Attribs.newBuilder().setKey("1").setValue("link").build()
                ))
                .addAllMetaData(Arrays.asList(
                        Attribs.newBuilder().setKey("interactive_attributes").setValue(interAttrb).build(),
                        Attribs.newBuilder().setKey("media_type").setValue("image").build()
                ))
                .build();


        /*Test-5 normal template with footer*/
        AddTemplateRequest test5 = AddTemplateRequest.newBuilder().
                setChannel(CommunicationChannel.WHATSAPP)
                .setContent("Hello {{farmer_first_name}}, \n This is test of template containing footer with header and placeholder and Call to action button of both type url & phone number. Click on below link {{link}}")
                .setLanguageCode(LanguageCode.EN_US)
                .setName("test_call_to_action_header_footer_ps")
                .setTemplateContentType(TemplateContentType.STRING)
                .setOwner("honey@nurture.farm")
                .setVertical("NURTURE_FARM")
                .addAllAttribs(Arrays.asList(
                        Attribs.newBuilder().setKey("0").setValue("farmer_first_name").build(),
                        Attribs.newBuilder().setKey("1").setValue("link").build()
                ))
                .addAllMetaData(Arrays.asList(
                        Attribs.newBuilder().setKey("interactive_attributes").setValue(interAttrb).build(),
                        Attribs.newBuilder().setKey("media_type").setValue("text").build()
                ))
                .build();

        /*TEST-4*/
        AddTemplateRequest test4 = AddTemplateRequest.newBuilder().
                setChannel(CommunicationChannel.WHATSAPP)
                .setContent("Good morning {{farmer_first_name}}, This is test template for second time testing, ignore the given link {{link}}")
                .setLanguageCode(LanguageCode.EN_US)
                .setName("test_template_with_header_ps_ps_2")
                .setTemplateContentType(TemplateContentType.STRING)
                .setOwner("honey@nurture.farm")
                .setVertical("NURTURE_FARM")
                .addAllAttribs(Arrays.asList(
                        Attribs.newBuilder().setKey("0").setValue("farmer_first_name").build(),
                        Attribs.newBuilder().setKey("1").setValue("link").build()
                ))
                .addAllMetaData(Arrays.asList(
                        Attribs.newBuilder().setKey("interactive_attributes").setValue(interAttrb).build(),
                        Attribs.newBuilder().setKey("media_type").setValue("text").build()
                ))
                .build();


        /*TEST-3*/
        AddTemplateRequest addTemplateRequest = AddTemplateRequest.newBuilder().
                setChannel(CommunicationChannel.WHATSAPP)
                .setContent("Hello {{farmer_first_name}}, This is a test template for testing interactive attributes containing header, header's placeholder and placeholder. Click on the given link {{link}}")
                .setLanguageCode(LanguageCode.EN_US)
                .setName("test_template_with_header_ps_ps")
                .setTemplateContentType(TemplateContentType.STRING)
                .setOwner("honey@nurture.farm")
                .setVertical("NURTURE_FARM")
                .addAllAttribs(Arrays.asList(
                        Attribs.newBuilder().setKey("0").setValue("farmer_first_name").build(),
                        Attribs.newBuilder().setKey("1").setValue("link").build()
                ))
                .addAllMetaData(Arrays.asList(
                       Attribs.newBuilder().setKey("interactive_attributes").setValue(interAttrb).build(),
                        Attribs.newBuilder().setKey("media_type").setValue("text").build()
                ))
                .build();

        /*TEST-2*/
        AddTemplateRequest testNormalTemplate = AddTemplateRequest.newBuilder().
                setChannel(CommunicationChannel.WHATSAPP)
                .setContent("Hello Users, this is the test 1 for normal template text with placeholders {{p1}} and {{p2}} for testing. Please ignore the message")
                .setLanguageCode(LanguageCode.EN_US)
                .setName("normal_template_test_p")
                .setTemplateContentType(TemplateContentType.STRING)
                .setOwner("honey@nurture.farm")
                .setVertical("NURTURE_FARM")
                .addAllAttribs(Arrays.asList(
                        Attribs.newBuilder().setKey("0").setValue("p1").build(),
                        Attribs.newBuilder().setKey("1").setValue("p2").build()
                ))
                .build();

        /**/
        AddTemplateRequest testNormalTemplate1 = AddTemplateRequest.newBuilder().
                setChannel(CommunicationChannel.WHATSAPP)
                .setContent("Dear {{farmer_first_name}}, \n Thank you for considering nurture farm to help you with your farming needs and testing call to action for phone number, url both with header and footer and document media test for stage. \n You can also visit our app for checking mandi prices. \n \n Please reach out to our support team for any queries.")
                .setLanguageCode(LanguageCode.EN_US)
                .setName("media_document_test_with_qrb_stage")
                .setTemplateContentType(TemplateContentType.STRING)
                .setOwner("honey@nurture.farm")
                .setVertical("NURTURE_FARM")
                .addAllMetaData(Arrays.asList(
                        Attribs.newBuilder().setKey("interactive_attributes").setValue(interAttrb).build(),
                        Attribs.newBuilder().setKey("media_type").setValue("document").build()
                ))
                .addAllAttribs(Arrays.asList(
                        Attribs.newBuilder().setKey("0").setValue("farmer_first_name").build()
                  //      Attribs.newBuilder().setKey("1").setValue("p1").build(),
                  //      Attribs.newBuilder().setKey("2").setValue("p2").build()
                ))
                .build();

        AddTemplateResponse addTemplateResponse = platformBlockingStub.addTemplate(testNormalTemplate1);
        System.out.println(addTemplateResponse);
    }
    private static void testOptOut() {

        OptOutRequest optOutRequest = OptOutRequest.newBuilder()
                .setRequestHeaders(RequestHeaders.newBuilder().setLanguageCode(LanguageCode.EN_US).build())
                .setMobileNumber("8076385578")
                .setActor(ActorID.newBuilder().setActorType(ActorType.FARMER).setActorId(41348))
//                .setSourceSystem(SourceSystem.RETAILER_APP)
//                .setNameSpace(NameSpace.RETAIL)
                .build();

        platformBlockingStub = CommunicationEnginePlatformGrpc.newBlockingStub(channel);
        OptOutResponse resp = platformBlockingStub.optOutUser(optOutRequest);

        System.out.println(resp);
    }


    class ResponseResult {
        String referenceId;
        String communicationChannel;
        String methodName;

        public ResponseResult(String referenceId, String communicationChannel, String methodName) {
            this.referenceId = referenceId;
            this.communicationChannel = communicationChannel;
            this.methodName = methodName;
        }

        public String getReferenceId() {
            return referenceId;
        }

        public void setReferenceId(String referenceId) {
            this.referenceId = referenceId;
        }

        public String getCommunicationChannel() {
            return communicationChannel;
        }

        public void setCommunicationChannel(String communicationChannel) {
            this.communicationChannel = communicationChannel;
        }

        public String getMethodName() {
            return methodName;
        }

        public void setMethodName(String methodName) {
            this.methodName = methodName;
        }
    }

}
