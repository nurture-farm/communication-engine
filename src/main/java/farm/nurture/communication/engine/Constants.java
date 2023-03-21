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

import java.time.ZoneId;

public class Constants {

    public static final String AUTHORIZATION = "Authorization";
    public static final String APPLICATION_JSON = "application/json";
    public static final String APPLICATION_X_WWW_FORM_URLENCODED= "application/x-www-form-urlencoded";
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String ACCEPT_LANGUAGE = "Accept-Language";
    public static final String LANGUAGE_ENGLISH = "en";
    public static final String API_KEY = "API-Key";
    public static final Integer OK_STATUS_CODE = 200;
    public static final String OK_STATUS_MESSAGE = "OK";
    public static final String APP_NOTIFICATION = "APP_NOTIFICATION";
    public static final String SMS = "SMS";
    public static final String WHATSAPP = "WHATSAPP";
    public static final String EMAIL = "EMAIL";
    public static final String KEY = "key";
    public static final String VALUE = "value";
    public static final String LABEL_TEMPLATE = "template";
    public static final String LABEL_LANGUAGE = "language";
    public static final String LABEL_ACTORTYPE = "actortype";
    public static final String LABEL_STATE = "state";
    public static final String LABEL_VENDOR = "vendor";
    public static final String[] LABEL_TEMPLATE_LIST = {LABEL_TEMPLATE};
    public static final String[] LABEL_TEMPLATE_LANGUAGE_LIST = {LABEL_TEMPLATE,LABEL_LANGUAGE};
    public static final String[] LABEL_TEMPLATE_ACTORTYPE_LIST = {LABEL_TEMPLATE,LABEL_ACTORTYPE};
    public static final String[] LABEL_TEMPLATE_LANGUAGE_STATE_LIST = {LABEL_TEMPLATE,LABEL_LANGUAGE,LABEL_STATE, LABEL_VENDOR};
    public static final String MULTIPART_FORM_DATA = "multipart/form-data";
    public static final String GUPSHUP_TEMPLATE_NAME = "gupshup_template_name";
    public static final String GUPSHUP_TEMPLATE_ID= "gupshup_template_id";
    public static final String KARIX_TEMPLATE_NAME = "karix_template_name";
    public static final String KARIX_TEMPLATE_ID = "karix_template_id";
    public static final String OPT_USER_DUPLICATE_ENTRY_CODE = "312";
    public static final String OPT_USER_SUCCESS_STATUS = "success";

    public static final String DLT_TEMPLATE_ID = "DLT_TEMPLATE_ID";

    public static final String API_RESPONSE_SUCCESS_STATUS = "success";
    public static final String API_RESPONSE_ERROR_STATUS = "error";

    public static final String DRAGON_FLY_INTERNAL_ERROR = "INTERNAL_ERROR";
    public static final String MEDIA_ERROR = "MEDIA_ERROR";
    public static final String MEDIA_SUCCESS = "MEDIA_SUCCESS";

    public static final String DEFAULT_TIMEZONE = "UTC";
    public static final ZoneId DEFAULT_ZONE_ID = ZoneId.of(DEFAULT_TIMEZONE);

    public static final String TEMPLATE_NAME_IN_KARIX = "karix_template_name";

    public static final String EVENT_PORTAL_HOST = "event.portal.host";
    public static final String EVENT_PORTAL_PORT = "event.portal.port";
    public static final String EVENT_PORTAL_DEFAULT_HOST ="event-portal-service.platform.svc.cluster.local";
    public static final int EVENT_PORTAL_DEFAULT_PORT = 8085;
    public static final String NUM_THREADS = "event.portal.thread";
    public static final String EVENT_NAME = "WHATSAPP DELIVERY EVENT";
    public static final String LABEL_CAMPAIGN="campaign_name";
    public static final String LABEL_STATUS = "status";
    public static final String LABEL_LANGUAGE_CODE = "language_code";
    public static final String CLEVER_TAP_CAMPAIGN_PREFIX = "CLTAP_";
    public static final String QUICK_REPLY = "QuickReply";
    public static final String CALL_TO_ACTION = "CallToAction";
    public static final String INTERACTIVE_ATTRIBUTES_KEY = "interactive_attributes";
    public static final String ACCEPT = "Accept";
    public static final String ACCEPT_VALUE = "*/*";
    public static final String AUTHENTICATION = "Authentication";
    public static final String KARIX_SMS_VERSION = "karix.sms.ver";
    public static final String KARIX_SMS_KEY = "karix.sms.key";
    public static final String KARIX_SMS_URL = "karix.sms.url";
    public static final String KARIX_SMS_SEND = "karix.sms.send";
    public static final String UC_TYPE = "UC";
    public static final String PM_TYPE = "PM";
    public static final String KARIX_WHATSAPP_KEY = "karix.whatsapp.key"; // odCjBxgrskPJ14FmGykCQA==
    public static final String KARIX_WHATSAPP_URL = "karix.whatsapp.url"; //
    public static final String KARIX_WHATSAPP_SENDER = "karix.whatsapp.sender";
    public static final String KARIX_WHATSAPP_OPTIN_URL = "karix.whatsapp.optin.url";
    public static final String KARIX_CATEGORY = "TRANSACTIONAL";
    public static final String KARIX_WEBHOOK_URL = "https://httpbin.org/post";
    public static final String BODY = "BODY";
    public static final String HEADER = "HEADER";
    public static final String FOOTER = "FOOTER";
    public static final String BUTTONS = "BUTTONS";
    public static final String MEDIA_TYPE = "media_type";
    public static final String TEXT = "text";
    public static final String IMAGE = "image";
    public static final String DOCUMENT = "document";
    public static final String VIDEO = "video";
    public static final String KARIX_WHATSAPP_CREATE_URL = "karix.whatsapp.create.url";
    public static final String KARIX_WHATSAPP_UPLOAD_URL = "karix.whatsapp.upload.url";
    public static final String GUPSHUP_WHATSAPP_USERNAME = "gupshup.whatsapp.username"; // odCjBxgrskPJ14FmGykCQA==
    public static final String GUPSHUP_WHATSAPP_PASSWORD = "gupshup.whatsapp.password"; //
    public static final String GUPSHUP_WHATSAPP_VERSION = "gupshup.whatsapp.version";
    public static final String GUPSHUP_WHATSAPP_URL = "gupshup.whatsapp.url";
    public static final String GUPSHUP_SMS_USERNAME = "gupshup.sms.username";
    public static final String GUPSHUP_SMS_PASSWORD = "gupshup.sms.password";
    public static final String GUPSHUP_SMS_URL = "gupshup.sms.url";
    public static final String HEADERS_EXAMPLE = "header_examples";
    public static final String GUPSHUP_CREATE_TEMPLATE_METHOD_NAME = "create_whatsapp_hsm";
    public static final String GUPSHUP_CATEGORY = "TRANSACTIONAL";
    public static final String GUPSHUP_WHATSAPP_CREATE_URL = "gupshup.whatsapp.create.url";
    public static final String WHATSAPP_AUTH_SCHEME = "PLAIN";
    public static final String OPT_CHANNEL = "WHATSAPP";
    public static final String JSON = "json";

}
