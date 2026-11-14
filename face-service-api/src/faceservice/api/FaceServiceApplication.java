package faceservice.api;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication(scanBasePackages = "faceservice")
@MapperScan("faceservice.infrastructure.persistence.mapper")
@ConfigurationPropertiesScan("faceservice")
public class FaceServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FaceServiceApplication.class, args);
    }
}
