package com.shuai.polymarket.bot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PolymarketBotApplication {
    public static void main(String[] args){
        SpringApplication.run(PolymarketBotApplication.class,args);
    }
}
