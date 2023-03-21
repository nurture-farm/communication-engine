package farm.nurture.communication.engine.vendor;

import com.google.inject.Singleton;
import farm.nurture.communication.engine.Constants;
import farm.nurture.communication.engine.dto.*;
import farm.nurture.communication.engine.event.DerivedCommunicationEvent;
import farm.nurture.communication.engine.models.Template;
import farm.nurture.communication.engine.models.WhatsappUsers;
import farm.nurture.core.contracts.common.enums.LanguageCode;
import farm.nurture.infra.util.StringUtils;
import farm.nurture.util.http.HttpUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.http.HttpEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static farm.nurture.communication.engine.Constants.*;


@Slf4j
@Singleton
public class KarixVendor extends Vendor {

    @Override
    public HttpClientRequest<KarixSmsRequest> requestForSendSms(DerivedCommunicationEvent event) {
        KarixSmsRequest karixSmsRequest = new KarixSmsRequest();
        karixSmsRequest.setVer(config.get(KARIX_SMS_VERSION));
        karixSmsRequest.setKey(config.get(KARIX_SMS_KEY));
        KarixSmsRequest.Messages messages = new KarixSmsRequest.Messages();
        if (event.isUnicode()) {
            messages.setType(UC_TYPE);
        } else {
            messages.setType(PM_TYPE);
        }
        messages.setDest(Collections.singletonList(event.getSmsAttributes().getMobileNumber().trim()));
        messages.setText(event.getContent());
        messages.setSend(config.get(KARIX_SMS_SEND));
        List<KarixSmsRequest.Messages> messagesList = new ArrayList<>();
        messagesList.add(messages);
        karixSmsRequest.setMessages(messagesList);
        HttpClientRequest<KarixSmsRequest> sendRequest = new HttpClientRequest<>();
        sendRequest.setMethod(HttpUtils.HttpMethod.POST);
        sendRequest.setRequestBody(karixSmsRequest);
        sendRequest.setUrl(config.get(KARIX_SMS_URL));
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.CONTENT_TYPE, Constants.APPLICATION_JSON);
        headers.put(ACCEPT, ACCEPT_VALUE);
        sendRequest.setHeaders(headers);
        return sendRequest;
    }


    public VendorType getVendorName() {
        return VendorType.KARIX;
    }

    @Override
    public HttpClientRequest<KarixWhatsAppRequest> getWhatsAppData(DerivedCommunicationEvent event) {
        HttpClientRequest<KarixWhatsAppRequest> sendRequest = new HttpClientRequest<>();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.CONTENT_TYPE, Constants.APPLICATION_JSON);
        headers.put(ACCEPT, ACCEPT_VALUE);
        headers.put(AUTHENTICATION, "Bearer " + config.get(KARIX_WHATSAPP_KEY));
        sendRequest.setHeaders(headers);
        sendRequest.setUrl(config.get(KARIX_WHATSAPP_URL));
        sendRequest.setMethod(HttpUtils.HttpMethod.POST);
        KarixWhatsAppRequest karixWhatsappRequest = new KarixWhatsAppRequest();
        karixWhatsappRequest.setMetaData(new KarixWhatsAppRequest.MetaData("v1.0.9"));
        KarixWhatsAppRequest.Message message = new KarixWhatsAppRequest.Message();
        message.setChannel("WABA");
        message.setSender(new KarixWhatsAppRequest.Sender(config.get(KARIX_WHATSAPP_SENDER)));
        message.setPreferences(new KarixWhatsAppRequest.Preferences("1001"));

        message.setRecipient(new KarixWhatsAppRequest.Recipient(formatMobileNumber(event.getWhatsappAttributes().getMobileNumber()), "individual"));

        Map<String, Object> vendorMetaData = event.getVendorMetaData();
        String templateName = event.getTemplateName();
        if (vendorMetaData != null && !vendorMetaData.isEmpty() && vendorMetaData.containsKey(TEMPLATE_NAME_IN_KARIX)) {
            templateName = (String) vendorMetaData.get(TEMPLATE_NAME_IN_KARIX);
        }
        InteractiveAttributes interactiveAttributes = parseIntegrativeAttributes(vendorMetaData);
        String headerExample = getHeaderExample(interactiveAttributes, event.getPlaceholders());
        if (event.getWhatsappAttributes().getMedia() == null && StringUtils.isEmpty(headerExample) ){
            message.setContent(new KarixWhatsAppRequest.Content(false, "TEMPLATE",
                    new KarixWhatsAppRequest.Template(templateName, event.getAttributesMap()),
                    null));

        } else {
            DerivedCommunicationEvent.WhatsappAttributes.Media mediaValue = event.getWhatsappAttributes().getMedia();
            KarixWhatsAppRequest.Buttons buttons = parseButtonsForWhatsAppRequest(interactiveAttributes);
            KarixWhatsAppRequest.Media media = getMedia(headerExample, mediaValue);
            KarixWhatsAppRequest.MediaTemplate mediaTemplate = new KarixWhatsAppRequest.MediaTemplate(templateName, event.getAttributesMap(), media, buttons);
            message.setContent(new KarixWhatsAppRequest.Content(false, "MEDIA_TEMPLATE", null, mediaTemplate));
        }
        karixWhatsappRequest.setMessage(message);
        sendRequest.setRequestBody(karixWhatsappRequest);
        return sendRequest;
    }

    @Override
    public HttpClientRequest getWhatsAppOptInData(String mobileNumber, WhatsappUsers.WhatsAppStatus optType) {
        HttpClientRequest<KarixWhatsAppOptinRequest> httpClientRequest = new HttpClientRequest<>();
        httpClientRequest.setMethod(HttpUtils.HttpMethod.POST);

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.CONTENT_TYPE, Constants.APPLICATION_JSON);
        headers.put(ACCEPT, ACCEPT_VALUE);

        httpClientRequest.setHeaders(headers);
        httpClientRequest.setUrl(config.get(KARIX_WHATSAPP_OPTIN_URL));

        KarixWhatsAppOptinRequest karixWhatsappOptinRequest = new KarixWhatsAppOptinRequest();
        karixWhatsappOptinRequest.setAction("optin");

        karixWhatsappOptinRequest.setMobile(formatMobileNumber(mobileNumber));
        karixWhatsappOptinRequest.setPin(config.get(KARIX_WHATSAPP_KEY));
        karixWhatsappOptinRequest.setOptinid(config.get(KARIX_WHATSAPP_SENDER));
        httpClientRequest.setRequestBody(karixWhatsappOptinRequest);

        return httpClientRequest;
    }

    @Override
    public HttpClientRequest<KarixCreateTemplateRequest> createWhatsAppTemplate(Template template, String file) {
        HttpClientRequest<KarixCreateTemplateRequest> httpClientRequest = new HttpClientRequest<>();

        KarixCreateTemplateRequest karixCreateTemplateRequest = KarixCreateTemplateRequest.builder().category(KARIX_CATEGORY)
                .language(languageToVendorLanguageMap.get(LanguageCode.valueOf(template.getLanguageId())))
                .template_name(template.getName()+"_"+template.getLanguageId())
                .webhook(KarixCreateTemplateRequest.Webhook.builder().url(KARIX_WEBHOOK_URL).build())
                .build();

        KarixCreateTemplateRequest.Components bodyComponent = KarixCreateTemplateRequest.Components.builder().text(parseContent(template))
                .type(BODY)
                .build();
        if (template.getAttributes() != null && !template.getAttributes().isEmpty()) {
            String[] bodyPlaceholderExample = mapAttributeValue(template.getAttributes());
            List<String> bodyPlaceholderList = new ArrayList<>(Arrays.asList(bodyPlaceholderExample));
            bodyComponent.setExample(KarixCreateTemplateRequest.Example.builder().body_text(
                    Collections.singletonList(bodyPlaceholderList)).build());
        }
        List<KarixCreateTemplateRequest.Components> componentsList = new ArrayList<>();
        componentsList.add(bodyComponent);

        // Upload Media Data
        if (template.getMetaData().containsKey(MEDIA_TYPE)
                && !template.getMetaData().get(MEDIA_TYPE).equals(TEXT)) {
            List<String> headerHandle = new ArrayList<>();
            headerHandle.add(file);
            KarixCreateTemplateRequest.Components mediaComponent = KarixCreateTemplateRequest.Components.builder().type(HEADER)
                    .format(getFormat((String) template.getMetaData().get(MEDIA_TYPE)))
                    .example(KarixCreateTemplateRequest.Example.builder().header_handle(headerHandle).build())
                    .build();
            componentsList.add(mediaComponent);
        }

        getInteractiveAttributes(template, componentsList);
        karixCreateTemplateRequest.setComponents(componentsList);

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.CONTENT_TYPE, Constants.APPLICATION_JSON);
        headers.put(ACCEPT, ACCEPT_VALUE);
        headers.put(AUTHENTICATION, "Bearer " + config.get(KARIX_WHATSAPP_KEY));

        httpClientRequest.setRequestBody(karixCreateTemplateRequest);
        httpClientRequest.setMethod(HttpUtils.HttpMethod.POST);
        httpClientRequest.setUrl(config.get(KARIX_WHATSAPP_CREATE_URL));
        httpClientRequest.setHeaders(headers);

        return httpClientRequest;
    }


    public HttpClientRequest<HttpEntity> uploadMedia(String fileName) {
        HttpClientRequest<HttpEntity> httpClientRequest = new HttpClientRequest<>();

        String extension = findFileExtension(fileName).orElse(null);
        String fileType = findFileType(extension);
        Map<String, String> headers = new HashMap<>();
        headers.put(ACCEPT, ACCEPT_VALUE);
        headers.put(AUTHENTICATION, "Bearer " + config.get(KARIX_WHATSAPP_KEY));

        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
        builder.addPart("file", createFile(fileName));
        builder.addPart("file_type", createFormData(fileType));
        HttpEntity httpEntity = builder.build();
        httpClientRequest.setRequestBody(httpEntity);
        httpClientRequest.setMethod(HttpUtils.HttpMethod.POST);
        httpClientRequest.setUrl(config.get(KARIX_WHATSAPP_UPLOAD_URL));
        httpClientRequest.setHeaders(headers);
        return httpClientRequest;
    }

    private KarixWhatsAppRequest.Buttons parseButtonsForWhatsAppRequest(InteractiveAttributes interactiveAttributes) {
        if (interactiveAttributes == null || CollectionUtils.isEmpty(interactiveAttributes.getButtons())
                || StringUtils.isEmpty(interactiveAttributes.getButton_category()) ||
                !interactiveAttributes.getButton_category().equalsIgnoreCase(QUICK_REPLY)) {
            return null;
        }
        AtomicInteger index = new AtomicInteger(0);
        List<KarixWhatsAppRequest.QuickReplies> quickRepliesList =
                interactiveAttributes.getButtons().stream().map(button -> {
                    return new KarixWhatsAppRequest.QuickReplies(String.valueOf(index.getAndIncrement()), button.text);
                }).collect(Collectors.toList());
        return KarixWhatsAppRequest.Buttons.builder().quickReplies(quickRepliesList).build();
    }

    public void getInteractiveAttributes(Template template, List<KarixCreateTemplateRequest.Components> componentsList) {
        if (template.getMetaData() != null && template.getMetaData().containsKey(INTERACTIVE_ATTRIBUTES_KEY)) {
            try {
                InteractiveAttributes interactiveAttributes = parseIntegrativeAttributes(template.getMetaData());
                if (interactiveAttributes != null) {
                    if (StringUtils.isNonEmpty(interactiveAttributes.getFooter())) {
                        KarixCreateTemplateRequest.Components footerComponent = KarixCreateTemplateRequest.Components.builder().type(FOOTER)
                                .text(interactiveAttributes.getFooter()).build();
                        componentsList.add(footerComponent);
                    }
                    if (StringUtils.isNonEmpty(interactiveAttributes.getHeaders())) {
                        KarixCreateTemplateRequest.Components headerComponent = KarixCreateTemplateRequest.Components.builder().type(HEADER)
                                .text(getHeaderForCreateTemplateRequest(interactiveAttributes.getHeaders(), interactiveAttributes.getHeader_examples()))
                                .build();
                        if (StringUtils.isNonEmpty(interactiveAttributes.getHeader_examples())) {
                            headerComponent.setExample(KarixCreateTemplateRequest.Example.builder()
                                    .header_text(Collections.singletonList(interactiveAttributes.getHeader_examples()))
                                    .build());
                        }
                        headerComponent.setFormat(TEXT.toUpperCase());
                        componentsList.add(headerComponent);
                    }
                    if (StringUtils.isNonEmpty(interactiveAttributes.getButton_category()) && CollectionUtils.isNotEmpty(interactiveAttributes.getButtons())) {
                        List<KarixCreateTemplateRequest.Button> buttonList = null;
                        switch (interactiveAttributes.getButton_category()) {
                            case CALL_TO_ACTION:
                                buttonList = interactiveAttributes.getButtons().stream().map(button -> {
                                    if (StringUtils.isNonEmpty(button.getPhone_number())) {
                                        return KarixCreateTemplateRequest.Button.builder().phone_number(button.getPhone_number())
                                                .type("PHONE_NUMBER").text(button.getText())
                                                .build();
                                    } else {
                                        return KarixCreateTemplateRequest.Button.builder()
                                                .type("URL").text(button.getText()).url(button.getUrl())
                                                .example(Collections.singletonList(button.getUrl()))
                                                .build();
                                    }
                                }).collect(Collectors.toList());
                                break;
                            case QUICK_REPLY:
                                buttonList = interactiveAttributes.getButtons().stream().map(button -> {
                                    return KarixCreateTemplateRequest.Button.builder().text(button.getText()).type("QUICK_REPLY").build();
                                }).collect(Collectors.toList());
                                break;
                        }
                        KarixCreateTemplateRequest.Components buttonComponent = KarixCreateTemplateRequest.Components.builder().type(BUTTONS)
                                .buttons(buttonList)
                                .build();
                        componentsList.add(buttonComponent);
                    }
                }
            } catch (Exception e) {
                log.error("Interactive Attribute Template Exception  ", e);
            }
        }
    }

    private KarixWhatsAppRequest.Media getMedia(String headerExample, DerivedCommunicationEvent.WhatsappAttributes.Media mediaValue) {
        KarixWhatsAppRequest.Media media = null;
        if (StringUtils.isNonEmpty(headerExample)) {
            media = new KarixWhatsAppRequest.Media(TEXT,
                    null,
                    null, headerExample);
        } else {
            switch (mediaValue.getMediaType()) {
                case IMAGE:
                    media = new KarixWhatsAppRequest.Media(IMAGE,
                            mediaValue.getMediaInfo(),
                            null, null);
                    break;
                case VIDEO:
                    media = new KarixWhatsAppRequest.Media(VIDEO,
                            mediaValue.getMediaInfo(),
                            null, null);
                    break;
                case DOCUMENT:
                    media = new KarixWhatsAppRequest.Media(DOCUMENT,
                            mediaValue.getMediaInfo(),
                            mediaValue.getDocumentName(), null);
                    break;

            }
        }
        return media;
    }

    private String formatMobileNumber(String mobileNumber) {
        StringBuilder mobileNumberBuilder = new StringBuilder();
        if (mobileNumber != null && mobileNumber.length() == 10) {
            mobileNumberBuilder.append("91");
            mobileNumberBuilder.append(mobileNumber);
        } else if (mobileNumber != null && mobileNumber.length() == 13) {
            mobileNumberBuilder.append(mobileNumber.substring(1));
        } else {
            mobileNumberBuilder.append(mobileNumber);
        }
        return mobileNumberBuilder.toString();
    }
}

