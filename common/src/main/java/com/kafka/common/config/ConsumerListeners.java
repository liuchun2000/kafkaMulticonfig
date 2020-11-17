package com.kafka.common.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import java.util.HashMap;

/**
 * @Author: chunliu
 * @Date: 2020/11/16 05:59
 */

@Data
@Getter
@Setter
@AllArgsConstructor
public class ConsumerListeners {
    HashMap<String, KafkaConsumer<String, String>> consumerListeners;
    //HashMap<String, KafkaMessageListenerContainer<String, String>> consumerListeners;
}
