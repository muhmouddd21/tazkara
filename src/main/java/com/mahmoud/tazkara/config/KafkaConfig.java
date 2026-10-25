package com.mahmoud.tazkara.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration 
public class KafkaConfig {
    public static final String TICKET_EVENTS_TOPIC = "tazkara.ticket-events";

    @Bean NewTopic ticketEventsTopic() {
        return TopicBuilder.name(TICKET_EVENTS_TOPIC).partitions(3).replicas(1).build();
    }

    @Bean DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<Object, Object> template) {
        return new DefaultErrorHandler(
            new DeadLetterPublishingRecoverer(template),   
            new FixedBackOff(1000L, 2));                  
    }
    @Bean NewTopic ticketEventsDtl(){
        return TopicBuilder.name(TICKET_EVENTS_TOPIC + "-dtl").partitions(3).replicas(1).build();
    }
}
