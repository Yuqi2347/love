package com.campus.love;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CampusLoveApplication {

    public static void main(String[] args) {
        SpringApplication.run(CampusLoveApplication.class, args);
    }
}
