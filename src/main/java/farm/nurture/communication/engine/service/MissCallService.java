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

package farm.nurture.communication.engine.service;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import farm.nurture.communication.engine.models.ActorAttributes;
import farm.nurture.communication.engine.models.WhatsappUsers;
import farm.nurture.communication.engine.repository.ActorAttributesRepository;
import farm.nurture.communication.engine.repository.ActorCommunicationDetailsRepository;
import farm.nurture.communication.engine.repository.WhatsappUsersRepository;
import farm.nurture.communication.engine.resource.MissCallResource;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class MissCallService {
    private static final String attrKey = "whatsapp_optin_status";

    @Inject
    private ActorCommunicationDetailsRepository actorCommunicationDetailsRepository;

    @Inject
    private WhatsappUsersRepository whatsappUsersRepository;

    @Inject
    private ActorAttributesRepository actorAttributesRepository;

    public boolean updateWhatsAppStatus(String senderMobile, MissCallResource.Status status) {
        ActorCommunicationDetailsRepository.ResponseObject responseObject = actorCommunicationDetailsRepository.getByMobileNumberType(senderMobile);
        log.info("Value for responseObject in updateWhatsAppStatus for MissCallResource is : {}", responseObject);
        if (!responseObject.success) {
            return false;
        } else if (responseObject.actorCommunicationDetails == null) {
            log.info("there is no entry in actor_communication_details for given mobileNumber : {}", senderMobile);
            return updateWhatsappUserTable(senderMobile, status);
        }
        ActorAttributes actorAttributes = ActorAttributes.builder().actorId(responseObject.actorCommunicationDetails.getActorId()).actorType(responseObject.actorCommunicationDetails.getActorType())
                .nameSpace(ActorAttributes.NameSpace.NURTURE_FARM).attrKey(attrKey).attrValue(status.name()).build();
        log.info("Value for ActorAttributes in updateWhatsAppStatus for MissCallResource is : {}", actorAttributes);
        return actorAttributesRepository.updateActorAttributes(actorAttributes);
    }

    private boolean updateWhatsappUserTable(String senderMobile, MissCallResource.Status status) {
        boolean updateWhatsAppUserTable;
        WhatsappUsers whatsappUsers;
        if (status == MissCallResource.Status.OPT_IN) {
            whatsappUsers = WhatsappUsers.builder().mobileNumber(senderMobile).status(WhatsappUsers.WhatsAppStatus.OPT_IN).build();
        } else {
            whatsappUsers = WhatsappUsers.builder().mobileNumber(senderMobile).status(WhatsappUsers.WhatsAppStatus.OPT_OUT).build();
        }
        updateWhatsAppUserTable = whatsappUsersRepository.findAndUpdateWhatsappUsers(whatsappUsers);
        log.info("status for updating WhatsAppUserTable is updateWhatsAppUserTable : {}", updateWhatsAppUserTable);
        return updateWhatsAppUserTable;
    }
}
