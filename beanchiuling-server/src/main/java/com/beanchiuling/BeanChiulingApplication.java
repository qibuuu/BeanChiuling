package com.beanchiuling;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 🫘 BeanChiuling - Discord Clone Application
 *
 * Entry point của toàn bộ ứng dụng Spring Boot.
 *
 * @SpringBootApplication là một annotation tổng hợp gồm:
 *   - @Configuration: Đây là class chứa bean definitions
 *   - @EnableAutoConfiguration: Tự động cấu hình Spring dựa trên dependencies trong pom.xml
 *   - @ComponentScan: Quét tất cả @Component, @Service, @Repository trong package này và sub-packages
 */
@SpringBootApplication
public class BeanChiulingApplication {

    public static void main(String[] args) {
        SpringApplication.run(BeanChiulingApplication.class, args);
    }
}
