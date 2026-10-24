package com.mahmoud.tazkara.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration 
public class KafkaConfig {
    public static final String TICKET_EVENTS_TOPIC = "tazkara.ticket-events";

    @Bean NewTopic ticketEventsTopic() {
        return TopicBuilder.name(TICKET_EVENTS_TOPIC).partitions(3).replicas(1).build();
    }
}
