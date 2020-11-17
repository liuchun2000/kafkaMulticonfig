package com.kafka.producer.service;

import com.alibaba.fastjson.JSONObject;
import com.kafka.common.config.ProducerTemplates;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class KafkaProducerService {

    @Resource
    private ProducerTemplates producerTemplates ;

    /**
     *  发送消息
     */
    public void send(JSONObject data,String topic) {
        // do something
        producerTemplates.getProducerTemplates().get(topic).send(topic,data.toString(),data.toString());
    }
}
