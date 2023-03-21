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
import farm.nurture.communication.engine.dto.MissCallRequest;
import farm.nurture.communication.engine.service.MissCallService;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.*;
import javax.ws.rs.core.*;

@Slf4j
@Path("/platform/communication-engine/v1")
public class MissCallResource {
    @Inject
    private MissCallService missCallService;

    public enum Status{
        OPT_IN, OPT_OUT
    }

    @Path("/missCallDetails")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMissCall(@QueryParam("msisdn") String senderMobile, @QueryParam("extension") String missCallNumber,
                                @QueryParam("causeId") String transactionId, @QueryParam("hasUserHungUp") Boolean hasUserHungUp,
                                @QueryParam("timestamp") Long timeOfMissCall, @QueryParam("location") String location) {
        log.info("MissCallResource started using senderMobile : {}, missCallNumber : {}, transactionId : {}, hasUserHungUp : {}," +
                "timeOfMissCall : {}, location : {}", senderMobile, missCallNumber, transactionId, hasUserHungUp, timeOfMissCall, location);

        Status status;
        Response response = null;

        if (senderMobile == null || missCallNumber == null) {
            log.error("given msisdn : {} or extension : {} is null ", senderMobile, missCallNumber);
            return Response.status(Response.Status.BAD_REQUEST).entity("msisdn and extension parameter are mandatory").build();
        }

        try {
            switch (missCallNumber) {
                case "9029059263":
                    status = Status.OPT_IN;
                    break;
                case "9029065989":
                    status = Status.OPT_OUT;
                    break;
                default:
                    log.error("given extension : {} is not among numbers for out-in or out-opt ", missCallNumber);
                    return Response.status(Response.Status.BAD_REQUEST).entity("Bad Request").build();
            }

            //if msisdn is coming in the form of 91XXXXXXXXXX so removing the country code
            if (senderMobile.length() == 12 && senderMobile.startsWith("91")) {
                log.info("Removing the country code form msisdn : {}", senderMobile);
                senderMobile = senderMobile.substring(2);
            }

            log.info("MissCall number in MissCallResource is : {}", missCallNumber);

            MissCallRequest request = new MissCallRequest(senderMobile, missCallNumber, transactionId, hasUserHungUp,
                    timeOfMissCall, location);
            log.info("MissCallRequest value in MissCallResource is : {}", request);

            boolean updateStatus = missCallService.updateWhatsAppStatus(senderMobile, status);
            if (updateStatus) {
                log.info("Update for WhatsAppStatus successful");
                response = Response.status(Response.Status.OK).entity(request).build();
            } else {
                log.error("some error while updating WhatsAppStatus");
                response = Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Something went wrong").build();
            }
        } catch (Exception e) {
            log.error("error in MissCallService", e);
            response = Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Something went wrong").build();
        }
        return response;
    }
}

