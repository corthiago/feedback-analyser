package com.thiago.feedback_analyser.service;

import com.thiago.feedback_analyser.client.GeminiInteractionsClient;
import com.thiago.feedback_analyser.client.dto.GenerationConfig;
import com.thiago.feedback_analyser.client.dto.InteractionRequest;
import com.thiago.feedback_analyser.client.dto.InteractionResponse;
import com.thiago.feedback_analyser.client.dto.ResponseFormat;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for interacting with Google's Gemini AI via the Interactions Feign client
 */
@Service
public class GeminiService {

    private static final Logger log = LoggerFactory.getLogger(GeminiService.class);

    @Value("${gemini.api.model}")
    private String model;

    @Value("${gemini.generation.temperature:0.7}")
    private Double temperature;

    @Value("${gemini.generation.max-output-tokens:1024}")
    private Integer maxOutputTokens;

    private final GeminiInteractionsClient geminiClient;

    public GeminiService(GeminiInteractionsClient geminiClient) {
        this.geminiClient = geminiClient;
    }

    /**
     * Sends a prompt to Gemini and returns the freeform text response
     */
    public String generateContent(String prompt) {
        return generate(prompt, null);
    }

    /**
     * Sends a prompt to Gemini constrained to return JSON matching the given schema, and
     * returns the raw JSON response text (ready to be deserialized by the caller).
     */
    public String generateJson(String prompt, Map<String, Object> jsonSchema) {
        ResponseFormat responseFormat = new ResponseFormat();
        responseFormat.setType("text");
        responseFormat.setMimeType("application/json");
        responseFormat.setSchema(jsonSchema);
        return generate(prompt, responseFormat);
    }

    private String generate(String prompt, ResponseFormat responseFormat) {
        try {
            GenerationConfig generationConfig = new GenerationConfig();
            generationConfig.setTemperature(temperature);
            generationConfig.setMaxOutputTokens(maxOutputTokens);

            InteractionRequest request = new InteractionRequest();
            request.setModel(model);
            request.setInput(prompt);
            request.setGenerationConfig(generationConfig);
            request.setResponseFormat(responseFormat);

            InteractionResponse response = geminiClient.createInteraction(request);

            if (response != null && response.getErrors() != null && !response.getErrors().isEmpty()) {
                String errorSummary = response.getErrors().stream()
                        .map(e -> e.getCode() + ": " + e.getMessage())
                        .collect(Collectors.joining("; "));
                log.error("Gemini API returned errors: {}", errorSummary);
                return "Error: " + errorSummary;
            }

            if (response != null && response.getOutputText() != null) {
                return response.getOutputText();
            }

            return "No response from Gemini";

        } catch (FeignException e) {
            log.error("Error calling Gemini API: status={}, message={}", e.status(), e.getMessage(), e);
            return "Error: " + e.getMessage();
        } catch (Exception e) {
            log.error("Unexpected error calling Gemini API", e);
            return "Error: " + e.getMessage();
        }
    }
}
