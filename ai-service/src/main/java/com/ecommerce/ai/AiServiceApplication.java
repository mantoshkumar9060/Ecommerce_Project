package com.ecommerce.ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.ecommerce.ai.config.AiProperties;
import com.ecommerce.ai.config.QdrantProperties;

@SpringBootApplication
@EnableFeignClients
@EnableConfigurationProperties({AiProperties.class, QdrantProperties.class})
public class AiServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AiServiceApplication.class, args);
    }
}
