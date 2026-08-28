package com.bibei;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.bibei.mapper")
public class BibeiApplication {
    public static void main(String[] args) {
        SpringApplication.run(BibeiApplication.class, args);
    }
}
