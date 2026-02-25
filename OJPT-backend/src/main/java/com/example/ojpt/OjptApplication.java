package com.example.ojpt;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.example.ojpt.mapper")
public class OjptApplication {

    public static void main(String[] args) {
        SpringApplication.run(OjptApplication.class, args);
    }

}
