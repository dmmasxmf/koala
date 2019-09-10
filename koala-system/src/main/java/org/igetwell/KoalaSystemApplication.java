package org.igetwell;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.cloud.client.SpringCloudApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

//@EnableKoalaFeign
@EnableFeignClients
@SpringCloudApplication
@MapperScan("org.igetwell.*.mapper")
@ComponentScan(basePackages = {"org.igetwell.**"})
public class KoalaSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(KoalaSystemApplication.class, args);
    }

}
