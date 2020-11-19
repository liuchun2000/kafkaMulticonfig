package com.kafka.producer;

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
 * @Date: 2020/11/12 14:12
 */
@SpringBootApplication(exclude = {KafkaAutoConfiguration.class})
@Import({KafkaConfig.class})
@ComponentScan(basePackages = {"com.kafka.common","com.kafka.producer"})
public class ProducerApplication {
    /**
     * 日志
     */
    private static final Logger LOG = LoggerFactory.getLogger(ProducerApplication.class);

    public static void main(String[] args) {
        LOG.info("********************************************");
        LOG.info("******* starting ProducerApplication *******");
        LOG.info("********************************************");
        SpringApplication.run(ProducerApplication.class, args);
    }
}
