package com.fairsplit.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.fairsplit")
@EntityScan(basePackages = "com.fairsplit.core.entity")
@EnableJpaRepositories(basePackages = "com.fairsplit.core.repository")
@EnableAsync
@EnableScheduling
public class FairSplitApplication {
    public static void main(String[] args) {
        SpringApplication.run(FairSplitApplication.class, args);
    }
}