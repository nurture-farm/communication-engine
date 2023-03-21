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

package farm.nurture.communication.engine.helper;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.MustacheFactory;
import com.google.protobuf.Timestamp;
import farm.nurture.communication.engine.Constants;
import farm.nurture.communication.engine.dto.WhatsAppSendRequest;
import farm.nurture.communication.engine.metric.MetricGroupNames;
import farm.nurture.communication.engine.metric.Metrics;
import farm.nurture.core.contracts.common.Attribs;
import farm.nurture.core.contracts.common.enums.*;
import farm.nurture.core.contracts.communication.engine.*;
import farm.nurture.infra.util.StringUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.StringReader;
import java.util.List;

import static farm.nurture.communication.engine.helper.Utils.validateMobileNumber;

@Slf4j
public class RequestValidator {

    private static final MustacheFactory mustacheFactory = new DefaultMustacheFactory();
    private final Metrics metrics = Metrics.getInstance();
    private final Long weekSeconds = 604800L;

    public MessageAcknowledgementResponse validate(MessageAcknowledgementRequest request) {

        boolean valid = false;
        Timestamp nullTimeStamp = Timestamp.newBuilder().build();
        Timestamp startTime = request.getStartTime();
        Timestamp endTime = request.getEndTime();
        if (StringUtils.isNonEmpty(request.getMobileNumber()) || StringUtils.isNonEmpty(request.getReferenceId())) {
            valid = true;
        } else if (!startTime.equals(nullTimeStamp) && !endTime.equals(nullTimeStamp) && endTime.getSeconds() - startTime.getSeconds() > 0
                && endTime.getSeconds() - startTime.getSeconds() <= weekSeconds) {
            valid = true;
        }

        if (!valid) {
            metrics.onIncrement(MetricGroupNames.NF_CE, "invalid_message_acknowledgement_request");
            log.error("Communication channel or templateName is not passed. Invalid MessageAcknowledgementRequest : {}", request);
            return MessageAcknowledgementResponse.newBuilder()
                    .setStatus(ResponseStatus.ERROR)
                    .setStatusCode(ResponseStatusCode.BAD_REQUEST)
                    .build();
        }
        return null;
    }

    public OptInRespone validate(OptInRequest request) {

        boolean valid = true;
        if (StringUtils.isEmpty(request.getMobileNumber()) || request.getRequestHeaders().getLanguageCode() == LanguageCode.NO_LANGUAGE_CODE) {
            valid = false;
        }

        if (!valid) {
            metrics.onIncrement(MetricGroupNames.NF_CE, "invalid_whatsapp_opt_in_request");
            log.error("Mobile number is not passed. Invalid OptInRequest : {}", request);
            return OptInRespone.newBuilder()
                    .setStatus(ResponseStatus.ERROR)
                    .setStatusCode(ResponseStatusCode.BAD_REQUEST)
                    .build();
        }
        return null;
    }

    public OptOutResponse validate(OptOutRequest request) {

        boolean valid = true;
        if (StringUtils.isEmpty(request.getMobileNumber())) {
            valid = false;
        }

        if (!valid) {
            metrics.onIncrement(MetricGroupNames.NF_CE, "invalid_whatsapp_opt_out_request");
            log.error("Mobile number is not passed. Invalid OptOutRequest : {}", request);
            return OptOutResponse.newBuilder()
                    .setStatus(ResponseStatus.ERROR)
                    .setStatusCode(ResponseStatusCode.BAD_REQUEST)
                    .build();
        }
        return null;
    }

    public AddTemplateResponse validate(AddTemplateRequest request) {

        boolean valid = true;
        if (StringUtils.isEmpty(request.getName()) || request.getLanguageCode() == LanguageCode.NO_LANGUAGE_CODE
                || request.getChannel() == CommunicationChannel.NO_CHANNEL
                || request.getTemplateContentType() == TemplateContentType.NO_TEMPLATE_CONTENT_TYPE ||
                StringUtils.isEmpty(request.getContent()) || StringUtils.isEmpty(request.getOwner())
                || StringUtils.isEmpty(request.getVertical())
        ) {
            valid = false;
        }
        try {
            mustacheFactory.compile(new StringReader(request.getContent()), request.getName() + "_" + request.getLanguageCodeValue());
        } catch (Exception exp) {
            valid = false;
            log.error("Invalid Template Content for request {} ", exp, request);
        }
        if (request.getChannel() == CommunicationChannel.SMS) {
            boolean dltIDpresent = false;
            List<Attribs> attributes = request.getAttribsList();
            for (Attribs attribs : attributes) {
                if (attribs.getKey().equals(Constants.DLT_TEMPLATE_ID) && StringUtils.isNonEmpty(attribs.getValue())) {
                    dltIDpresent = true;
                }
            }
            log.error("DLT Template ID is not present in SMS add template request: {}", request);
            if (!dltIDpresent) valid = false;
        }

        if (!valid) {
            metrics.onIncrement(MetricGroupNames.NF_CE, "invalid_add_template_request");
            log.error("Invalid AddTemplate request : {}", request);
            return AddTemplateResponse.newBuilder()
                    .setStatus(ResponseStatus.ERROR)
                    .setStatusCode(ResponseStatusCode.BAD_REQUEST)
                    .build();
        }
        return null;
    }

    public AddTemplateResponse validate(TemplateUpdateRequest templateUpdateRequest) {
        boolean valid = false;
        if (StringUtils.isNonEmpty(templateUpdateRequest.getName())
                && templateUpdateRequest.getLanguageCode() != null
                &&
                (StringUtils.isNonEmpty(templateUpdateRequest.getContent()) ||
                        StringUtils.isNonEmpty(templateUpdateRequest.getVertical()) ||
                        StringUtils.isNonEmpty(templateUpdateRequest.getOwner()) ||
                        StringUtils.isNonEmpty(templateUpdateRequest.getTitle()) ||
                        templateUpdateRequest.getAttribsCount() != 0 ||
                        templateUpdateRequest.getMetaDataCount() != 0
                )
        ) {
            valid = true;
        }
        if (StringUtils.isNonEmpty(templateUpdateRequest.getContent())) {
            try {
                mustacheFactory.compile(new StringReader(templateUpdateRequest.getContent()), templateUpdateRequest.getName() + "_" + templateUpdateRequest.getLanguageCodeValue());
            } catch (Exception exp) {
                valid = false;
                log.error("Invalid Template Content for request {} ", exp, templateUpdateRequest);
            }
        }
        if (!valid) {
            metrics.onIncrement(MetricGroupNames.NF_CE, "invalid_update_template_request");
            log.error("Invalid Update Template request : {}", templateUpdateRequest);
            return AddTemplateResponse.newBuilder()
                    .setStatus(ResponseStatus.ERROR)
                    .setStatusCode(ResponseStatusCode.BAD_REQUEST)
                    .build();
        }
        return null;
    }

    public ActivationResponse validate(ActivationRequest activationRequest) {
        if (StringUtils.isEmpty(activationRequest.getTemplateName()) || activationRequest.getLanguageCode() == LanguageCode.NO_LANGUAGE_CODE ||
                !validateMobileNumber(activationRequest.getMobileNumber())) {
            return ActivationResponse.newBuilder().addVendorsResponse(VendorResponse.newBuilder().setStatusCode(ResponseStatusCode.BAD_REQUEST)
                    .setStatus(ResponseStatus.ERROR)).build();
        }
        return null;
    }

    public boolean validate(WhatsAppSendRequest whatsAppSendRequest) {
        return whatsAppSendRequest != null && StringUtils.isNonEmpty(whatsAppSendRequest.getMobileNumber())
                && validateMobileNumber(whatsAppSendRequest.getMobileNumber())
                && StringUtils.isNonEmpty(whatsAppSendRequest.getLanguageCode())
                && StringUtils.isNonEmpty(whatsAppSendRequest.getTemplateName());
    }
}
//    public BulkCommunicationResponse validate(BulkCommunicationEvent events) {
//
//        boolean valid = true;
//        for(CommunicationEvent event: events.getCommunicationEventsList()) {
//
//            if (event.getReceiverActorDetails() == null || StringUtils.isEmpty(event.getReceiverActorDetails().getMobileNumber())
//                    || event.getReceiverActorDetails().getLanguageCode() == LanguageCode.NO_LANGUAGE_CODE || StringUtils.isEmpty(event.getTemplateName())
//                    || StringUtils.isEmpty(event.getReferenceId())) {
//                valid = false;
//                log.error("ReceiverActorDetails or mobileNumber or languageCode or tamplateName or referenceId " +
//                        "in receiver actor details or message content in not passed. Invalid SendWhatsappMessage event : {}", event);
//                break;
//            }
//        }
//
//        if(!valid) {
//            metrics.onIncrement(MetricGroupNames.NF_CE, "invalid_send_whatsapp_message_request");
//            return BulkCommunicationResponse.newBuilder()
//                    .setStatus(ResponseStatus.ERROR)
//              