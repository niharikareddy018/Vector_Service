package com.company.vectortool.config;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    public static final String DOCUMENT_QUEUE = "document-task-queue";

    @Bean
    public Queue documentQueue() {
        return new Queue(DOCUMENT_QUEUE, true);
    }
}
