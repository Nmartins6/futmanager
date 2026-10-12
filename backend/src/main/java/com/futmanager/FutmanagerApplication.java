package com.futmanager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
@EnableScheduling
public class FutmanagerApplication {

    public static void main(String[] args) {
        // Garantir UTC como padrão de JVM para armazenamento de instantes de auditoria
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        SpringApplication.run(FutmanagerApplication.class, args);
    }
}
