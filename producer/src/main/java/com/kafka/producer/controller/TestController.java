package com.kafka.producer.controller;

import com.alibaba.fastjson.JSONObject;
import com.kafka.producer.service.KafkaProducerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @Author: chunliu
 * @Date: 2020/11/16 04:07
 */

@RestController
@RequestMapping("/test")
public class TestController {

    @Autowired
    KafkaProducerService kafkaProducerService;

    @PostMapping("/send")
    String send(@RequestBody(required = true) JSONObject jsonObject) {
        String topic = jsonObject.getString("topic");
        kafkaProducerService.send(jsonObject,topic);
        return "Send msg "+jsonObject.toString();
    }

    @GetMapping("/2")
    String test() {
        return "ok";
    }
}
