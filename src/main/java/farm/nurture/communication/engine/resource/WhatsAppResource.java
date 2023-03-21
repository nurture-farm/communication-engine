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
import farm.nurture.communication.engine.cache.LanguageCache;
import farm.nurture.communication.engine.cache.TemplateCacheValue;
import farm.nurture.communication.engine.dto.WhatsAppSendRequest;
import farm.nurture.communication.engine.event.DerivedCommunicationEvent;
import farm.nurture.communication.engine.helper.RequestValidator;
import farm.nurture.communication.engine.helper.TemplateHelper;
import farm.nurture.communication.engine.models.Language;
import farm.nurture.communication.engine.service.WhatsappService;
import farm.nurture.communication.engine.utils.VendorLoadBalancer;
import farm.nurture.communication.engine.vendor.VendorType;
import farm.nurture.core.contracts.common.enums.CommunicationChannel;
import farm.nurture.core.contracts.common.enums.CommunicationVendor;
import farm.nurture.core.contracts.common.enums.LanguageCode;
import farm.nurture.core.contracts.common.enums.MediaAccessType;
import farm.nurture.core.contracts.common.enums.ActorType;
import farm.nurture.core.contracts.communication.engine.ActorDetails;
import farm.nurture.core.contracts.communication.engine.CommunicationEvent;
import farm.nurture.core.contracts.communication.engine.Media;
import farm.nurture.core.contracts.communication.engine.Placeholder;
import farm.nurture.infra.util.StringUtils;
import farm.nurture.kafka.Event;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static farm.nurture.communication.engine.Constants.CLEVER_TAP_CAMPAIGN_PREFIX;

@Slf4j
@Path("/platform/communication-engine/v1")
public class WhatsAppResource {

    @Inject
    WhatsappService whatsappService;

    @Inject
    private LanguageCache languageCache;


    @Inject
    private TemplateHelper templateHelper;


    @Inject
    private RequestValidator requestValidator;

    @Inject
    private VendorLoadBalancer vendorLoadBalancer;

    private static final String CLIENT_ID = "CLEVER_TAP";

    @Path("/send-whatsapp")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response sendWhatsApp(WhatsAppSendRequest whatsAppSendRequest) {
        log.info("Serving  WhatsApp Send Request : {}", whatsAppSendRequest);
        Response response;
        if(!requestValidator.validate(whatsAppSendRequest)){
            log.error("Invalid WhatAapp Send Request {} ", whatsAppSendRequest);
            return Response.status(Response.Status.OK).entity("languageId, mobileNumber and templateName parameter are mandatory").build();
        }
        try {
            whatsAppSendRequest.setMobileNumber(
                    whatsAppSendRequest.getMobileNumber().substring(whatsAppSendRequest.getMobileNumber().length()-10));
            final String referenceId = UUID.randomUUID().toString();
            DerivedCommunicationEvent derivedEvent = getDerivedEvent(referenceId, whatsAppSendRequest);
            log.info("Derived Event: {} for sending whatsApp communication request: {}", derivedEvent, whatsAppSendRequest);
            whatsappService.sendMessage(derivedEvent);
            response = Response.status(Response.Status.OK).entity(referenceId).build();
        } catch (Exception e) {
            log.error("Error in sending whatsapp communication", e);
            response = Response.status(Response.Status.OK).entity("Something went wrong").build();
        }
        return response;
    }

    private ActorDetails getActorDetail(WhatsAppSendRequest whatsAppSendRequest) {
        return ActorDetails.newBuilder().
                setMobileNumber(whatsAppSendRequest.getMobileNumber()).
                setLanguageCode(LanguageCode.valueOf(whatsAppSendRequest.getLanguageCode())).build();
    }

    private CommunicationEvent getCommunicationEvent(ActorDetails actorDetails, WhatsAppSendRequest whatsAppSendRequest, String referenceId) {
        CommunicationEvent.Builder communicationEventBuilder = CommunicationEvent.newBuilder()
                .setReceiverActorDetails(actorDetails)
                .setTemplateName(whatsAppSendRequest.getTemplateName())
                .addChannel(CommunicationChannel.WHATSAPP)
                .setReferenceId(referenceId);

        if (whatsAppSendRequest.getMedia() != null) {
            farm.nurture.core.contracts.common.enums.MediaType mediaType = farm.nurture.core.contracts.common.enums.MediaType.valueOf(whatsAppSendRequest.getMedia().getMediaType());
            Media.Builder mediaBuilder = Media.newBuilder().setMediaAccessType(MediaAccessType.valueOf(whatsAppSendRequest.getMedia().getMediaAccessType()))
                    .setMediaType(mediaType)
                    .setMediaInfo(whatsAppSendRequest.getMedia().getMediaInfo());

            if (mediaType == farm.nurture.core.contracts.common.enums.MediaType.DOCUMENT) {
                mediaBuilder.setDocumentName(whatsAppSendRequest.getMedia().getDocumentName());
            }
            communicationEventBuilder.setMedia(mediaBuilder.build());
        }

        if (whatsAppSendRequest.getPlaceholders() != null && whatsAppSendRequest.getPlaceholders().size() != 0) {
            List<Placeholder> placeholderList = templateHelper.getPlaceholderMapFromList(whatsAppSendRequest.getPlaceholders());
            communicationEventBuilder.addAllPlaceholder(placeholderList);
        }
        return communicationEventBuilder.build();
    }

    public DerivedCommunicationEvent getDerivedEvent(String referenceId, WhatsAppSendRequest whatsAppSendRequest) throws IOException {
        ActorDetails actorDetails = getActorDetail(whatsAppSendRequest);
        CommunicationEvent communicationEvent = getCommunicationEvent(actorDetails, whatsAppSendRequest, referenceId);
        Event<byte[], byte[]> event = new Event<>(communicationEvent.getReferenceId().getBytes(), communicationEvent.toByteArray());
        Language language = languageCache.getLanguageByCode(communicationEvent.getReceiverActorDetails()
                .getLanguageCode().name().toLowerCase().replace('_', '-'));
        Language secondaryLanguage=communicationEvent.getReceiverActorDetails().getSecondaryLanguageCode()!=null ?languageCache.getLanguageByCode(communicationEvent.getReceiverActorDetails()
                .getSecondaryLanguageCode().name().toLowerCase().replace('_', '-')):null;
        TemplateCacheValue templateCacheValue = templateHelper.getTemplateFromNameAndLangId(whatsAppSendRequest.getTemplateName(), language,secondaryLanguage);
        String content = templateHelper.getContent(templateCacheValue, whatsAppSendRequest.getPlaceholders());
        Map<String, Object> vendorMetaData = templateHelper.getMetaData(templateCacheValue);
        DerivedCommunicationEvent derivedEvent = new DerivedCommunicationEvent(event, referenceId, CLIENT_ID,
                content, language != null ? language.getUnicode() : false, null, null,
                CommunicationChannel.WHATSAPP, language.getId(), whatsAppSendRequest.getTemplateName(),
                whatsAppSendRequest.getPlaceholders(), null, null, vendorMetaData,
                StringUtils.isNonEmpty(whatsAppSendRequest.getCampaignName()) ?
                CLEVER_TAP_CAMPAIGN_PREFIX + whatsAppSendRequest.getCampaignName() : CLEVER_TAP_CAMPAIGN_PREFIX,
                whatsAppSendRequest.getUserId() == null ? 0L : whatsAppSendRequest.getUserId(),
                StringUtils.isNonEmpty(whatsAppSendRequest.getUserType()) ? ActorType.valueOf(whatsAppSendRequest.getUserType().toUpperCase()) : ActorType.NO_ACTOR);
        derivedEvent.setVendor(getVendor(communicationEvent.getVendor(),  CommunicationChannel.WHATSAPP.name()));
        if (whatsAppSendRequest.getMedia() != null) {
            DerivedCommunicationEvent.WhatsappAttributes.Media.MediaBuilder media = DerivedCommunicationEvent.WhatsappAttributes.Media.builder();
            media.mediaAccessType(communicationEvent.getMedia().getMediaAccessType()).mediaInfo(communicationEvent.getMedia().getMediaInfo())
                    .documentName(communicationEvent.getMedia().getDocumentName()).mediaType(communicationEvent.getMedia().getMediaType());
            derivedEvent.setWhatsappAttributes(new DerivedCommunicationEvent.WhatsappAttributes(
                    whatsAppSendRequest.getMobileNumber(), media.build()));
        } else {
            derivedEvent.setWhatsappAttributes(DerivedCommunicationEvent.WhatsappAttributes.builder().mobileNumber(whatsAppSendRequest.getMobileNumber()).build());
        }
        return derivedEvent;
    }

    private VendorType getVendor(CommunicationVendor vendor, String channel) {
        VendorType vendorType;
        if (vendor == CommunicationVendor.GUPSHUP) {
            vendorType = VendorType.GUPSHUP;
        } else if (vendor == CommunicationVendor.KARIX) {
            vendorType = VendorType.KARIX;
        } else {
            vendorType = vendorLoadBalancer.getVendor(channel);
        }
        return vendorType;

    }
}
