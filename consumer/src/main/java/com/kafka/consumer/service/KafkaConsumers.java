package com.kafka.consumer.service;

import com.kafka.common.config.ConsumerListeners;
import com.kafka.common.config.ListenerProperties;
import com.kafka.common.constant.KafkaConst;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.time.Duration;
import java.util.Collections;


@Component
@Slf4j
public class KafkaConsumers implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(KafkaConsumers.class);

    @Resource
    ConsumerListeners consumerListeners;

    @Resource
    ListenerProperties listenerProperties;
    /**
     * 开启消费者线程
     * 异常请自己根据需求自己处理
     */
    @Override
    public void run(String... args) throws Exception {
        // 初始化topic
        consumerListeners.getConsumerListeners().forEach((topic,listener)-> {
            // method one：订阅topic:
            listener.subscribe(Collections.singleton(topic));
            String autoCommit = listenerProperties.getListenerProperties().get(topic).get(KafkaConst.CONSUMER_AUTO_COMMIT);

            // 开启一个消费者线程
            new Thread(() -> {
                while (true) {
                    ConsumerRecords<String, String> records = listener.poll(Duration.ofMillis(100));
                    for (ConsumerRecord<String, String> record : records) {
                        logger.info("key:" + record.key() + "" + ",value:" + record.value());
                        if(autoCommit.equalsIgnoreCase("false")) {
                            listener.commitSync();
                        }
                    }
                }
            }).start();
        });
    }
}


