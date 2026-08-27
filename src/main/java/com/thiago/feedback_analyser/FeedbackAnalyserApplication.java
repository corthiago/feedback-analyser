package com.thiago.feedback_analyser;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class FeedbackAnalyserApplication {

	public static void main(String[] args) {
		SpringApplication.run(FeedbackAnalyserApplication.class, args);
	}

}
