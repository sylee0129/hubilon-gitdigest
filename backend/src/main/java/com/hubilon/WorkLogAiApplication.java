package com.hubilon;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class WorkLogAiApplication {

    public static void main(String[] args) {
        SpringApplication.run(WorkLogAiApplication.class, args);
    }
}
