package com.thiago.feedback_analyser.service;

import tools.jackson.databind.ObjectMapper;
import com.thiago.feedback_analyser.model.EnhancedFeedback;
import com.thiago.feedback_analyser.model.FeedbackEntry;
import com.thiago.feedback_analyser.model.FeedbackSummary;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for processing feedback data and enhancing it with AI insights.
 */
@Service
public class FeedbackService {

    // Constrains Gemini's structured output for enhanceFeedback to exactly these fields
    private static final Map<String, Object> ANALYSIS_SCHEMA = Map.of(
            "type", "object",
            "properties", Map.of(
                    "sentiment", Map.of("type", "string", "enum", List.of("Positive", "Neutral", "Negative")),
                    "category", Map.of("type", "string", "enum", List.of(
                            "Product Quality", "Customer Service", "Store Experience", "Website/App",
                            "Delivery", "Price/Value", "Inventory/Stock", "Other")),
                    "actionableInsight", Map.of("type", "string")
            ),
            "required", List.of("sentiment", "category", "actionableInsight")
    );

    private record FeedbackAnalysis(String sentiment, String category, String actionableInsight) {
    }

    private final GeminiService geminiService;
    private final FileService fileService;
    private final ObjectMapper objectMapper;

    // Cache for feedback data
    private List<EnhancedFeedback> enhancedFeedbackCache = null;

    public FeedbackService(GeminiService geminiService, FileService fileService, ObjectMapper objectMapper) {
        this.geminiService = geminiService;
        this.fileService = fileService;
        this.objectMapper = objectMapper;
    }

    /**
     * Enhances feedback with AI-generated categories and actionable insights.
     */
    public synchronized List<EnhancedFeedback> getEnhancedFeedback() throws IOException {
        // Return cached data if available
        if (enhancedFeedbackCache != null) {
            return enhancedFeedbackCache;
        }

        List<FeedbackEntry> entries = fileService.readFeedbackData();
        List<EnhancedFeedback> enhancedEntries = new ArrayList<>();
        for (FeedbackEntry entry : entries) {
            EnhancedFeedback enhancedEntry = enhanceFeedback(entry);
            enhancedEntries.add(enhancedEntry);
        }

        // Cache the enhanced feedback
        enhancedFeedbackCache = enhancedEntries;

        return enhancedEntries;
    }

    /**
     * Enhances a single feedback entry with AI-generated category and actionable insight.
     *
     * @param entry FeedbackEntry to enhance
     * @return EnhancedFeedback with AI-generated category and actionable insight
     */
    private EnhancedFeedback enhanceFeedback(FeedbackEntry entry) {
        EnhancedFeedback enhancedEntry = new EnhancedFeedback(entry);

        // Create a prompt for Gemini - the response shape is enforced by ANALYSIS_SCHEMA,
        // not by prompt instructions
        String prompt = String.format("""
            You are an AI assistant specialized in customer feedback analysis.
            Analyze the following customer feedback and:
            1. Classify its sentiment.
            2. Categorize it into the most appropriate category.
            3. Provide a specific, actionable insight or recommendation based on the feedback.

            Customer Feedback:
            Comment: %s
            Department: %s
            """, entry.getComment(), entry.getDepartment());

        try {
            String json = geminiService.generateJson(prompt, ANALYSIS_SCHEMA);
            FeedbackAnalysis analysis = objectMapper.readValue(json, FeedbackAnalysis.class);
            enhancedEntry.setSentiment(analysis.sentiment());
            enhancedEntry.setCategory(analysis.category());
            enhancedEntry.setActionableInsight(analysis.actionableInsight());
        } catch (Exception e) {
            // Handle API errors or malformed responses gracefully
            enhancedEntry.setSentiment("Uncategorized");
            enhancedEntry.setCategory("Error in processing");
            enhancedEntry.setActionableInsight("Could not generate insight due to API error: " + e.getMessage());
        }

        return enhancedEntry;
    }

    /**
     * Creates a new feedback entry, persists it to the sentiment file, enhances it
     * with AI-generated category/insight, and appends it to the in-memory cache.
     * If sentiment is null or blank, it is auto-detected from the comment via Gemini.
     *
     * @return The newly created, enhanced feedback entry
     * @throws IOException If an I/O error occurs
     */
    public synchronized EnhancedFeedback createFeedback(String customer, String department, String comment) throws IOException {
        List<FeedbackEntry> entries = fileService.readFeedbackData();
        int nextId = entries.stream().mapToInt(FeedbackEntry::getId).max().orElse(0) + 1;

        FeedbackEntry entry = new FeedbackEntry(nextId, customer, department, comment);
        fileService.appendFeedbackEntry(entry);

        EnhancedFeedback enhanced = enhanceFeedback(entry);


        if (enhancedFeedbackCache != null) {
            enhancedFeedbackCache.add(enhanced);
        }

        return enhanced;
    }


    /**
     * Generates a summary of the feedback data for the dashboard.
     *
     * @return FeedbackSummary object
     * @throws IOException If an I/O error occurs
     */
    public FeedbackSummary generateFeedbackSummary() throws IOException {
        List<EnhancedFeedback> allFeedback = getEnhancedFeedback();
        FeedbackSummary summary = new FeedbackSummary();

        // Set total feedback count
        summary.setTotalFeedback(allFeedback.size());

        // Count sentiments
        Map<String, Integer> sentimentCounts = new HashMap<>();
        for (EnhancedFeedback feedback : allFeedback) {
            String sentiment = feedback.getSentiment();
            sentimentCounts.put(sentiment, sentimentCounts.getOrDefault(sentiment, 0) + 1);
        }
        summary.setSentimentCounts(sentimentCounts);

        // Count categories
        Map<String, Integer> categoryCounts = new HashMap<>();
        for (EnhancedFeedback feedback : allFeedback) {
            String category = feedback.getCategory();
            categoryCounts.put(category, categoryCounts.getOrDefault(category, 0) + 1);
        }
        summary.setCategoryCounts(categoryCounts);

        // Count departments
        Map<String, Integer> departmentCounts = new HashMap<>();
        for (EnhancedFeedback feedback : allFeedback) {
            String department = feedback.getDepartment();
            departmentCounts.put(department, departmentCounts.getOrDefault(department, 0) + 1);
        }
        summary.setDepartmentCounts(departmentCounts);

        // Get recent feedback (last 10 entries)
        List<EnhancedFeedback> recentFeedback = allFeedback.stream()
                .sorted(Comparator.comparing(EnhancedFeedback::getId).reversed())
                .limit(10)
                .collect(Collectors.toList());
        summary.setRecentFeedback(recentFeedback);

        return summary;
    }
}
