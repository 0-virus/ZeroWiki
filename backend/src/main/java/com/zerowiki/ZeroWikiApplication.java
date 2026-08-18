package com.zerowiki;

import com.zerowiki.config.AuthProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(AuthProperties.class)
public class ZeroWikiApplication {
    public static void main(String[] args) {
        SpringApplication.run(ZeroWikiApplication.class, args);
    }
}
