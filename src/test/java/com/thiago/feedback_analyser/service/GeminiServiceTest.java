package com.thiago.feedback_analyser.service;

import com.thiago.feedback_analyser.client.GeminiInteractionsClient;
import com.thiago.feedback_analyser.client.dto.InteractionError;
import com.thiago.feedback_analyser.client.dto.InteractionRequest;
import com.thiago.feedback_analyser.client.dto.InteractionResponse;
import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class GeminiServiceTest {

    private GeminiInteractionsClient geminiClient;
    private GeminiService geminiService;

    @BeforeEach
    void setUp() {
        geminiClient = Mockito.mock(GeminiInteractionsClient.class);
        geminiService = new GeminiService(geminiClient);
        ReflectionTestUtils.setField(geminiService, "model", "gemini-3.7-flash");
        ReflectionTestUtils.setField(geminiService, "temperature", 0.7);
        ReflectionTestUtils.setField(geminiService, "maxOutputTokens", 1024);
    }

    @Test
    void returnsOutputTextOnSuccess() {
        InteractionResponse response = new InteractionResponse();
        response.setOutputText("Hello, World!");
        when(geminiClient.createInteraction(any())).thenReturn(response);

        String result = geminiService.generateContent("Say hello");

        assertThat(result).isEqualTo("Hello, World!");

        ArgumentCaptor<InteractionRequest> captor = ArgumentCaptor.forClass(InteractionRequest.class);
        Mockito.verify(geminiClient).createInteraction(captor.capture());
        InteractionRequest sentRequest = captor.getValue();
        assertThat(sentRequest.getModel()).isEqualTo("gemini-3.7-flash");
        assertThat(sentRequest.getInput()).isEqualTo("Say hello");
        assertThat(sentRequest.getGenerationConfig().getTemperature()).isEqualTo(0.7);
        assertThat(sentRequest.getGenerationConfig().getMaxOutputTokens()).isEqualTo(1024);
    }

    @Test
    void returnsErrorStringWhenResponseContainsErrors() {
        InteractionError error = new InteractionError();
        error.setCode("INVALID_ARGUMENT");
        error.setMessage("prompt too long");

        InteractionResponse response = new InteractionResponse();
        response.setErrors(List.of(error));
        when(geminiClient.createInteraction(any())).thenReturn(response);

        String result = geminiService.generateContent("Say hello");

        assertThat(result).startsWith("Error: ");
        assertThat(result).contains("INVALID_ARGUMENT").contains("prompt too long");
    }

    @Test
    void returnsErrorStringWhenClientThrowsFeignException() {
        when(geminiClient.createInteraction(any()))
                .thenThrow(Mockito.mock(FeignException.class));

        String result = geminiService.generateContent("Say hello");

        assertThat(result).startsWith("Error: ");
    }

    @Test
    void generateJsonSetsResponseFormatWithGivenSchema() {
        InteractionResponse response = new InteractionResponse();
        response.setOutputText("{\"sentiment\":\"Positive\"}");
        when(geminiClient.createInteraction(any())).thenReturn(response);

        Map<String, Object> schema = Map.of(
                "type", "object",
                "properties", Map.of("sentiment", Map.of("type", "string"))
        );

        String result = geminiService.generateJson("Analyze this", schema);

        assertThat(result).isEqualTo("{\"sentiment\":\"Positive\"}");

        ArgumentCaptor<InteractionRequest> captor = ArgumentCaptor.forClass(InteractionRequest.class);
        Mockito.verify(geminiClient).createInteraction(captor.capture());
        InteractionRequest sentRequest = captor.getValue();
        assertThat(sentRequest.getResponseFormat()).isNotNull();
        assertThat(sentRequest.getResponseFormat().getType()).isEqualTo("text");
        assertThat(sentRequest.getResponseFormat().getMimeType()).isEqualTo("application/json");
        assertThat(sentRequest.getResponseFormat().getSchema()).isEqualTo(schema);
    }
}
