package cn.pzhdv.blog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class BlogAdminApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(BlogAdminApiApplication.class, args);
    }

}
