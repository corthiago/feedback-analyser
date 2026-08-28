package com.thiago.feedback_analyser;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = "gemini.api.key=test-key")
class FeedbackAnalyserApplicationTests {

	@Test
	void contextLoads() {
	}

}
