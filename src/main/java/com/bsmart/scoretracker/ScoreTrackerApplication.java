package com.bsmart.scoretracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ScoreTrackerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ScoreTrackerApplication.class, args);
    }
}
