package com.sunt.NetworkTester.Configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackages = "com.sunt.NetworkTester.Repository")
@EntityScan(basePackages = "com.sunt.NetworkTester.Entity")
public class H2Config {

}

