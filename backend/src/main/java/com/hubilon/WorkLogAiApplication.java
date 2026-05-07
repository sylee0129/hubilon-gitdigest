package com.hubilon;

import com.hubilon.auth.example.AuthController;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

@SpringBootApplication
@ConfigurationPropertiesScan
@ComponentScan(
        basePackages = "com.hubilon",
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = AuthController.class
        )
)
public class WorkLogAiApplication {

    public static void main(String[] args) {
        SpringApplication.run(WorkLogAiApplication.class, args);
    }
}
