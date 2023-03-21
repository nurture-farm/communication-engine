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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.module.SimpleModule;
import farm.nurture.util.serializer.AbstractJacksonSerializer;


public class PlainJsonSerializer extends AbstractJacksonSerializer {

    public PlainJsonSerializer() {
        this(new ObjectMapper(), null);
    }

    public PlainJsonSerializer(final ObjectMapper mapper, final PropertyNamingStrategy propertyNamingStrategy) {
        super(mapper, propertyNamingStrategy);
    }

    @Override
    protected void registerModules(ObjectMapper mapper) {
        mapper.registerModule(new SimpleModule());
    }

}