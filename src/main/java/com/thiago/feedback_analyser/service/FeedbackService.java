package com.thiago.feedback_analyser.service;

import com.thiago.feedback_analyser.model.EnhancedFeedback;
import com.thiago.feedback_analyser.model.FeedbackEntry;
import com.thiago.feedback_analyser.model.FeedbackSummary;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Service for processing feedback data and enhancing it with AI insights.
 */
@Service
public class FeedbackService {

    private final GeminiService geminiService;
    private final FileService fileService;

    // Cache for feedback data
    private List<EnhancedFeedback> enhancedFeedbackCache = null;

    public FeedbackService(GeminiService geminiService, FileService fileService) {
        this.geminiService = geminiService;
        this.fileService = fileService;
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

        // Create a prompt for Gemini
        String prompt = String.format("""
            You are an AI assistant specialized in customer feedback analysis.
            Analyze the following customer feedback and:
            1. Classify the sentiment of the following customer feedback comment. Respond with exactly one word: Positive, Neutral, or Negative. No punctuation or explanation.
            2. Categorize the feedback into one of these categories: Product Quality, Customer Service, Store Experience, Website/App, Delivery, Price/Value, Inventory/Stock, or Other.
            3. Provide a specific actionable insight or recommendation based on the feedback.
            
            Format your response as JSON with three fields: "sentiment", "category" and "actionableInsight".
            Keep your response concise but insightful.
            
            Customer Feedback:
            Comment: %s
            Department: %s
            
            Provide the category and actionable insight as JSON:
            """, entry.getComment(), entry.getDepartment());

        try {
            // Call Gemini API and parse the response
            String response = geminiService.generateContent(prompt);

            // Parse JSON response
            // This is a simple parsing approach - for production, use a proper JSON parser
            String jsonResponse = response.trim();

            // Extract sentiment
            Pattern sentimentPattern = Pattern.compile("\"sentiment\"\\s*:\\s*\"([^\"]+)\"");
            Matcher sentimentMatcher = sentimentPattern.matcher(jsonResponse);
            if (sentimentMatcher.find()) {
                enhancedEntry.setSentiment(sentimentMatcher.group(1));
            } else {
                enhancedEntry.setSentiment("Uncategorized");
            }

            // Extract category
            Pattern categoryPattern = Pattern.compile("\"category\"\\s*:\\s*\"([^\"]+)\"");
            Matcher categoryMatcher = categoryPattern.matcher(jsonResponse);
            if (categoryMatcher.find()) {
                enhancedEntry.setCategory(categoryMatcher.group(1));
            } else {
                enhancedEntry.setCategory("Uncategorized");
            }

            // Extract actionable insight
            Pattern insightPattern = Pattern.compile("\"actionableInsight\"\\s*:\\s*\"([^\"]+)\"");
            Matcher insightMatcher = insightPattern.matcher(jsonResponse);
            if (insightMatcher.find()) {
                enhancedEntry.setActionableInsight(insightMatcher.group(1));
            } else {
                enhancedEntry.setActionableInsight("No specific action recommended.");
            }

        } catch (Exception e) {
            // Handle API errors gracefully
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
