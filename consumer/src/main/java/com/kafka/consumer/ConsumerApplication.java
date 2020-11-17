package com.kafka.consumer;

import com.kafka.common.config.KafkaConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

/**
 * @Author: chunliu
 * @Date: 2020/11/12 14:13
 */

@SpringBootApplication(exclude = {KafkaAutoConfiguration.class})
@Import(KafkaConfig.class)
@ComponentScan(basePackages = {"com.kafka.common","com.kafka.consumer"})
public class ConsumerApplication {
    /**
     * 日志
     */
    private static final Logger LOG = LoggerFactory.getLogger(ConsumerApplication.class);


    public static void main(String[] args) {
        LOG.info("********************************************");
        LOG.info("******* starting ConsumerApplication *******");
        LOG.info("********************************************");
        SpringApplication.run(ConsumerApplication.class, args);
    }
}
