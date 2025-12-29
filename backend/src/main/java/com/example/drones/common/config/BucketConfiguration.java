package com.example.drones.common.config;

import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "gcs")
public class BucketConfiguration {
    private String bucketName;
    private String subdirectory;

    @Bean
    public Storage storage() {
        return StorageOptions.getDefaultInstance().getService();
    }
}
