package com.thiago.feedback_analyser.service;

import com.thiago.feedback_analyser.model.EnhancedFeedback;
import com.thiago.feedback_analyser.model.FeedbackEntry;
import com.thiago.feedback_analyser.model.FeedbackSummary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Service for processing feedback data and enhancing it with AI insights.
 */
@Service
public class FeedbackService {

    @Autowired
    private GeminiService geminiService;

    // Path to sentiment analysis output file
    private static final String FEEDBACKS_FILE_PATH = "sentiment_feedback.txt";

    // Patterns for parsing feedback file
    private static final Pattern FEEDBACK_PATTERN = Pattern.compile("Feedback #(\\d+)");
    private static final Pattern CUSTOMER_PATTERN = Pattern.compile("Customer:\\s*(.+)");
    private static final Pattern DEPARTMENT_PATTERN = Pattern.compile("Department:\\s*(.+)");
    private static final Pattern DATE_PATTERN = Pattern.compile("Date:\\s*(.+)");
    private static final Pattern COMMENT_PATTERN = Pattern.compile("Comment:\\s*(.+)");
    private static final Pattern SENTIMENT_PATTERN = Pattern.compile("Sentiment:\\s*(.+)");

    // Cache for feedback data
    private List<EnhancedFeedback> enhancedFeedbackCache = null;

    /**
     * Reads feedback data from the sentiment analysis output file.
     *
     * @return List of FeedbackEntry objects
     * @throws IOException If an I/O error occurs
     */
    public List<FeedbackEntry> readFeedbackData() throws IOException {
        List<FeedbackEntry> entries = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(FEEDBACKS_FILE_PATH))) {
            StringBuilder entryText = new StringBuilder();
            String line;

//            boolean inDetailedSection = false;
            while ((line = reader.readLine()) != null) {
//                // Check if we've reached the detailed feedback section
//                if (line.contains("## Detailed Feedback Entries")) {
//                    inDetailedSection = true;
//                    continue;
//                }
//
//                if (!inDetailedSection) {
//                    continue;
//                }

                // Process each line
                if (line.trim().isEmpty() && entryText.length() > 0) {
                    // We've reached the end of an entry
                    FeedbackEntry entry = parseFeedbackEntry(entryText.toString());
                    if (entry != null) {
                        entries.add(entry);
                    }
                    entryText = new StringBuilder();
                } else {
                    entryText.append(line).append("\n");
                }
            }

            // Process the last entry if there is one
            if (entryText.length() > 0) {
                FeedbackEntry entry = parseFeedbackEntry(entryText.toString());
                if (entry != null) {
                    entries.add(entry);
                }
            }
        }

        return entries;
    }

    /**
     * Parses a feedback entry from text.
     *
     * @param text Text containing a feedback entry
     * @return FeedbackEntry object or null if parsing fails
     */
    private FeedbackEntry parseFeedbackEntry(String text) {
        FeedbackEntry entry = new FeedbackEntry();

        // Extract feedback ID
        Matcher idMatcher = FEEDBACK_PATTERN.matcher(text);
        if (idMatcher.find()) {
            entry.setId(Integer.parseInt(idMatcher.group(1)));
        } else {
            return null;
        }

        // Extract customer
        Matcher customerMatcher = CUSTOMER_PATTERN.matcher(text);
        if (customerMatcher.find()) {
            entry.setCustomer(customerMatcher.group(1));
        }

        // Extract department
        Matcher departmentMatcher = DEPARTMENT_PATTERN.matcher(text);
        if (departmentMatcher.find()) {
            entry.setDepartment(departmentMatcher.group(1));
        }

        // Extract date
        Matcher dateMatcher = DATE_PATTERN.matcher(text);
        if (dateMatcher.find()) {
            entry.setDate(parseDate(dateMatcher.group(1)));
        }

        // Extract comment
        Matcher commentMatcher = COMMENT_PATTERN.matcher(text);
        if (commentMatcher.find()) {
            entry.setComment(commentMatcher.group(1));
        }

        return entry;
    }

    /**
     * Parses a date string into a LocalDateTime. Handles both full timestamps (written by
     * createFeedback going forward) and legacy date-only strings (from the original sample
     * data), which are interpreted as start-of-day.
     */
    private LocalDateTime parseDate(String raw) {
        try {
            return LocalDateTime.parse(raw);
        } catch (DateTimeParseException e) {
            return LocalDate.parse(raw).atStartOfDay();
        }
    }

    /**
     * Enhances feedback with AI-generated categories and actionable insights.
     *
     * @return List of EnhancedFeedback objects
     * @throws IOException If an I/O error occurs
     */
    public synchronized List<EnhancedFeedback> getEnhancedFeedback() throws IOException {
        // Return cached data if available
        if (enhancedFeedbackCache != null) {
            return enhancedFeedbackCache;
        }

        List<FeedbackEntry> entries = readFeedbackData();
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
        List<FeedbackEntry> entries = readFeedbackData();
        int nextId = entries.stream().mapToInt(FeedbackEntry::getId).max().orElse(0) + 1;

        FeedbackEntry entry = new FeedbackEntry(nextId, customer, department, comment);
        appendFeedbackEntry(entry);

        EnhancedFeedback enhanced = enhanceFeedback(entry);


        if (enhancedFeedbackCache != null) {
            enhancedFeedbackCache.add(enhanced);
        }

        return enhanced;
    }


    /**
     * Appends a feedback entry to the sentiment file in the same format readFeedbackData parses.
     */
    private void appendFeedbackEntry(FeedbackEntry entry) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(FEEDBACKS_FILE_PATH, true))) {
            writer.write("Feedback #" + entry.getId() + "\n");
            writer.write("Customer: " + entry.getCustomer() + "\n");
            writer.write("Department: " + entry.getDepartment() + "\n");
            writer.write("Date: " + entry.getDate() + "\n");
            writer.write("Comment: " + entry.getComment() + "\n");
            writer.write("\n");
        }
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
