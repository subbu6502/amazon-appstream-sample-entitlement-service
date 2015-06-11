package com.amazonaws.sample.entitlement;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.web.SpringBootServletInitializer;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class EntitlementApplication extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(EntitlementApplication.class);
    }

    public static void main(String[] args) {
        new EntitlementApplication().configure(
            new SpringApplicationBuilder(EntitlementApplication.class)).run(args);
    }

}
