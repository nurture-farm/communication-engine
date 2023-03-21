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


import java.util.HashMap;
import java.util.Map;

public class Utils {

    private static final String INDIA_COUNTRY_CODE = "91";
    private static final int VALID_MOBILE_NUMBER_LENGTH = 10;
    public static Map<String, Object> mergeAttributes(Map<String, String> sourceAttributes, Map<String, Object> destinationAttributes) {
        if (sourceAttributes != null && !sourceAttributes.isEmpty()) {
            if (destinationAttributes == null) destinationAttributes = new HashMap<>();

            for (Map.Entry<String, String> entry : sourceAttributes.entrySet()) {
                if (entry.getValue() == null) {
                    destinationAttributes.remove(entry.getKey());
                } else {
                    destinationAttributes.put(entry.getKey(), entry.getValue());
                }
            }
        }
        return destinationAttributes;
    }

    public static boolean validateMobileNumber(String mobileNumber){
        if(mobileNumber.length() == VALID_MOBILE_NUMBER_LENGTH || (mobileNumber.length() == 12 && mobileNumber.startsWith(INDIA_COUNTRY_CODE))
                || (mobileNumber.length() == 13 && mobileNumber.startsWith("+" + INDIA_COUNTRY_CODE)))
            return true;
        return false;

    }
}
