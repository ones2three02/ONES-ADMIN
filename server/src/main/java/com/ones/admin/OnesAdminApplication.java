package com.ones.admin;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.ones.admin.system.mapper")
public class OnesAdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(OnesAdminApplication.class, args);
    }
}
