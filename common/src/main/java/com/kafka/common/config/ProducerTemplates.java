package com.kafka.common.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.HashMap;

/**
 * @Author: chunliu
 * @Date: 2020/11/16 05:57
 */

@Data
@Getter
@Setter
@AllArgsConstructor
public class ProducerTemplates {
    HashMap<String, KafkaTemplate> producerTemplates;
}
