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

import com.google.inject.Inject;
import com.google.protobuf.Timestamp;
import farm.nurture.communication.engine.cache.LanguageCache;
import farm.nurture.communication.engine.models.MessageAcknowledgement;
import farm.nurture.communication.engine.models.Template;
import farm.nurture.core.contracts.common.ActorID;
import farm.nurture.core.contracts.common.Attribs;
import farm.nurture.core.contracts.common.enums.*;
import farm.nurture.core.contracts.communication.engine.*;
import farm.nurture.infra.util.StringUtils;
import farm.nurture.util.serializer.Serializer;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

@Slf4j
public class ResponseMapper {

    @Inject
    private LanguageCache languageCache;

    public OptInRespone mapToOptInRespone(ResponseStatus status, ResponseStatusCode statusCode, Map<String, String> map) {
        OptInRespone.Builder builder = OptInRespone.newBuilder();
        builder.setStatus(status)
                .setStatusCode(statusCode);

        if (map != null && !map.isEmpty()) {
            for (Map.Entry<String, String> entry : map.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                if (key.equals("id")) {
                    builder.setId(Long.parseLong(value));
                } else {
                    builder.addAttribs(Attribs.newBuilder().setKey(key).setValue(value).build());
                }
            }
        }

        return builder.build();
    }

    public OptOutResponse mapToOptOutRespone(ResponseStatus status, ResponseStatusCode statusCode) {
        OptOutResponse.Builder builder = OptOutResponse.newBuilder();
        builder.setStatus(status)
                .setStatusCode(statusCode);
        return builder.build();
    }


    public AddTemplateResponse mapToAddTemplateResponse(ResponseStatus status, ResponseStatusCode statusCode, Map<String, String> map, List<VendorResponse> vendorResponses) {
        AddTemplateResponse.Builder builder = AddTemplateResponse.newBuilder();
        builder.setStatus(status)
                .setStatusCode(statusCode)
                .addAllVendorsResponse(vendorResponses)
        ;

        if (map != null && !map.isEmpty()) {
            for (Map.Entry<String, String> entry : map.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                if (key.equals("id")) {
                    builder.setId(Long.parseLong(value));
                }
            }
        }

        return builder.build();
    }

    public VendorResponse mapToVendorResponse(String vendorName, ResponseStatus status, ResponseStatusCode statusCode, Map<String, Object> attributeMap) {
        VendorResponse.Builder builder = VendorResponse.newBuilder();
        builder.setStatus(status)
                .setStatusCode(statusCode)
                .setVendorName(vendorName);
        if (attributeMap != null && !attributeMap.isEmpty()) {
            for (Map.Entry<String, Object> entry : attributeMap.entrySet()) {
                String key = entry.getKey();
                String value = (String) entry.getValue();
                if (key.equals("error")) {
                    builder.setErrorMsg(value);
                }
            }
        }
        else
        {
            builder.setErrorMsg("Callback Not Received!");
        }
        return builder.build();
    }

    public ActivationResponse mapToActivationResponse(List<VendorResponse> vendorResponses, boolean activeTemplate) {
        ActivationResponse.Builder builder = ActivationResponse.newBuilder().addAllVendorsResponse(vendorResponses);
        builder.setActiveTemplate(activeTemplate);
        return builder.build();
    }

    public MessageAcknowledgementResponse mapToMessageAcknowledgementResponse(ResponseStatus status, ResponseStatusCode statusCode, List<MessageAcknowledgement> messageAcknowledgements) {

        MessageAcknowledgementResponse.Builder builder = MessageAcknowledgementResponse.newBuilder();
        builder.setStatus(status).setStatusCode(statusCode);

        if (messageAcknowledgements != null) {
            messageAcknowledgements.forEach(messageAck -> builder.addMessageAcknowledgements(parseMessageAcknowledgement(messageAck)));
        }
        return builder.build();
    }

    public GetAllTemplateResponse mapToTemplateResponse(ResponseStatus status, ResponseStatusCode statusCode, List<Template> templateList) {
        GetAllTemplateResponse.Builder builder = GetAllTemplateResponse.newBuilder();
        builder.setStatus(status).setStatusCode(statusCode);
        if (templateList != null) {
            templateList.forEach(template ->
                    {
                        farm.nurture.core.contracts.communication.engine.Template parsedTemplate = parseTemplate(template);
                        if (parsedTemplate != null) builder.addTemplates(parsedTemplate);
                    }
            );
        }
        return builder.build();
    }

    private farm.nurture.core.contracts.communication.engine.Template parseTemplate(Template template) {
        farm.nurture.core.contracts.communication.engine.Template.Builder templateBuilder = farm.nurture.core.contracts.communication.engine.Template.newBuilder();
        log.info("Serving parse template request {}", template);
        if (template.getId() != 0) {
            templateBuilder.setId(template.getId());
        }
        if (StringUtils.isNonEmpty(template.getName())) {
            templateBuilder.setName(template.getName());
        }
        if (StringUtils.isNonEmpty(template.getContent())) {
            templateBuilder.setContent(template.getContent());
        }
        if (StringUtils.isNonEmpty(template.getTitle())) {
            templateBuilder.setTitle(template.getTitle());
        }
        if (StringUtils.isNonEmpty(template.getOwnerEmail())) {
            templateBuilder.setOwnerEmail(template.getOwnerEmail());
        }
        if (StringUtils.isNonEmpty(template.getVertical())) {
            templateBuilder.setVertical(template.getVertical());
        }
        if (template.getLanguageId() != null && template.getLanguageId() != 0) {
            farm.nurture.communication.engine.models.Language language = languageCache.getLanguageById(template.getLanguageId());
            if (language == null) {
                log.error("Language Id is not present in db: {} ", template.getLanguageId());
                return null;
            }
            templateBuilder.setLanguage(Language.valueOf(language.getName().toUpperCase()));
        }
        if (template.getActive() != null) templateBuilder.setActive(template.getActive());
        if (template.getCreatedAt() != null)
            templateBuilder.setCreatedAt(Timestamp.newBuilder().setSeconds(template.getCreatedAt().getTime() / 1000).build());
        if (template.getDeletedAt() != null)
            templateBuilder.setDeletedAt(Timestamp.newBuilder().setSeconds(template.getDeletedAt().getTime() / 1000).build());
        if (template.getUpdatedAt() != null)
            templateBuilder.setUpdatedAt(Timestamp.newBuilder().setSeconds(template.getUpdatedAt().getTime() / 1000).build());
        if (template.getAttributes() != null) {
            log.info("Serving parse template request for list of Attributes {} ", template.getAttributes());
            template.getAttributes().forEach((key, value) -> {
                if (StringUtils.isNonEmpty(key) && StringUtils.isNonEmpty(value)) {
                    templateBuilder.addAttributes(Attribs.newBuilder().setKey(key).setValue(value).build());
                }
            });
        }
        if (template.getMetaData() != null) {
            log.info("Serving parse template request for list of MetaData {} ", template.getMetaData());
            template.getMetaData().forEach((key, value) -> {
                if (StringUtils.isNonEmpty(key) && value != null) {
                    if (key.equalsIgnoreCase("interactive_attributes")) {
                        try {
                            String interactiveAttributes = Serializer.DEFAULT_JSON_SERIALIZER.serialize(value);
                            templateBuilder.addMetaData(Attribs.newBuilder().setKey(key).setValue(interactiveAttributes).build());

                        } catch (Exception exception) {
                            log.error("Interactive attributes are not in json format ");
                        }
                    } else {
                        templateBuilder.addMetaData(Attribs.newBuilder().setKey(key).setValue((String) value).build());
                    }

                }
            });
        }
        return templateBuilder.build();
    }

    private farm.nurture.core.contracts.communication.engine.MessageAcknowledgement parseMessageAcknowledgement(MessageAcknowledgement messageAcknowledgement) {

        //TO-DO: add attributes
        farm.nurture.core.contracts.communication.engine.MessageAcknowledgement.Builder messageAckBuilder = farm.nurture.core.contracts.communication.engine.MessageAcknowledgement.newBuilder();
        ActorID.Builder actorIDBuilder = ActorID.newBuilder();
        if (messageAcknowledgement.getId() != null && messageAcknowledgement.getId() != 0) {
            messageAckBuilder.setId(messageAcknowledgement.getId());
        }
        if (messageAcknowledgement.getActorId() != null && messageAcknowledgement.getActorId() != 0) {
            actorIDBuilder.setActorId(messageAcknowledgement.getActorId());
        }
        if (messageAcknowledgement.getActorType() != null && messageAcknowledgement.getActorType() != ActorType.NO_ACTOR) {
            actorIDBuilder.setActorType(messageAcknowledgement.getActorType());
        }
        messageAckBuilder.setActor(actorIDBuilder.build());
        if (StringUtils.isNonEmpty(messageAcknowledgement.getActorContactId())) {
            messageAckBuilder.setMobileNumber(messageAcknowledgement.getActorContactId());
        }
        if (StringUtils.isNonEmpty(messageAcknowledgement.getCommunicationChannel())) {
            messageAckBuilder.setChannel(CommunicationChannel.valueOf(messageAcknowledgement.getCommunicationChannel()));
        }
        if (StringUtils.isNonEmpty(messageAcknowledgement.getReferenceId())) {
            messageAckBuilder.setReferenceId(messageAcknowledgement.getReferenceId());
        }
        if (StringUtils.isNonEmpty(messageAcknowledgement.getTempateName())) {
            messageAckBuilder.setTemplateName(messageAcknowledgement.getTempateName());
        }
        if (messageAcknowledgement.getLanguageId() != null && messageAcknowledgement.getLanguageId() != 0) {
            messageAckBuilder.setLanguage(Language.valueOf(languageCache.getLanguageById(messageAcknowledgement.getLanguageId()).getName().toUpperCase()));
        }
        if (StringUtils.isNonEmpty(messageAcknowledgement.getMessageContent())) {
            messageAckBuilder.setMessageContent(messageAcknowledgement.getMessageContent());
        }
        if (messageAcknowledgement.getIsUnicode() != null)
            messageAckBuilder.setIsUnicode(messageAcknowledgement.getIsUnicode());
        if (StringUtils.isNonEmpty(messageAcknowledgement.getVendorName())) {
            messageAckBuilder.setVendorName(messageAcknowledgement.getVendorName());
        }
        if (StringUtils.isNonEmpty(messageAcknowledgement.getVendorMessageId())) {
            messageAckBuilder.setVendorMessageId(messageAcknowledgement.getVendorMessageId());
        }
        if (messageAcknowledgement.getState() != null)
            messageAckBuilder.setState(CommunicationState.valueOf(messageAcknowledgement.getState().name()));
        if (messageAcknowledgement.getRetryCount() != null)
            messageAckBuilder.setRetryCount(messageAcknowledgement.getRetryCount());
        if (messageAcknowledgement.getPlaceHolders() != null)
            convertPlaceholders(messageAckBuilder, messageAcknowledgement.getPlaceHolders());
        if (messageAcknowledgement.getAttributes() != null)
            convertAttributes(messageAckBuilder, messageAcknowledgement.getAttributes());
        if (messageAcknowledgement.getVendorDeliveryTime() != null)
            messageAckBuilder.setVendorDeliveryTime(Timestamp.newBuilder().setSeconds(messageAcknowledgement.getVendorDeliveryTime().getTime() / 1000).build());
        if (messageAcknowledgement.getActorDeliveryTime() != null)
            messageAckBuilder.setActorDeliveryTime(Timestamp.newBuilder().setSeconds(messageAcknowledgement.getActorDeliveryTime().getTime() / 1000).build());
        if (messageAcknowledgement.getCreatedAt() != null)
            messageAckBuilder.setCreatedAt(Timestamp.newBuilder().setSeconds(messageAcknowledgement.getCreatedAt().getTime() / 1000).build());
        if (messageAcknowledgement.getUpdatedAt() != null)
            messageAckBuilder.setUpdatedAt(Timestamp.newBuilder().setSeconds(messageAcknowledgement.getUpdatedAt().getTime() / 1000).build());
        return messageAckBuilder.build();
    }

    private void convertPlaceholders(final farm.nurture.core.contracts.communication.engine.MessageAcknowledgement.Builder messageAckBuilder, final Map<String, Object> placeHolders) {

        placeHolders.forEach((key, value) -> {
            String valueString = value.toString();
            if (StringUtils.isNonEmpty(valueString)) {
                messageAckBuilder.addPlaceholders(Placeholder.newBuilder().setKey(key).setValue(valueString).build());
            }
        });
    }

    private void convertAttributes(final farm.nurture.core.contracts.communication.engine.MessageAcknowledgement.Builder messageAckBuilder, final Map<String, Object> attributes) {

        attributes.forEach((key, value) -> {
            String valueString = "";
            if (value != null) valueString = value.toString();
            if (StringUtils.isNonEmpty(key) && StringUtils.isNonEmpty(valueString)) {
                messageAckBuilder.addAttributes(Attribs.newBuilder().setKey(key).setValue(valueString).build());
            }
        });
    }
}
