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

package farm.nurture.communication.engine.kafka;

import co.elastic.apm.api.CaptureTransaction;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import farm.nurture.infra.util.StringUtils;
import farm.nurture.kafka.Event;
import farm.nurture.kafka.Producer;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.header.internals.RecordHeaders;


@Slf4j
@Singleton
public class KafkaProducerWrapperServiceImpl implements KafkaProducerWrapperService {

    @Inject
    private Producer producer;

    @CaptureTransaction
    @Override
    public void pushByteArrayMessage(byte[] message, String topic, String partitionKey, Integer retryCount) {

        RecordHeaders headers = new RecordHeaders();
        headers.add("serialization", topic.getBytes());
        headers.add("retry", String.valueOf(retryCount).getBytes());
        if (!StringUtils.isEmpty(partitionKey)) {
            headers.add("partitionKey", partitionKey.getBytes());
        }

        Long currentTime = System.currentTimeMillis();

        log.info("{} partitionKey: {}", topic, partitionKey);

        log.info("{} retry count: {}", topic, retryCount);

        log.info("getProducerInstance: " + producer.toString());

        producer.send(topic, new Event<>(StringUtils.isNonEmpty(partitionKey) ? partitionKey.getBytes() : null, message, currentTime, headers));

        log.info("KafkaProducerWrapperServiceImpl Pushed data to kafka queue: {} with partitionKey: {}", topic, partitionKey);
    }
}
