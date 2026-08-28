package com.thiago.feedback_analyser.service;

import com.thiago.feedback_analyser.model.FeedbackAnalysis;
import com.thiago.feedback_analyser.model.FeedbackEntry;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

@Service
public class AnalysisService {

    // Constrains Gemini's structured output to exactly the fields FeedbackAnalysis needs
    private static final Map<String, Object> ANALYSIS_SCHEMA = Map.of(
            "type", "object",
            "properties", Map.of(
                    "sentiment", Map.of(
                            "type", "string",
                            "enum", List.of("Positive", "Neutral", "Negative")),
                    "category", Map.of(
                            "type", "string",
                            "enum", List.of(
                                    "Product Quality", "Customer Service", "Store Experience", "Website/App",
                                    "Delivery", "Price/Value", "Inventory/Stock", "Other"
                            )
                    ),
                    "actionableInsight", Map.of("type", "string")
            ),
            "required", List.of("sentiment", "category", "actionableInsight")
    );

    private final GeminiService geminiService;
    private final ObjectMapper objectMapper;

    public AnalysisService(GeminiService geminiService, ObjectMapper objectMapper) {
        this.geminiService = geminiService;
        this.objectMapper = objectMapper;
    }

    /**
     * Analyzes a feedback entry's sentiment, category, and actionable insight via Gemini.
     * Always returns a usable result - falls back internally if Gemini or parsing fails.
     */
    public FeedbackAnalysis analyze(FeedbackEntry entry) {
        String prompt = buildPrompt(entry);
        try {
            String json = geminiService.generateJson(prompt, ANALYSIS_SCHEMA);
            return objectMapper.readValue(json, FeedbackAnalysis.class);
        } catch (Exception e) {
            return FeedbackAnalysis.fallback(e.getMessage());
        }
    }

    private String buildPrompt(FeedbackEntry entry) {
        return String.format("""
                You are an AI assistant specialized in customer feedback analysis.
                Analyze the following customer feedback and:
                1. Classify its sentiment.
                2. Categorize it into the most appropriate category.
                3. Provide a specific, actionable insight or recommendation based on the feedback.

                Customer Feedback:
                Comment: %s
                Department: %s
                """, entry.getComment(), entry.getDepartment());
    }
}
