package com.feat.final_year_Major_project;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.feat")
@EnableJpaRepositories(basePackages = "com.feat.repository")

@EntityScan(basePackages = "com.feat.entity")

public class FinalYearMajorProjectApplication {

	public static void main(String[] args) {
		SpringApplication.run(FinalYearMajorProjectApplication.class, args);
	}

}
