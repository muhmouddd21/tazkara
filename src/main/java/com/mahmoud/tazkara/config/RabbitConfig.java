package com.mahmoud.tazkara.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {
    public static final String EXCHANGE = "tazkara.tickets";
    public static final String CONFIRMATIONS_QUEUE = "ticket-confirmations";
    public static final String CANCELLATIONS_QUEUE = "ticket-cancellations";
    public static final String CANCELLATIONS_DLQ = "ticket-cancellations.dlq";
    public static final String CONFIRMATIONS_DLQ = "ticket-confirmations.dlq";
    public static final String PURCHASED_KEY = "ticket.purchased";
    public static final String CANCELLED_KEY ="ticket.cancelled";
    public static final String DLX = "tazkara.dlx";

    @Bean 
    TopicExchange ticketsExchange() { 
        return new TopicExchange(EXCHANGE); 
    }
    @Bean
    DirectExchange deadLetterExchange(){
        return new DirectExchange(DLX);
    }

    @Bean 
    Queue confirmationsDlq() { 
        return QueueBuilder.durable(CONFIRMATIONS_DLQ).build(); 
    }
    
    @Bean 
    Queue cancellationsDlq() { 
        return QueueBuilder.durable(CANCELLATIONS_DLQ).build(); 
    }

    @Bean 
    Queue confirmationsQueue() { 
        return QueueBuilder.durable(CONFIRMATIONS_QUEUE)
        .deadLetterExchange(DLX)
        .deadLetterRoutingKey(CONFIRMATIONS_QUEUE)
        .build(); 
    }
    @Bean 
    Queue cancellationsQueue() { 
        return QueueBuilder.durable(CANCELLATIONS_QUEUE)
        .deadLetterExchange(DLX)
        .deadLetterRoutingKey(CANCELLATIONS_QUEUE)
        .build(); 
    }

    @Bean 
    Binding confirmationsBinding(TopicExchange ticketsExchange) {
        return BindingBuilder.bind(confirmationsQueue()).to(ticketsExchange()).with(PURCHASED_KEY);
    }
    @Bean 
    Binding cancellationBinding(TopicExchange ticketsExchange) {
        return BindingBuilder.bind(cancellationsQueue()).to(ticketsExchange()).with(CANCELLED_KEY);
    }

    @Bean 
    Binding confirmationsDlqBinding() {
        return BindingBuilder.bind(confirmationsDlq()).to(deadLetterExchange()).with(CONFIRMATIONS_QUEUE);
    }

    @Bean 
    Binding cancellationsDlqBinding() {
        return BindingBuilder.bind(cancellationsDlq()).to(deadLetterExchange()).with(CANCELLATIONS_QUEUE);
    }

    @Bean 
    MessageConverter jsonMessageConverter() { 
        return new JacksonJsonMessageConverter(); 
    }
}