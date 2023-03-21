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

package farm.nurture.communication.engine;

import farm.nurture.infra.util.ApplicationConfiguration;
import farm.nurture.util.http.TimeOutConfig;

public class TimeOutConfigs {
    public static TimeOutConfig afsServiceTimeOutConfig()
    {
        TimeOutConfig timeOutConfig=new TimeOutConfig();
        ApplicationConfiguration configuration = ApplicationConfiguration.getInstance();
        int connectionTimeout=configuration.getInt("httpClientConfig.connectionTimeout.afs", 7000);
        int requestTimeout=configuration.getInt("httpClientConfig.requestTimeout.afs", 60000);
        timeOutConfig.setConnectionTimeout(connectionTimeout);
        timeOutConfig.setSoTimeout(requestTimeout);
        return timeOutConfig;
    }
    public static TimeOutConfig notificationInboxServiceTimeOutConfig()
    {
        TimeOutConfig timeOutConfig=new TimeOutConfig();
        ApplicationConfiguration configuration = ApplicationConfiguration.getInstance();
        int connectionTimeout=configuration.getInt("httpClientConfig.connectionTimeout.notificationInbox", 7000);
        int requestTimeout=configuration.getInt("httpClientConfig.requestTimeout.notificationInbox", 60000);
        timeOutConfig.setConnectionTimeout(connectionTimeout);
        timeOutConfig.setSoTimeout(requestTimeout);
        return timeOutConfig;
    }
    public static TimeOutConfig pushNotificationServiceTimeOutConfig() {
        TimeOutConfig timeOutConfig=new TimeOutConfig();
        ApplicationConfiguration configuration = ApplicationConfiguration.getInstance();
        int connectionTimeout = configuration.getInt("httpClientConfig.connectionTimeout.pushNotification", 7000);
        int requestTimeout = configuration.getInt("httpClientConfig.requestTimeout.pushNotification", 60000);
        timeOutConfig.setConnectionTimeout(connectionTimeout);
        timeOutConfig.setSoTimeout(requestTimeout);
        return timeOutConfig;
    }
    public static TimeOutConfig OptUserServiceTimeOutConfig() {
        TimeOutConfig timeOutConfig=new TimeOutConfig();
        ApplicationConfiguration configuration = ApplicationConfiguration.getInstance();
        int connectionTimeout=configuration.getInt("httpClientConfig.connectionTimeout.optUser", 7000);
        int requestTimeout=configuration.getInt("httpClientConfig.requestTimeout.optUser", 60000);
        timeOutConfig.setConnectionTimeout(connectionTimeout);
        timeOutConfig.setSoTimeout(requestTimeout);
        return timeOutConfig;
    }
    public static TimeOutConfig smsServiceTimeOutConfig() {
        TimeOutConfig timeOutConfig=new TimeOutConfig();
        ApplicationConfiguration configuration = ApplicationConfiguration.getInstance();
        int connectionTimeout = configuration.getInt("httpClientConfig.connectionTimeout.whatsApp.sms", 7000);
        int requestTimeout = configuration.getInt("httpClientConfig.requestTimeout.whatsApp.sms", 60000);
        timeOutConfig.setConnectionTimeout(connectionTimeout);
        timeOutConfig.setSoTimeout(requestTimeout);
        return timeOutConfig;
    }
    public static TimeOutConfig whatsappServiceGupshupTimeOutConfig()
    {
        TimeOutConfig timeOutConfig=new TimeOutConfig();
        ApplicationConfiguration configuration = ApplicationConfiguration.getInstance();
        int connectionTimeout = configuration.getInt("httpClientConfig.connectionTimeout.whatsApp.gupshup", 7000);
        int requestTimeout = configuration.getInt("httpClientConfig.requestTimeout.whatsApp.gupshup", 60000);
        timeOutConfig.setConnectionTimeout(connectionTimeout);
        timeOutConfig.setSoTimeout(requestTimeout);
        return timeOutConfig;
    } public static TimeOutConfig whatsappServiceKarixTimeOutConfig()
    {
        TimeOutConfig timeOutConfig=new TimeOutConfig();
        ApplicationConfiguration configuration = ApplicationConfiguration.getInstance();
        int connectionTimeout = configuration.getInt("httpClientConfig.connectionTimeout.whatsApp.karix", 7000);
        int requestTimeout = configuration.getInt("httpClientConfig.requestTimeout.whatsApp.karix", 60000);
        timeOutConfig.setConnectionTimeout(connectionTimeout);
        timeOutConfig.setSoTimeout(requestTimeout);
        return timeOutConfig;
    }
}
