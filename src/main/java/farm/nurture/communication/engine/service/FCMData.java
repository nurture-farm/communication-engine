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

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FCMData {

    String AUTHORIZATION = "Authorization";
    String APPLICATION_JSON = "application/json";
    String CONTENT_TYPE = "Content-Type";
    String POST = "POST";
    String KEY = "key=";
    String TITLE = "title";
    String BODY = "body";
    String HIGH = "high";
	
	 private String to;

     private Map<String, String> data;

     private Map<String, String> notification;

     private String priority;

}
