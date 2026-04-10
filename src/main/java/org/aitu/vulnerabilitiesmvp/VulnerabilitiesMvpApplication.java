package org.aitu.vulnerabilitiesmvp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class VulnerabilitiesMvpApplication {

    public static void main(String[] args) {
        SpringApplication.run(VulnerabilitiesMvpApplication.class, args);
    }
}
