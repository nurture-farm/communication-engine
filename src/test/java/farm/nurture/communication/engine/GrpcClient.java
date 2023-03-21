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

import farm.nurture.core.contracts.common.enums.CommunicationChannel;
import farm.nurture.core.contracts.common.enums.LanguageCode;
import farm.nurture.core.contracts.communication.engine.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;

@Slf4j
public class GrpcClient {

    private static final String LOCAL_URL = "127.0.0.1:8010";
    private static final String DEV_URL = "internal-a2d376e6948514a73a514e3584160823-409853754.ap-south-1.elb.amazonaws.com:80";
    private static final String STAGE_URL = "internal-a49872e2b408d43d28ce6000e575e84b-437549400.ap-south-1.elb.amazonaws.com:80";
    private static final String PROD_URL = "internal-a85f3a2f550f84f578459537c8ae3304-113299092.ap-south-1.elb.amazonaws.com:80";

    private static ManagedChannel channel;
    private static CommunicationEngineGrpc.CommunicationEngineBlockingStub blockingStub;
    private static CommunicationEnginePlatformGrpc.CommunicationEnginePlatformBlockingStub platformBlockingStub;
    static {
        channel = ManagedChannelBuilder.forTarget(LOCAL_URL).usePlaintext().build();
        blockingStub = CommunicationEngineGrpc.newBlockingStub(channel);
        platformBlockingStub = CommunicationEnginePlatformGrpc.newBlockingStub(channel);
    }

    /*
    Placeholder: []*commEngine.Placeholder{{
			}, {
				Key:   "name",
				Value: "LMN DFG",
			}, {
				Key:   "number",
				Value: "6463244344",
			}, {
				Key:   "service_name",
				Value: "spraying",
			}},
     */

    public static void main(String[] args) throws Exception {

        getAllTemplate();
//        List<String> templates = Arrays.asList("farmer_registration", "operator_booking_assignment", "farmer_service_completed", "operator_nextday_booking_reminder",
//                "farmer_booking_creation", "farmer_booking_reject", "farmer_service_scheduled", "farmer_booking_cancelled_by_system", "operator_service_cancelled");
//        List<String> templates = Arrays.asList("farmer_service_confirmed", "farmer_service_scheduled_v1", "farmer_sts_sample_collection_completed", "farmer_sts_farm_scan_completed");
//        List<String> templates = Arrays.asList("farmer_booking_reject");
//
//        List<LanguageCode> languageCodes = Arrays.asList(LanguageCode.EN_US, LanguageCode.HI_IN, LanguageCode.GU, LanguageCode.PA, LanguageCode.KN, LanguageCode.TA,
//                LanguageCode.TE, LanguageCode.BN, LanguageCode.MR, LanguageCode.ML);
//
////        List<LanguageCode> languageCodes = Arrays.asList(LanguageCode.ML);
//
//        List<Placeholder> placeholders = Arrays.asList(
//                Placeholder.newBuilder().setKey("app_link").setValue("https://tinyurl.com/ParaliEN").build(),
//                Placeholder.newBuilder().setKey("invoice_link").setValue("https://tinyurl.com/ParaliHI").build(),
//                Placeholder.newBuilder().setKey("token_id").setValue("123").build(),
//                Placeholder.newBuilder().setKey("booking_id").setValue("100").build(),
//                Placeholder.newBuilder().setKey("service_id").setValue("99").build(),
//                Placeholder.newBuilder().setKey("farmer_name").setValue("Test farmer").build(),
//                Placeholder.newBuilder().setKey("farmer_mobile_number").setValue("8643534645").build(),
//                Placeholder.newBuilder().setKey("name").setValue("Test operator").build(),
//                Placeholder.newBuilder().setKey("number").setValue("6463244344").build(),
//                Placeholder.newBuilder().setKey("service_name").setValue("spraying").build(),
//                Placeholder.newBuilder().setKey("link").setValue("https://tinyurl.com/ParaliEN").build()
//        );
//
//        Placeholder contentMetaData2 = Placeholder.newBuilder().setKey("attachment_fileurl_2").setValue("https://afs-static-content.s3.ap-south-1.amazonaws.com/Operators_Controller_Issues_Guide_Punjabi.pdf").build();
//        Placeholder contentMetaData3 = Placeholder.newBuilder().setKey("attachment_filename_2").setValue("punjabi.pdf").build();
//        Placeholder contentMetaData6 = Placeholder.newBuilder().setKey("attachment_fileurl_1").setValue("https://afs-static-content.s3.ap-south-1.amazonaws.com/Gujrati_Operators_Controller_Issues_Guide.pdf").build();
//        Placeholder contentMetaData7 = Placeholder.newBuilder().setKey("attachment_filename_1").setValue("gujarati.pdf").build();
//
//        ActorDetails actorDetails = ActorDetails.newBuilder()
////                .setAppId(AppID.NF_FARMER)
////                .setAppType(AppType.ANDROID)
////                .setFcmToken("dfP7q7McS_mvL5tV42ygZI:APA91bELR0ScSnYZ8A7JPzY1FZPvU2MpZ5d_z1zbAcHHhRXI3aoaeq9wxnnJHnMgsMxyUUCDZyas2LA43crdbClAHH8ne6RARKZ3M2NndkG0Bfy-1DNSnH2obx4bc2TAK6giqINFT3LD")
////                .setMobileNumber("9453849441")
//                .setEmailId("kishan.ngm@gmail.com")
//                .setLanguageCode(LanguageCode.EN_US).build();
//
////        for(String template : templates) {
////            for(LanguageCode languageCode : languageCodes) {
////                ActorDetails actorDetails = ActorDetails.newBuilder().setMobileNumber("9591373075").setLanguageCode(languageCode).build();
//                CommunicationEvent event = CommunicationEvent.newBuilder()
//                        .addChannel(CommunicationChannel.EMAIL)
////                        .addAllPlaceholder(placeholders)
//                        .setReceiverActorDetails(actorDetails)
//                        .setTemplateName("farmer_booking_reject")
//                        .addContentMetadata(contentMetaData2)
//                .addContentMetadata(contentMetaData3)
//                .addContentMetadata(contentMetaData6)
//                .addContentMetadata(contentMetaData7)
//                        .build();
//                try {
//                    CommunicationResponse response = blockingStub.sendCommunication(event);
//                    log.info("response : {}", response);
//                } catch (Exception e) {
//                    log.error("Error in sending communication event : {}", event, e);
//                }
//            }
//        }
    }

    private static void getAllTemplate() {

        GetAllTemplateRequest request = GetAllTemplateRequest.newBuilder()
                .setLimit(5)
                .setOffset(0)
                .build();
        log.info("Sending getAllTemplateRequest : {}", request);
        GetAllTemplateResponse response =  platformBlockingStub.getAllTemplate(request);
        log.info("response : {}", response);
    }
}
