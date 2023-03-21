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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import farm.nurture.communication.engine.cache.LanguageCache;
import farm.nurture.communication.engine.cache.MobileAppDetailsCache;
import farm.nurture.communication.engine.cache.TemplateCache;
import farm.nurture.communication.engine.event.ActorAppTokenEventHandler;
import farm.nurture.communication.engine.event.ActorCommunicationDetailsEventHandler;
import farm.nurture.communication.engine.event.CommunicationEventHandler;
import farm.nurture.communication.engine.grpc.CommunicationEngine;
import farm.nurture.communication.engine.grpc.CommunicationEngineImpl;
import farm.nurture.communication.engine.grpc.GrpcService;
import farm.nurture.communication.engine.helper.RequestMapper;
import farm.nurture.communication.engine.helper.RequestValidator;
import farm.nurture.communication.engine.helper.ResponseMapper;
import farm.nurture.communication.engine.helper.TemplateHelper;
import farm.nurture.communication.engine.kafka.KafkaProducerWrapperService;
import farm.nurture.communication.engine.kafka.KafkaProducerWrapperServiceImpl;
import farm.nurture.communication.engine.repository.*;
import farm.nurture.communication.engine.resource.MissCallResource;
import farm.nurture.communication.engine.service.*;
import farm.nurture.communication.engine.utils.ExecutorServiceImpl;
import farm.nurture.communication.engine.utils.VendorLoadBalancer;
import farm.nurture.communication.engine.vendor.GupShupVendor;
import farm.nurture.communication.engine.vendor.KarixVendor;
import farm.nurture.infra.util.ApplicationConfiguration;
import farm.nurture.kafka.Producer;
import farm.nurture.kafka.config.KafkaProducerConfig;
import farm.nurture.kafka.impl.KafkaProducer;
import farm.nurture.util.http.AsyncHttpClientFactory;
import farm.nurture.util.http.HttpClientConfig;
import farm.nurture.util.http.HttpClientFactory;
import farm.nurture.util.http.TimeOutConfig;
import farm.nurture.util.http.client.NFAsyncHttpClient;
import farm.nurture.util.http.client.NFHttpClient;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;

import java.util.Properties;

public class DIModule extends AbstractModule {

    @Override
    protected void configure() {

        bind(GrpcService.class).in(Singleton.class);
        bind(CommunicationEngine.class).toInstance(new CommunicationEngineImpl());

//      Initialize repositories
        bind(LanguageRepository.class).in(Singleton.class);
        bind(MobileAppDetailsRepository.class).in(Singleton.class);
        bind(TemplateRepository.class).in(Singleton.class);
        bind(ActorCommunicationDetailsRepository.class).in(Singleton.class);
        bind(ActorAppTokenRepository.class).in(Singleton.class);
        bind(MessageAcknowledgementRepository.class).in(Singleton.class);

//      Initialize caches
        bind(LanguageCache.class).in(Singleton.class);
        bind(MobileAppDetailsCache.class).in(Singleton.class);
        bind(TemplateCache.class).in(Singleton.class);

//      Initialize Load balancer
        bind(VendorLoadBalancer.class).in(Singleton.class);
        bind(KarixVendor.class).in(Singleton.class);
        bind(GupShupVendor.class).in(Singleton.class);

        bind(TemplateHelper.class).in(Singleton.class);

//      Initialize event handlers
        bind(CommunicationEventHandler.class).in(Singleton.class);
        bind(ActorCommunicationDetailsEventHandler.class).in(Singleton.class);
        bind(ActorAppTokenEventHandler.class).in(Singleton.class);

//      Initialize resources
        bind(MissCallResource.class).in(Singleton.class);
        bind(OptUserService.class).in(Singleton.class);

//      Initialize resources
        bind(RequestMapper.class).in(Singleton.class);
        bind(ResponseMapper.class).in(Singleton.class);
        bind(RequestValidator.class).in(Singleton.class);


//      Initialize external services
        bind(SMSService.class).in(Singleton.class);
        bind(PushNotificationService.class).in(Singleton.class);
        bind(EmailService.class).in(Singleton.class);
        bind(WhatsappService.class).in(Singleton.class);
        bind(TemplateManagementService.class).in(Singleton.class);
//      Initialize external libraries
        bind(ObjectMapper.class).toInstance(buildObjectMapper());
        bind(NFAsyncHttpClient.class).toInstance(buildAsyncHttpClient());
        bind(NFHttpClient.class).toInstance(buildHttpClient());
        bind(CloseableHttpClient.class).toInstance(buildCloseableHttpClient());
        bind(Producer.class).toInstance(buildKafkaProducer());
        bind(TimeOutConfig.class).toInstance(new TimeOutConfig());
        bind(KafkaProducerWrapperService.class).toInstance(new KafkaProducerWrapperServiceImpl());

//     Initialize Executor Service
        bind(ExecutorServiceImpl.class).in(Singleton.class);
    }

    private Producer buildKafkaProducer() {

        ApplicationConfiguration configuration = ApplicationConfiguration.getInstance();
        Properties props = new Properties();
        props.put("bootstrap.servers", configuration.get("kafka.bootstrap.servers", "localhost:9092"));
        props.put("acks", configuration.get("kafka.producer.acks","all"));
        props.put("compression.type", configuration.get("kafka.producer.compression.type","none"));
        props.put("max.in.flight.requests.per.connection", configuration.getInt("kafka.producer.max.in.flight.requests.per.connection",1));
        props.put("batch.size", configuration.getInt("kafka.producer.batch.size",16384));
        props.put("linger.ms", configuration.getInt("kafka.producer.linger.ms",5));
        props.put("key.serializer", configuration.get("kafka.communication.event.key.serializer","org.apache.kafka.common.serialization.ByteArraySerializer"));
        props.put("value.serializer", configuration.get("kafka.communication.event.value.serializer","org.apache.kafka.common.serialization.ByteArraySerializer"));

        KafkaProducerConfig config = new KafkaProducerConfig(props);
        Producer<byte[], byte[]> producer = new KafkaProducer<>(config);
        return producer;
    }

    private ObjectMapper buildObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_ABSENT);
        return mapper;
    }

    private NFAsyncHttpClient buildAsyncHttpClient() {
        HttpClientConfig config = createAsyncHttpClientConfig();
        CloseableHttpAsyncClient httpClient = new AsyncHttpClientFactory(config).createClient();
        return new NFAsyncHttpClient(httpClient);
    }

    private NFHttpClient buildHttpClient() {
        HttpClientConfig config = createHttpClientConfig();
        HttpClient httpClient = new HttpClientFactory(config).createClient();
        return new NFHttpClient(httpClient);
    }

    private CloseableHttpClient buildCloseableHttpClient() {
        HttpClientConfig config = createHttpClientConfig();
        return new HttpClientFactory(config).createClient();
    }

    private HttpClientConfig createAsyncHttpClientConfig() {
        ApplicationConfiguration configuration = ApplicationConfiguration.getInstance();
        HttpClientConfig config = new HttpClientConfig();

        config.setMaxTotalConnections(configuration.getInt("httpClientConfig.maxConnections", 150));
        config.setMaxConnectionsPerRoute(configuration.getInt("httpClientConfig.maxConnectionsPerRoute", 50));
        config.setConnectionTimeout(configuration.getInt("httpClientConfig.connectionTimeout", 7000));
        config.setSoTimeout(configuration.getInt("httpClientConfig.requestTimeout", 60000));
        config.setSoReuseAddress(configuration.getBoolean("httpClientConfig.soReuseAddress", true));
        config.setSoLinger(configuration.getInt("httpClientConfig.soLinger", 0));
        config.setSoKeepAlive(configuration.getBoolean("httpClientConfig.keepAlive", false));
        config.setTcpNoDelay(configuration.getBoolean("httpClientConfig.tcpNoDelay", false));
        return config;
    }

    private HttpClientConfig createHttpClientConfig() {
        ApplicationConfiguration configuration = ApplicationConfiguration.getInstance();
        HttpClientConfig config = new HttpClientConfig();

        config.setMaxTotalConnections(configuration.getInt("httpClientConfig.maxConnections", 150));
        config.setMaxConnectionsPerRoute(configuration.getInt("httpClientConfig.maxConnectionsPerHost", 50));
        config.setConnectionTimeout(configuration.getInt("httpClientConfig.connectionTimeout", 7000));
        config.setSoTimeout(configuration.getInt("httpClientConfig.requestTimeout", 60000));
        config.setSoReuseAddress(configuration.getBoolean("httpClientConfig.soReuseAddress", true));
        config.setSoLinger(configuration.getInt("httpClientConfig.soLinger", 0));
        config.setSoKeepAlive(configuration.getBoolean("httpClientConfig.keepAlive", false));
        config.setTcpNoDelay(configuration.getBoolean("httpClientConfig.tcpNoDelay", false));
        return config;
    }
}
