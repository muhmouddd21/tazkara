package com.mahmoud.tazkara.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {
    public static final String EXCHANGE = "tazkara.tickets";
    public static final String CONFIRMATIONS_QUEUE = "ticket-confirmations";
    public static final String PURCHASED_KEY = "ticket.purchased";

    @Bean 
    TopicExchange ticketsExchange() { 
        return new TopicExchange(EXCHANGE); 
    }

    @Bean 
    Queue confirmationsQueue() { 
        return QueueBuilder.durable(CONFIRMATIONS_QUEUE).build(); 
    }

    @Bean 
    Binding confirmationsBinding(Queue confirmationsQueue, TopicExchange ticketsExchange) {
        return BindingBuilder.bind(confirmationsQueue).to(ticketsExchange).with(PURCHASED_KEY);
    }

    @Bean 
    MessageConverter jsonMessageConverter() { 
        return new JacksonJsonMessageConverter(); 
    }
}