package com.thiago.feedback_analyser.service;

import com.thiago.feedback_analyser.model.EnhancedFeedback;
import com.thiago.feedback_analyser.model.FeedbackAnalysis;
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

    private final AnalysisService analysisService;
    private final FileService fileService;
    private List<EnhancedFeedback> enhancedFeedbackCache = null;

    public FeedbackService(AnalysisService analysisService, FileService fileService) {
        this.analysisService = analysisService;
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
     */
    private EnhancedFeedback enhanceFeedback(FeedbackEntry entry) {
        EnhancedFeedback enhancedEntry = new EnhancedFeedback(entry);

        FeedbackAnalysis analysis = analysisService.analyze(entry);
        enhancedEntry.setSentiment(analysis.sentiment());
        enhancedEntry.setCategory(analysis.category());
        enhancedEntry.setActionableInsight(analysis.actionableInsight());

        return enhancedEntry;
    }

    /**
     * Creates a new feedback entry, persists it to the sentiment file, enhances it
     * with AI-generated category/insight, and appends it to the in-memory cache.
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
