package com.shuai.polymarket.listener;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PolymarketListenerApplication {
    public static void main(String[] args){
        SpringApplication.run(PolymarketListenerApplication.class,args);
    }
}
