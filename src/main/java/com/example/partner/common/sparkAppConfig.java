package com.example.partner.common;

import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.SparkSession;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Autowired;

@Configuration
public class sparkAppConfig {

    @Autowired
    private SparkManager sparkManager;

    @Bean
    public SparkSession sparkSession() {
        return sparkManager.getSparkSession();
    }

    @Bean
    public JavaSparkContext javaSparkContext() {
        return sparkManager.getJavaSparkContext();
    }
}
