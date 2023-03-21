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

package farm.nurture.communication.engine.event;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import farm.nurture.communication.engine.service.PushNotificationService;

@Slf4j
@AllArgsConstructor
public class PushNotificationHandler implements Runnable {

    private PushNotificationService pushNotificationService;

    private DerivedCommunicationEvent event;

    @Override
    public void run() {
        pushNotificationService.sendPushNotification(event);
    }
}
