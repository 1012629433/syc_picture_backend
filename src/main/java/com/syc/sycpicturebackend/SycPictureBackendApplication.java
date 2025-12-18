package com.syc.sycpicturebackend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@MapperScan("com.syc.sycpicturebackend.mapper")
@EnableAspectJAutoProxy(exposeProxy = true)
public class SycPictureBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(SycPictureBackendApplication.class, args);
    }

}
