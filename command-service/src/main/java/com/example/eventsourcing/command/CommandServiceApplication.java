package com.example.eventsourcing.command;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
//@EnableJpaRepositories(basePackages = "com.example.eventsourcing.command.infrastructure")
//@EntityScan(basePackages = "com.example.eventsourcing.command.domain")
@EnableAsync
@EnableScheduling
@EnableTransactionManagement
public class CommandServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CommandServiceApplication.class, args);
    }

}

