package com.kafka.common.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

/**
 * @Author: chunliu
 * @Date: 2020/11/16 16:33
 */

@Data
@Getter
@Setter
@AllArgsConstructor
public class ListenerProperties {
    HashMap<String, Map<String, String>> ListenerProperties;
}
