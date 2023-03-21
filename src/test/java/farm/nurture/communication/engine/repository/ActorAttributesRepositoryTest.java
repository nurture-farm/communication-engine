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

package farm.nurture.communication.engine.repository;

import farm.nurture.communication.engine.H2Extension;
import farm.nurture.communication.engine.models.ActorAttributes;
import farm.nurture.core.contracts.common.enums.ActorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith({H2Extension.class})
public class ActorAttributesRepositoryTest {
    private static final long actorId = 1l;
    private static final String attrKey = "whatsapp_optin_status";
    private static final String attValue = "OPT_IN";

    private final ActorAttributesRepository attributesRepository = new ActorAttributesRepository();

    private ActorAttributes attributes = null;

    @BeforeEach
    public void setUp() {
        attributes = ActorAttributes.builder().actorId(actorId).actorType(ActorType.FARMER).nameSpace(ActorAttributes.NameSpace.NURTURE_FARM)
                .attrKey(attrKey).attrValue(attValue).build();
    }

    @Test
    public void testInsertActorAttributes() {
        boolean isInserted = attributesRepository.insertActorAttributes(attributes);

        ActorAttributes actorAttributes = attributesRepository.getByActorAndAttributeKey(actorId, ActorType.FARMER, ActorAttributes.NameSpace.NURTURE_FARM, attrKey);
        assertTrue(isInserted);
        assertEquals(actorId, actorAttributes.getActorId());
        assertEquals(attrKey, actorAttributes.getAttrKey());
        assertEquals(attValue, actorAttributes.getAttrValue());
        assertEquals(ActorType.FARMER, actorAttributes.getActorType());
        assertEquals(ActorAttributes.NameSpace.NURTURE_FARM, actorAttributes.getNameSpace());
    }

    @Test
    public void testUpdateExistingActorAttributes() {
        final String updatedAttrValue = "OPT_OUT";
        attributes.setAttrValue(updatedAttrValue);
        boolean isUpdated = attributesRepository.updateActorAttributes(attributes);

        ActorAttributes actorAttributes = attributesRepository.getByActorAndAttributeKey(actorId, ActorType.FARMER, ActorAttributes.NameSpace.NURTURE_FARM, attrKey);
        assertTrue(isUpdated);
        assertEquals(actorId, actorAttributes.getActorId());
        assertEquals(attrKey, actorAttributes.getAttrKey());
        assertEquals(updatedAttrValue, actorAttributes.getAttrValue());
        assertEquals(ActorType.FARMER, actorAttributes.getActorType());
        assertEquals(ActorAttributes.NameSpace.NURTURE_FARM, actorAttributes.getNameSpace());
    }

    @Test
    public void testUpdateNonExistingActorAttributes() {
        final String updatedAttrValue = "OPT_OUT";
        attributes.setAttrValue(updatedAttrValue);
        attributes.setActorType(ActorType.OPERATOR);
        attributes.setNameSpace(ActorAttributes.NameSpace.NURTURE_SUSTAIN);
        boolean isUpdated = attributesRepository.updateActorAttributes(attributes);

        ActorAttributes actorAttributes = attributesRepository.getByActorAndAttributeKey(actorId, ActorType.OPERATOR, ActorAttributes.NameSpace.NURTURE_SUSTAIN, attrKey);
        assertTrue(isUpdated);
        assertEquals(actorId, actorAttributes.getActorId());
        assertEquals(attrKey, actorAttributes.getAttrKey());
        assertEquals(updatedAttrValue, actorAttributes.getAttrValue());
        assertEquals(ActorType.OPERATOR, actorAttributes.getActorType());
        assertEquals(ActorAttributes.NameSpace.NURTURE_SUSTAIN, actorAttributes.getNameSpace());
    }

    @Test
    public void testExceptionInInsertActorAttributes() {
        attributes = Mockito.mock(ActorAttributes.class);
        when(attributes.getActorId()).thenThrow(new RuntimeException("Exception"));

        boolean isInserted = attributesRepository.insertActorAttributes(attributes);

        ActorAttributes actorAttributes = attributesRepository.getByActorAndAttributeKey(actorId, ActorType.FARMER, ActorAttributes.NameSpace.NURTURE_SUSTAIN, attrKey);
        assertFalse(isInserted);
        assertNull(actorAttributes);
    }

    @Test
    public void testDeleteActorAttributes() {
        attributesRepository.insertActorAttributes(attributes);
        boolean isDeleted = attributesRepository.deleteActorAttributes(attributes);

        ActorAttributes actorAttributes = attributesRepository.getByActorAndAttributeKey(actorId, ActorType.FARMER, ActorAttributes.NameSpace.NURTURE_FARM, attrKey);
        assertTrue(isDeleted);
        assertEquals(actorId, actorAttributes.getActorId());
        assertEquals(attrKey, actorAttributes.getAttrKey());
        assertEquals(attValue, actorAttributes.getAttrValue());
        assertEquals(ActorType.FARMER, actorAttributes.getActorType());
        assertEquals(ActorAttributes.NameSpace.NURTURE_FARM, actorAttributes.getNameSpace());
        assertNotNull(actorAttributes.getDeletedAt());
    }

    @Test
    public void testRowNotExistForDeleting() {
        attributes.setActorType(ActorType.SUPPORT_AGENT);
        attributes.setNameSpace(ActorAttributes.NameSpace.NURTURE_SUSTAIN);
        boolean isDeleted = attributesRepository.deleteActorAttributes(attributes);

        assertFalse(isDeleted);
    }
}
