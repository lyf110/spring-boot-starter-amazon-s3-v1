package com.amazon.s3.v1.auto.config;

import com.amazon.s3.v1.config.AmazonS3V1Properties;
import com.amazon.s3.v1.template.AmazonS3V1Template;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author liuyangfang
 * @description
 * @since 2023/5/31 19:41:03
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(AmazonS3V1Properties.class)
public class AmazonS3ClientV1AutoConfiguration {

    @ConditionalOnMissingBean(AmazonS3V1Template.class)
    @Bean(name = "amazonS3Template")
    public AmazonS3V1Template amazonS3Template() {
        return new AmazonS3V1Template();
    }
}
