package com.daiend.muriox;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class MyApplication {

     static void main(String[] args) {
        SpringApplication.run(MyApplication.class, args);
    }
}
