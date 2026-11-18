package com.mci.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MciWebApplication {

    public static void main(String[] args) {
        // Ensure IPv4 is preferred for stable network connections
        System.setProperty("java.net.preferIPv4Stack", "true");

        SpringApplication.run(MciWebApplication.class, args);
    }
}
