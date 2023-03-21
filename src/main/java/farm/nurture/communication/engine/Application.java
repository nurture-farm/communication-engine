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

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import com.google.inject.Guice;
import com.google.inject.Injector;
import farm.nurture.communication.engine.cache.LanguageCache;
import farm.nurture.communication.engine.cache.MobileAppDetailsCache;
import farm.nurture.communication.engine.cache.TemplateCache;
import farm.nurture.communication.engine.event.ActorAppTokenEventHandler;
import farm.nurture.communication.engine.event.ActorCommunicationDetailsEventHandler;
import farm.nurture.communication.engine.event.CommunicationEventHandler;
import farm.nurture.communication.engine.grpc.CommunicationEngine;
import farm.nurture.communication.engine.grpc.GrpcService;
import farm.nurture.communication.engine.resource.CommunicationServiceCallbackResource;
import farm.nurture.communication.engine.resource.MissCallResource;
import farm.nurture.communication.engine.resource.WhatsAppResource;
import farm.nurture.communication.engine.utils.ExecutorServiceImpl;
import farm.nurture.communication.engine.utils.VendorLoadBalancer;
import farm.nurture.communication.engine.vendor.GupShupVendor;
import farm.nurture.communication.engine.vendor.KarixVendor;
import farm.nurture.infra.metrics.HealthInfoServerFactory;
import farm.nurture.infra.metrics.prometheus.ServerStatus;
import farm.nurture.infra.util.ApplicationConfiguration;
import farm.nurture.infra.util.StringUtils;
import farm.nurture.kafka.Consumer;
import farm.nurture.kafka.config.KafkaConsumerConfig;
import farm.nurture.kafka.impl.KafkaConsumer;
import farm.nurture.laminar.core.io.sql.dao.DbConfig;
import farm.nurture.laminar.core.io.sql.dao.PoolFactory;
import farm.nurture.util.http.NFException;
import io.grpc.ServerBuilder;
import io.netty.channel.Channel;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.jersey.netty.httpserver.NettyHttpContainerProvider;
import org.glassfish.jersey.server.ResourceConfig;

import java.net.URI;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

@Slf4j
public class Application {

    public static void main(String[] args) throws Exception {
        initializeDatabase();

        Injector injector = Guice.createInjector(new DIModule());
        startNettyServer(injector);
        initializeLoadBalancer(injector);
        initializeInMemoryCache(injector);
        startPrometheusServer();
        startKafkaConsumers(injector);
        startTemporalWorker(injector);
        startExecutorService(injector);
        startGrpcServer(injector);
    }

    private static void startExecutorService(Injector injector) {
        injector.getInstance(ExecutorServiceImpl.class).init();
    }

    private static JacksonJaxbJsonProvider getJacksonProvider() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.setSerializationInclusion(JsonInclude.Include.ALWAYS);
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);

        JacksonJaxbJsonProvider provider = new JacksonJaxbJsonProvider();
        provider.setMapper(mapper);
        return provider;
    }

    private static void startNettyServer(Injector injector) {
        ApplicationConfiguration appConfig = ApplicationConfiguration.getInstance();
        int serverPort = appConfig.getInt("server.port", 8000);
        URI uri = URI.create("http://localhost:" + serverPort + "/");
        MissCallResource resource = injector.getInstance(MissCallResource.class);
        WhatsAppResource resource1 = injector.getInstance(WhatsAppResource.class);
        CommunicationServiceCallbackResource resource2 = injector.getInstance(CommunicationServiceCallbackResource.class);

        ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig.register(getJacksonProvider()).register(resource)
                .register(resource1)
                .register(resource2);

        final Channel server = NettyHttpContainerProvider.createHttp2Server(uri, resourceConfig, null);

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                server.close();
            }
        }));
    }


    private static void initializeDatabase() {
        log.info("Initializing database");
        ApplicationConfiguration appConfig = ApplicationConfiguration.getInstance();
        DbConfig dbConfig = new DbConfig();
        dbConfig.connectionUrl = appConfig.get("db.connection.url");
        dbConfig.login = appConfig.get("db.username");
        dbConfig.password = appConfig.get("db.password");
        dbConfig.driverClass = appConfig.get("db.driver.class", "com.mysql.cj.jdbc.Driver");
        dbConfig.poolName = appConfig.get("db.connection.pool.name", "communication_rw");
        dbConfig.idleConnections = appConfig.getInt("db.idle.connections", 2);
        dbConfig.maxConnections = appConfig.getInt("db.max.connections", 10);
        dbConfig.incrementBy = appConfig.getInt("db.connection.increment.by", 2);
        dbConfig.healthCheckDurationMillis = appConfig.getInt("db.connection.health.check.duration.ms", 900000);
        dbConfig.testConnectionOnBorrow = true;

        PoolFactory.getInstance().setup(dbConfig);
    }

    private static void startKafkaConsumers(Injector injector) {
        log.info("Starting kafka consumers");
        String groupId = System.getenv("kafka.consumer.group.id");
        if(StringUtils.isEmpty(groupId)) {
            groupId = "communication_engine_group";
        }
        startActorCommDetailsEventConsumer(injector, groupId);
        startActorAppTokenEventConsumer(injector, groupId);
        startSendCommunicationEventConsumer(injector, groupId);
    }

    private static void startSendCommunicationEventConsumer(Injector injector, String groupId) {
        ApplicationConfiguration appConfig = ApplicationConfiguration.getInstance();
        Properties props = new Properties();
        props.put("bootstrap.servers", appConfig.get("kafka.bootstrap.servers", "localhost:9092"));
        props.put("group.id", groupId);
        props.put("enable.auto.commit", appConfig.getBoolean("kafka.enable.auto.commit", false));
        props.put("auto.offset.reset", appConfig.get("kafka.auto.offset.reset", "latest"));
        props.put("max.poll.records", appConfig.getInt("kafka.max.poll.records", 5));
        props.put("key.deserializer", appConfig.get("kafka.communication.event.key.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer"));
        props.put("value.deserializer", appConfig.get("kafka.communication.event.value.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer"));

        CommunicationEventHandler handler = injector.getInstance(CommunicationEventHandler.class);
        ObjectMapper mapper = injector.getInstance(ObjectMapper.class);
        String kafkaConsumerConfig = System.getenv("consumer_config");
        Map<String, Integer> consumerTopics = Collections.EMPTY_MAP;
        try {
            if (StringUtils.isNonEmpty(kafkaConsumerConfig)) {
                consumerTopics = mapper.readValue(kafkaConsumerConfig, Map.class);
            }
        } catch (Exception e) {
            log.error("Unable to process consumer config from env variable : consumer_config", e);
        }

        for (Map.Entry<String, Integer> entry : consumerTopics.entrySet()) {
            log.info("KafkaConsumerConfig topic name : {} , consumer count : {}", entry.getKey(), entry.getValue());
            KafkaConsumerConfig config = new KafkaConsumerConfig(Arrays.asList(entry.getKey()),
                    true, Duration.ofMillis(Long.MAX_VALUE), props);
            int noOfConsumers = entry.getValue();
            for (int i = 0; i < noOfConsumers; i++) {
                Consumer<byte[], byte[]> consumer = new KafkaConsumer<>(config, handler);
                consumer.start();
            }
        }
    }

    private static void startActorAppTokenEventConsumer(Injector injector, String groupId) {
        ApplicationConfiguration appConfig = ApplicationConfiguration.getInstance();
        Properties props = new Properties();
        props.put("bootstrap.servers", appConfig.get("kafka.bootstrap.servers", "localhost:9092"));
        props.put("group.id", groupId);
        props.put("enable.auto.commit", appConfig.getBoolean("kafka.enable.auto.commit", false));
        props.put("auto.offset.reset", appConfig.get("kafka.auto.offset.reset", "latest"));
        props.put("max.poll.records", appConfig.getInt("kafka.max.poll.records", 5));
        props.put("key.deserializer", appConfig.get("kafka.actor.app.token.event.key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer"));
        props.put("value.deserializer", appConfig.get("kafka.actor.app.token.event.value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer"));

        ActorAppTokenEventHandler handler = injector.getInstance(ActorAppTokenEventHandler.class);
        KafkaConsumerConfig config = new KafkaConsumerConfig(Arrays.asList(appConfig.get("kafka.actor.app.token.event.topic", "actor_app_token_events")),
                true, Duration.ofMillis(Long.MAX_VALUE), props);

        int noOfConsumers = appConfig.getInt("kafka.actor.app.token.event.consumers", 1);
        for(int i = 0; i < noOfConsumers; i++) {
            Consumer<String, String> consumer = new KafkaConsumer<>(config, handler);
            consumer.start();
        }
    }

    private static void startActorCommDetailsEventConsumer(Injector injector, String groupId) {
        ApplicationConfiguration appConfig = ApplicationConfiguration.getInstance();
        Properties props = new Properties();
        props.put("bootstrap.servers", appConfig.get("kafka.bootstrap.servers", "localhost:9092"));
        props.put("group.id", groupId);
        props.put("enable.auto.commit", appConfig.getBoolean("kafka.enable.auto.commit", false));
        props.put("auto.offset.reset", appConfig.get("kafka.auto.offset.reset", "latest"));
        props.put("max.poll.records", appConfig.getInt("kafka.max.poll.records", 5));
        props.put("key.deserializer", appConfig.get("kafka.actor.comm.details.event.key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer"));
        props.put("value.deserializer", appConfig.get("kafka.actor.comm.details.event.value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer"));

        ActorCommunicationDetailsEventHandler handler = injector.getInstance(ActorCommunicationDetailsEventHandler.class);
        KafkaConsumerConfig config = new KafkaConsumerConfig(Arrays.asList(appConfig.get("kafka.actor.comm.details.event.topic", "actor_comm_details_event")),
                true, Duration.ofMillis(Long.MAX_VALUE), props);

        int noOfConsumers = appConfig.getInt("kafka.actor.comm.details.event.consumers", 1);
        for(int i = 0; i < noOfConsumers; i++) {
            Consumer<String, String> consumer = new KafkaConsumer<>(config, handler);
            consumer.start();
        }
    }

    private static void initializeInMemoryCache(Injector injector) {
        log.info("Initializing In-Memory LanguageCache");
        injector.getInstance(LanguageCache.class).init();

        log.info("Initializing In-Memory MobileAppDetailsCache");
        injector.getInstance(MobileAppDetailsCache.class).init();

        log.info("Initializing In-Memory TemplateCache");
        injector.getInstance(TemplateCache.class).init();
    }

    private static void startPrometheusServer() {
        ApplicationConfiguration config = ApplicationConfiguration.getInstance();
        log.info("Starting communication engine prometheus server");
        ServerStatus.getInstance().state = ServerStatus.State.STARTING;
        try {
            int port = config.getInt("server.prometheus.port", 8000);
            HealthInfoServerFactory.start(port, 1, false, TimeUnit.MILLISECONDS, 300, 5);
            log.info("Communication engine prometheus server started");

        } catch (Exception e) {
            log.error("Error in starting Communication engine prometheus server : {}", e.getMessage(), e);
            throw new NFException(e);
        }
    }

    private static void startGrpcServer(Injector injector) throws Exception {

        ApplicationConfiguration config = ApplicationConfiguration.getInstance();
        GrpcService grpcService = injector.getInstance(GrpcService.class);
        int port = config.getInt("grpcServer.port", 8010);
        io.grpc.Server server = ServerBuilder.forPort(port).addService(grpcService).build().start();
        log.info("Communication Engine grpc server started, listening on " + port);
        server.awaitTermination();
    }

    public static void startTemporalWorker(Injector injector) {
        ApplicationConfiguration config = ApplicationConfiguration.getInstance();

        // gRPC stubs wrapper that talks to the local docker instance of temporal service.
        String temporalAddress = config.get("temporal.address", "127.0.0.1:7233");
        WorkflowServiceStubsOptions options = WorkflowServiceStubsOptions.newBuilder().setTarget(temporalAddress).build();
        WorkflowServiceStubs service = WorkflowServiceStubs.newInstance(options);

        // client that can be used to start and signal workflows
        String namespace = config.get("temporal.namespace", "default");

        WorkflowClientOptions clientOptions = WorkflowClientOptions.newBuilder()
                .setNamespace(namespace).build();
        WorkflowClient client = WorkflowClient.newInstance(service, clientOptions);

        // worker factory that can be used to create workers for specific task lists
        WorkerFactory factory = WorkerFactory.newInstance(client);

        // Worker that listens on a task list and hosts activity implementations.
        Worker worker = factory.newWorker(config.get("temporal.worker.taskqueue"));
        worker.registerActivitiesImplementations(injector.getInstance(CommunicationEngine.class));

        factory.start();
    }

    public static void initializeLoadBalancer(Injector injector){
        injector.getInstance(KarixVendor.class).init();
        injector.getInstance(GupShupVendor.class).init();
        VendorLoadBalancer loadBalancer = injector.getInstance(VendorLoadBalancer.class);
        ObjectMapper mapper = injector.getInstance(ObjectMapper.class);
        String whatsappVendorConfig = System.getenv("whatsapp_vendor_config");
        String smsVendorConfig = System.getenv("sms_vendor_config");
        Map<String, Integer>  vendorToWeightMap;

        try {
            if (StringUtils.isNonEmpty(whatsappVendorConfig)) {
                vendorToWeightMap = mapper.readValue(whatsappVendorConfig, Map.class);
                loadBalancer.initializeVendorWeightedMap("WHATSAPP", vendorToWeightMap);
            }
            if(StringUtils.isNonEmpty(smsVendorConfig)){
                vendorToWeightMap = mapper.readValue(smsVendorConfig, Map.class);
                loadBalancer.initializeVendorWeightedMap("SMS", vendorToWeightMap);
            }
            loadBalancer.init();
        } catch (Exception e) {
            log.error("Unable to process Vendor config from env variable:  ", e);
        }

    }

}
