package com.careerforge.backend.demo;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.demo")
public record DemoProperties(
        boolean mode,
        String userEmail,
        String userPassword
) {
    public DemoProperties {
        if (userEmail == null) userEmail = "demo@careerforge.dev";
        if (userPassword == null) userPassword = "Demo1234!";
    }
}
