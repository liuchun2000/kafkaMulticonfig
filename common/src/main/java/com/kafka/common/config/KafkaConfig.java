package com.kafka.common.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * @Author: chunliu
 * @Date: 2020/11/13 10:11
 */
@Configuration
@EnableKafka
public class KafkaConfig implements ImportBeanDefinitionRegistrar, EnvironmentAware {
    private static final Logger logger = LoggerFactory.getLogger(KafkaConfig.class);

    /**
     * 配置上下文（也可以理解为配置文件的获取工具）
     */
    private Environment evn;

    /**
     * 存储kafka templates
     */
    private HashMap<String, KafkaTemplate> producerTemplates = new HashMap<String, KafkaTemplate>();
    private HashMap<String, KafkaConsumer<String, String>> consumerListeners = new HashMap<String, KafkaConsumer<String, String>>();
    //private HashMap<String, KafkaMessageListenerContainer<String, String>> consumerListeners = new HashMap<String, KafkaMessageListenerContainer<String, String>>();
    private HashMap<String, Map<String, String>> ListenerProperties = new HashMap<String, Map<String, String>>();
    private Binder binder;


    /**
     * ImportBeanDefinitionRegistrar接口的实现方法，通过该方法可以按照自己的方式注册bean
     *
     * @param annotationMetadata
     * @param beanDefinitionRegistry
     */
    @Override
    public void registerBeanDefinitions(AnnotationMetadata annotationMetadata, BeanDefinitionRegistry beanDefinitionRegistry) {

        //kafka templates
        String templateNames = evn.getProperty("kafka.names");
        String[] templateNamesArray = null;
        // 获取所有templates name
        if(!StringUtils.isEmpty(templateNames)) {
            templateNamesArray = templateNames.split(",", 0);
        }

        if (templateNames == null || templateNamesArray.length == 0){
            //没有kafka
        }else{
            //多数据组,遍历组
            String topic = null;
            for(int i=0 ; i<templateNamesArray.length; i++){
                Map templateConfigMap = binder.bind("kafka."+templateNamesArray[i], Bindable.mapOf(String.class,String.class)).get();
                //配置template
                String kafkaType = (String) templateConfigMap.get("type");
                //区分生产者和消费者创建template
                topic = (String)templateConfigMap.get("topic");
                if(null == topic){
                    logger.error(templateConfigMap.toString()+" invalid");
                    continue;
                }
                if(kafkaType.equalsIgnoreCase("producer")){
                    //config producer
                    producerTemplates.put(topic, kafkaTemplate(topic,templateConfigMap));
                }else if(kafkaType.equalsIgnoreCase("consumer")){

                    ListenerProperties.put(topic,templateConfigMap);

                    //KafkaMessageListenerContainer<String, String> consumer = listenerFactory(consumerFactory(templateConfigMap),topic);
                    //config consumer
                    KafkaConsumer<String, String> consumer = new KafkaConsumer<String,String>(consumerConfigs(templateConfigMap));
                    consumer.subscribe(Collections.singleton(topic));
                    consumerListeners.put(topic,consumer);
                }
            }
        }
        // bean定义类
        GenericBeanDefinition producers = new GenericBeanDefinition();
        producers.setBeanClass(ProducerTemplates.class);
        MutablePropertyValues producersValues = producers.getPropertyValues();
        producersValues.add("producerTemplates",producerTemplates);

        GenericBeanDefinition consumers = new GenericBeanDefinition();
        consumers.setBeanClass(ConsumerListeners.class);
        MutablePropertyValues consumersValues = consumers.getPropertyValues();
        consumersValues.add("consumerListeners",consumerListeners);

        GenericBeanDefinition factorys = new GenericBeanDefinition();
        factorys.setBeanClass(ListenerProperties.class);
        MutablePropertyValues factorysValues = factorys.getPropertyValues();
        factorysValues.add("ListenerProperties",ListenerProperties);

        BeanDefinition producersDefinition = new RootBeanDefinition(ProducerTemplates.class,null,producersValues);
        BeanDefinition consumersDefinition = new RootBeanDefinition(ConsumerListeners.class,null,consumersValues);
        BeanDefinition factorysDefinition = new RootBeanDefinition(ListenerProperties.class,null,factorysValues);

        // 将该bean注册为 consumerListeners
        beanDefinitionRegistry.registerBeanDefinition("producerTemplates", producersDefinition);

        beanDefinitionRegistry.registerBeanDefinition("consumerListeners", consumersDefinition);

        beanDefinitionRegistry.registerBeanDefinition("ListenerProperties", factorysDefinition);
    }

    /**
     * 工厂配置
     *
     * 关于consumer的主要的封装在ConcurrentKafkaListenerContainerFactory这个里头，
     * 本身的KafkaConsumer是线程不安全的，无法并发操作，这里spring又在包装了一层，
     * 根据配置的spring.kafka.listener.concurrency来生成多个并发的KafkaMessageListenerContainer实例
     */
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, String>> kafkaListenerContainerFactory(Map propMap) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory(propMap));
        factory.setAutoStartup(true);
        //设置并发量，小于或等于Topic的分区数,并且要在consumerFactory设置一次拉取的数量
        factory.setConcurrency(1);
        //设置拉取等待时间(也可间接的理解为延时消费)
        factory.getContainerProperties().setPollTimeout(1500);

        //设置为批量监听
        factory.setBatchListener(true);
        //指定使用此bean工厂的监听方法，消费确认为方式为用户指定aks,可以用下面的配置也可以直接使用enableAutoCommit参数
        //factory.getContainerProperties().setAckMode(AbstractMessageListenerContainer.AckMode.MANUAL_IMMEDIATE);

        //使用过滤器
        //配合RecordFilterStrategy使用，被过滤的信息将被丢弃
        //factory.setAckDiscarded(true);
        //factory.setRecordFilterStrategy(kafkaRecordFilterStrategy);

        return factory;
    }

    public ConsumerFactory<String, String> consumerFactory(Map propMap) {
        return new DefaultKafkaConsumerFactory<>(consumerConfigs(propMap));
    }

    /**
     *  消费者监听器配置
     *
     *  每个KafkaMessageListenerContainer都自己创建一个ListenerConsumer，
     *  然后自己创建一个独立的kafka consumer，每个ListenerConsumer在线程池里头运行，这样来实现并发。
     *
     *  每个ListenerConsumer里头都有一个recordsToProcess队列，从原始的kafka consumer poll出来的记录会放到这个队列里头，
     *  然后有一个ListenerInvoker线程循环超时等待从recordsToProcess取出记录，然后交给应用程序的KafkaListener标注的方法去执行
     */
    KafkaMessageListenerContainer<String, String> listenerFactory(ConsumerFactory<String, String> cf, String topic) {
        // 设置topics
        ContainerProperties containerProperties = new ContainerProperties(topic);
        Properties properties = new Properties();
        properties.putAll(cf.getConfigurationProperties());
        containerProperties.setKafkaConsumerProperties(properties);
        containerProperties.setGroupId((String)cf.getConfigurationProperties().get(ConsumerConfig.GROUP_ID_CONFIG));
        KafkaMessageListenerContainer<String, String> container = new KafkaMessageListenerContainer<>(cf, containerProperties);

        container.setBeanName(topic);
        container.setAutoStartup(true);

        return container;
    }


    /**
     * 消费者基本配置
     */
    private Map<String, Object> consumerConfigs(Map propMap) {
        Map<String, Object> propsMap = new HashMap<>(12);
        propsMap.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, propMap.get("bootstrap-servers"));
        propsMap.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        propsMap.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        propsMap.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, propMap.get("auto-offset-reset"));
        propsMap.put(ConsumerConfig.GROUP_ID_CONFIG, propMap.get("group-id"));
        propsMap.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG,propMap.get("auto-commit-interval"));
        propsMap.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG,propMap.get("enable-auto-commit"));
        propsMap.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG,propMap.get("max-poll-records"));
        propsMap.put(ConsumerConfig.CLIENT_ID_CONFIG,propMap.get("client-id"));
        return propsMap;
    }

    public KafkaTemplate<String, String> kafkaTemplate(String topic,Map prop) {
        KafkaTemplate<String, String> kafkaTemplate = new KafkaTemplate<String, String>(producerConfigs(prop));
        kafkaTemplate.setDefaultTopic(topic);
        return kafkaTemplate;
    }

    /**
     * 生产者基本配置
     */
    private ProducerFactory<String, String> producerConfigs(Map propMap) {
        Map<String, Object> properties = new HashMap<>(12);
        if (!StringUtils.isEmpty(propMap.get("saslJaasConfig"))) {
            // 设置sasl认证的两种方式
            //System.setProperty("java.security.auth.login.config", "classpath:/application.properties:/kafka_client_jaas.conf");
            properties.put(SaslConfigs.SASL_JAAS_CONFIG, propMap.get("saslJaasConfig"));
        }
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,StringSerializer.class);
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,StringSerializer.class);
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, propMap.get("bootstrap-servers"));
        properties.put(ProducerConfig.BATCH_SIZE_CONFIG, propMap.get("batch-size"));
        properties.put(ProducerConfig.LINGER_MS_CONFIG, propMap.get("linger"));
        properties.put(ProducerConfig.BUFFER_MEMORY_CONFIG, propMap.get("buffer-memory"));
        properties.put(ProducerConfig.RETRIES_CONFIG, propMap.get("retries"));
        if (!StringUtils.isEmpty(propMap.get("saslMechanism")) && !StringUtils.isEmpty(propMap.get("securityProtocol"))) {
            //properties.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, propMap.get("securityProtocol"));
            //properties.put(SaslConfigs.SASL_MECHANISM, propMap.get("saslMechanism"));
        }
        return new DefaultKafkaProducerFactory<>(properties);
    }

    /**
     * EnvironmentAware接口的实现方法，通过aware的方式注入，此处是environment对象
     *
     * @param environment
     */
    @Override
    public void setEnvironment(Environment environment) {
        logger.debug("开始注册");
        this.evn = environment;
        // 绑定配置器
        binder = Binder.get(evn);
    }
}
