package com.example.partner.common;

import lombok.extern.slf4j.Slf4j;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.SparkSession;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;

@Slf4j
@Component
public class SparkManager {
    
    private SparkSession sparkSession;
    private JavaSparkContext javaSparkContext;
    private boolean isInitialized = false;

    /**
     * 初始化 Spark
     */
    public synchronized void initializeSpark() {
        if (!isInitialized) {
            log.info("正在初始化 Spark...");
            sparkSession = SparkSession.builder()
                    .appName("recommend")
                    .master("local[*]")
                    .getOrCreate();
            javaSparkContext = new JavaSparkContext(sparkSession.sparkContext());
            isInitialized = true;
            log.info("Spark 初始化完成");
        }
    }

    /**
     * 获取 SparkSession
     */
    public SparkSession getSparkSession() {
        if (!isInitialized) {
            initializeSpark();
        }
        return sparkSession;
    }

    /**
     * 获取 JavaSparkContext
     */
    public JavaSparkContext getJavaSparkContext() {
        if (!isInitialized) {
            initializeSpark();
        }
        return javaSparkContext;
    }

    /**
     * 关闭 Spark
     */
    public synchronized void closeSpark() {
        if (isInitialized) {
            log.info("正在关闭 Spark...");
            if (javaSparkContext != null) {
                javaSparkContext.close();
                javaSparkContext = null;
            }
            if (sparkSession != null) {
                sparkSession.close();
                sparkSession = null;
            }
            isInitialized = false;
            log.info("Spark 已关闭");
        }
    }

    /**
     * 在应用关闭时自动关闭 Spark
     */
    @PreDestroy
    public void onApplicationShutdown() {
        closeSpark();
    }
} 