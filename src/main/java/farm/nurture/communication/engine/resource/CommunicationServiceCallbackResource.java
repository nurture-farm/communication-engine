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

package farm.nurture.communication.engine.resource;

import com.google.inject.Inject;
import farm.nurture.communication.engine.dto.*;
import farm.nurture.communication.engine.dto.CommunicationServiceCallbackRequest.Status;
import farm.nurture.communication.engine.metric.MetricGroupNames;
import farm.nurture.communication.engine.metric.MetricTracker;
import farm.nurture.communication.engine.metric.Metrics;
import farm.nurture.communication.engine.service.CommunicationServiceCallbackService;
import farm.nurture.communication.engine.vendor.VendorType;
import farm.nurture.core.contracts.common.enums.CommunicationChannel;
import farm.nurture.infra.util.StringUtils;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Path("/platform/communication-engine/v1")
public class CommunicationServiceCallbackResource {

    @Inject
    private CommunicationServiceCallbackService communicationServiceCallbackService;

    @Path("/sms-callback")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMissCall(SmsCallbackRequest smsCallbackRequest) {

        log.info("Sms Callback Request: {}", smsCallbackRequest);
        if (smsCallbackRequest == null || smsCallbackRequest.getResponse() == null || smsCallbackRequest.getResponse().size() == 0) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Bad Request").build();
        }

        Status status;
        Response response;
        MetricTracker tracker = new MetricTracker(MetricGroupNames.NF_CE_SMS_SERVICE_CALLBACK_STATUS, "smsCallbackRequest");
        List<CommunicationServiceCallbackRequest> callbackRequestArrayList = new ArrayList<>();
        boolean success = false;
        try {
        for(SmsCallbackRequest.ResponseDetails responseDetails : smsCallbackRequest.getResponse()) {
            log.info("received values for all the parameters are externalId : {} , deliveredTS : {} , status : {} , cause : {} , phoneNo : {} , errCode : {} and noOfFrags : {}",
                    responseDetails.getExternalId(), responseDetails.getEventTs(),
                    responseDetails.getEventType(), responseDetails.getCause(),
                    responseDetails.getDestAddr(),
                    responseDetails.getErrorCode(), responseDetails.getNoOfFrags());

            if (responseDetails.getExternalId() == null || responseDetails.getEventType() == null ||
                    responseDetails.getEventTs()== null || responseDetails.getCause() == null || responseDetails.getErrorCode() == null) {
                log.error("given externalId : {} or status : {} or  deliveredTS : {} or cause : {} or errorCode : {} is missing from callback",
                        responseDetails.getExternalId(), responseDetails.getEventType(),
                        responseDetails.getEventTs(), responseDetails.getCause(), responseDetails.getErrorCode());
                Metrics.getInstance().onIncrement(MetricGroupNames.NF_CE_SMS_SERVICE_CALLBACK_STATUS, "BAD_REQUEST");
                return Response.status(Response.Status.BAD_REQUEST).entity("Bad Request").build();
            }

            switch (responseDetails.getEventType()) {
                case "DELIVERED":
                    status = Status.SUCCESS;
                    break;
                case "SENT":
                    status = Status.SENT;
                    break;
                default:
                    log.info("Final status : {} of the message, marked as Failure", responseDetails.getEventType());
                    status = Status.FAIL;
            }

            CommunicationServiceCallbackRequest request = new CommunicationServiceCallbackRequest(responseDetails.getExternalId(),
                    new Timestamp(responseDetails.getEventTs()), status, responseDetails.getCause(), responseDetails.getDestAddr(),
                    responseDetails.getErrorCode(), responseDetails.getNoOfFrags(), VendorType.GUPSHUP.name());
            boolean isUpdated = communicationServiceCallbackService.
                    updateMessageAcknowledgements(CommunicationChannel.SMS.name(), request,
                            MetricGroupNames.NF_CE_SMS_SERVICE_CALLBACK_STATUS);
            if (!isUpdated) {
                log.error("Update failed in MessageAcknowledgements. Vendor message Id : {}", responseDetails.getExternalId());
                //response = Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Something went wrong").build();
                request = null;
            } else {
                log.info("UpdateMessageAcknowledgements is successful for Vendor message Id : {}", responseDetails.getExternalId());
               // response = Response.status(Response.Status.OK).entity(request).build();
                success = true;
            }
            callbackRequestArrayList.add(request);
        }
        } catch (Exception e) {
            log.error("error in CommunicationServiceCallbackService", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Something went wrong").build();
        } finally {
            tracker.stop(success);
        }
        return Response.ok(callbackRequestArrayList.toString()).build();
    }

    @Path("/whatsapp-incoming-callback")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response sendWhatsappIncomingCallback(GupshupWhatsAppIncomingCallbackRequest whatsAppIncomingCallbackRequest){
        log.info("Gupshup Whatsapp Incoming Callback request: {}", whatsAppIncomingCallbackRequest);

        if (whatsAppIncomingCallbackRequest == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Bad Request").build();
        }
        boolean success = false;
        MetricTracker tracker = new MetricTracker(MetricGroupNames.NF_CE_WHATSAPP_SERVICE_CALLBACK_STATUS, "whatsapp_incoming_callback_request");
        try {
            success = communicationServiceCallbackService.insertMessageAcknowledgments(whatsAppIncomingCallbackRequest);
        } catch (Exception e) {
            log.error("Insert failed in MessageAcknowledgements for whatsAppIncomingCallbackRequest {} ", whatsAppIncomingCallbackRequest.toString(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Something went wrong").build();
        } finally {
            tracker.stop(success);
        }
        return Response.ok(whatsAppIncomingCallbackRequest.toString()).build();
    }

    @Path("/whatsapp-callback")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response sendWhatsappCallBack(List<WhatsAppCallbackRequest> whatsAppCallbackRequestList) {
        log.info("Gupshup Whatsapp Callback request: {}", whatsAppCallbackRequestList);

        if (whatsAppCallbackRequestList == null || whatsAppCallbackRequestList.size() == 0) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Bad Request").build();
        }
        boolean success = false;

        List<CommunicationServiceCallbackRequest> callbackRequestArrayList = new ArrayList<>();
        MetricTracker tracker =  new MetricTracker(MetricGroupNames.NF_CE_WHATSAPP_SERVICE_CALLBACK_STATUS, "whatsapp_callback_request");
        try {
            for (WhatsAppCallbackRequest whatsAppCallbackRequest : whatsAppCallbackRequestList) {
                Status status = getStatus(whatsAppCallbackRequest.getEventType());
                CommunicationServiceCallbackRequest communicationServiceCallbackRequest =
                        new CommunicationServiceCallbackRequest(whatsAppCallbackRequest.getExternalId(),
                                new Timestamp(whatsAppCallbackRequest.getEventTs()), status,
                                whatsAppCallbackRequest.getCause().name(),
                                whatsAppCallbackRequest.getDestAddr(),
                                whatsAppCallbackRequest.getErrorCode(),
                                0L, VendorType.GUPSHUP.name()
                        );

                boolean isUpdated = communicationServiceCallbackService.updateMessageAcknowledgements(
                        CommunicationChannel.WHATSAPP.name(), communicationServiceCallbackRequest,
                        MetricGroupNames.NF_CE_WHATSAPP_SERVICE_CALLBACK_STATUS);
                if (!isUpdated) {
                    log.error("Update failed in MessageAcknowledgements. Vendor message Id : {}", whatsAppCallbackRequest.getExternalId());
                     communicationServiceCallbackRequest = null;
                } else {
                    log.info("UpdateMessageAcknowledgements is successful for Vendor message Id : {}",  whatsAppCallbackRequest.getExternalId());
                    //response = communicationServiceCallbackRequest; Response.status(Response.Status.OK).entity(communicationServiceCallbackRequest).build();
                    success = true;
                }
                callbackRequestArrayList.add(communicationServiceCallbackRequest);
            }
        } catch (Exception e) {
            log.error("error in CommunicationServiceCallbackService", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Something went wrong").build();
        } finally {
            tracker.stop(success);
        }
        return Response.ok(callbackRequestArrayList.toString()).build();
    }

    private Status getStatus(WhatsAppCallbackRequest.EventType eventType) {
        Status status;
        switch (eventType) {
            case READ:
                status = Status.VIEW;
                break;
            case FAILED:
                status = Status.FAIL;
                break;
            case DELIVERED:
                status = Status.SUCCESS;
                break;
            case SENT:
                status = Status.SENT;
                break;
            default:
                status = Status.FAIL;
                break;
        }
        return status;
    }

    @Path("/sms-karix-callback")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSMSCallback(@QueryParam("sid") String messageUniqueId, @QueryParam("dest") String receiverPhoneNumber,
                                   @QueryParam("stime") String messageSourceTime, @QueryParam("dtime") String messageDeliveryTime,
                                   @QueryParam("status") String errorCode, @QueryParam("reason") String cause
                                   ) {
        Response response;
        log.info("received values for all the parameters are sid : {} , dest : {} , stime : {} , dtime : {} , status : {} , reason : {} ",
                messageUniqueId, receiverPhoneNumber, messageSourceTime, messageDeliveryTime, errorCode, cause);

        if (messageUniqueId == null || receiverPhoneNumber == null || messageSourceTime == null || errorCode == null || cause == null) {

            log.error("given sid : {} or dest : {} or  stime : {} or dtime : {} or status : {} or reason : {} is missing from callback",
                    messageUniqueId, receiverPhoneNumber, messageSourceTime, messageDeliveryTime, errorCode, cause);

            Metrics.getInstance().onIncrement(MetricGroupNames.NF_CE_SMS_SERVICE_CALLBACK_STATUS, "BAD_REQUEST");
            return Response.status(Response.Status.BAD_REQUEST).entity("Bad Request").build();
        }
        Timestamp destTime = null;
        String destTimeStr = null;
        try {
            if(StringUtils.isEmpty(messageDeliveryTime)){
                destTime = new Timestamp(System.currentTimeMillis());
            }else{
                destTimeStr = URLDecoder.decode(messageDeliveryTime, StandardCharsets.UTF_8.name());
                destTime = Timestamp.valueOf(destTimeStr);
            }
        } catch (Exception exp) {
                log.error("dest time: {} or destination time url decoded: {}, exception: {}",
                        destTime, destTimeStr, exp);
            destTime = new Timestamp(System.currentTimeMillis());
        }
        boolean success = false;
        MetricTracker tracker = new MetricTracker(MetricGroupNames.NF_CE_SMS_SERVICE_CALLBACK_STATUS, "karixSmsRequest");

        try {
            Status status = Status.FAIL;
            if(cause.equalsIgnoreCase(KarixReasonCode.DELIVRD.name())){
                status = Status.SUCCESS;
            }
            CommunicationServiceCallbackRequest request = new CommunicationServiceCallbackRequest(messageUniqueId,
                    destTime, status, cause, receiverPhoneNumber, errorCode, 0L,
                    VendorType.KARIX.name()
                    );
            boolean isUpdated = communicationServiceCallbackService.
                    updateMessageAcknowledgements(CommunicationChannel.SMS.name(), request,
                            MetricGroupNames.NF_CE_SMS_SERVICE_CALLBACK_STATUS);
            if (!isUpdated) {
                log.error("Update failed in MessageAcknowledgements. Vendor message Id : {}", messageUniqueId);
                response = Response.status(Response.Status.OK).entity(request).build();
            } else {
                log.info("UpdateMessageAcknowledgements is successful for Vendor message Id : {}", messageUniqueId);
                response = Response.status(Response.Status.OK).entity(request).build();
                success = true;
            }
        } catch (Exception e) {
            log.error("error in CommunicationServiceCallbackService", e);
            response = Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Something went wrong").build();
        } finally {
            tracker.stop(success);
        }
        return response;
    }


    @Path("/whatsapp-karix-callback")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Response sendWhatsappCallBack(KarixWhatsAppCallbackRequest karixWhatsAppCallbackRequest){
        Response response;
        log.info("Karix Whatsapp Callback request: {}", karixWhatsAppCallbackRequest);
        MetricTracker tracker =  new MetricTracker(MetricGroupNames.NF_CE_WHATSAPP_SERVICE_CALLBACK_STATUS, "whatsapp_callback_request");
        boolean success = false;
        try{
            CommunicationServiceCallbackRequest communicationServiceCallbackRequest =
                    new CommunicationServiceCallbackRequest(karixWhatsAppCallbackRequest.getEvents().getMid(),
                            new Timestamp(karixWhatsAppCallbackRequest.getEvents().getTimestamp()),
                            getCallbackStatus(karixWhatsAppCallbackRequest.
                            getNotificationAttributes().getStatus()),
                            karixWhatsAppCallbackRequest.getNotificationAttributes().getReason(),
                            karixWhatsAppCallbackRequest.getRecipient().getTo(),
                           karixWhatsAppCallbackRequest.getNotificationAttributes().getCode(),
                            0L, VendorType.KARIX.name()
                    );

            boolean isUpdated = communicationServiceCallbackService.updateMessageAcknowledgements(
                    CommunicationChannel.WHATSAPP.name(), communicationServiceCallbackRequest,
                    MetricGroupNames.NF_CE_WHATSAPP_SERVICE_CALLBACK_STATUS);
            if (!isUpdated) {
                log.error("Update failed in MessageAcknowledgements. Vendor message Id : {}", karixWhatsAppCallbackRequest.getEvents().getMid());
                response = Response.ok(karixWhatsAppCallbackRequest).build();
            } else {
                log.info("UpdateMessageAcknowledgements is successful for Vendor message Id : {}", karixWhatsAppCallbackRequest.getEvents().getMid());
                response = Response.ok(karixWhatsAppCallbackRequest).build();
                success = true;
            }

        }catch (Exception e) {
            log.error("error in CommunicationServiceCallbackService", e);
            response = Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Something went wrong").build();
        } finally {
            tracker.stop(success);
        }
        return response;
    }

    private Status getCallbackStatus(String statusStr) {
        Status status = Status.FAIL;
        if (statusStr != null) {
            switch (statusStr.toLowerCase()) {
                case "read":
                    status = Status.VIEW;
                    break;
                case "dropped":
                    status = Status.FAIL;
                    break;
                case "delivered":
                    status = Status.SUCCESS;
                    break;
                case "sent":
                    status = Status.SENT;
                    break;
                default:
                    status = Status.FAIL;
                    break;
            }
        }
        return status;
    }
    }
