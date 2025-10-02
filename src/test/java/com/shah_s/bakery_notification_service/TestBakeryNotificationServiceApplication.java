package com.shah_s.bakery_notification_service;

import org.springframework.boot.SpringApplication;

public class TestBakeryNotificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.from(BakeryNotificationServiceApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
