package com.kunlong.dubbo;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.apache.dubbo.config.spring.context.annotation.EnableDubboConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.kunlong.dubbo.pf","com.kunlong","com.kunlong.platform"})

@EnableDubbo
@EnableDubboConfig
public class PfDubboApp {
    public static void main(String[] args) {
        Class[] classes = new Class[]{
                PfDubboApp.class
        };
        SpringApplication.run(classes, args);
    }
}
