package com.canalesdigitales.contacto.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class HttpClientConfig {

    @Bean("identityRestTemplate")
    public RestTemplate identityRestTemplate(
            RestTemplateBuilder builder,
            @Value("${contacto.integracion.identity.connect-timeout-ms:2000}") long connectTimeout,
            @Value("${contacto.integracion.identity.read-timeout-ms:60000}") long readTimeout) {
        return builder
                .setConnectTimeout(Duration.ofMillis(connectTimeout))
                .setReadTimeout(Duration.ofMillis(readTimeout))
                .build();
    }

    @Bean("coreRestTemplate")
    @Primary
    public RestTemplate coreRestTemplate(
            RestTemplateBuilder builder,
            @Value("${contacto.integracion.customer-data.connect-timeout-ms:2000}") long connectTimeout,
            @Value("${contacto.integracion.customer-data.read-timeout-ms:8000}") long readTimeout) {
        return builder
                .setConnectTimeout(Duration.ofMillis(connectTimeout))
                .setReadTimeout(Duration.ofMillis(readTimeout))
                .build();
    }
}
