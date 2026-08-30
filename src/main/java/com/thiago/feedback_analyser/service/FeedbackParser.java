package com.thiago.feedback_analyser.service;

import com.thiago.feedback_analyser.model.EnhancedFeedback;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class FeedbackParser {

    private static final Pattern FEEDBACK_PATTERN = Pattern.compile("Feedback #(\\d+)");
    private static final Pattern CUSTOMER_PATTERN = Pattern.compile("Customer:\\s*(.+)");
    private static final Pattern DEPARTMENT_PATTERN = Pattern.compile("Department:\\s*(.+)");
    private static final Pattern DATE_PATTERN = Pattern.compile("Date:\\s*(.+)");
    private static final Pattern COMMENT_PATTERN = Pattern.compile("Comment:\\s*(.+)");
    private static final Pattern SENTIMENT_PATTERN = Pattern.compile("Sentiment:\\s*(.+)");
    private static final Pattern CATEGORY_PATTERN = Pattern.compile("Category:\\s*(.+)");
    private static final Pattern INSIGHT_PATTERN = Pattern.compile("Insight:\\s*(.+)");

    /**
     * Parses a feedback entry from text.
     */
    public EnhancedFeedback parseEnhancedFeedback(String text) {
        EnhancedFeedback entry = new EnhancedFeedback();

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

        // Extract sentiment
        Matcher sentimentMatcher = SENTIMENT_PATTERN.matcher(text);
        if (sentimentMatcher.find()) {
            entry.setSentiment(sentimentMatcher.group(1));
        }

        // Extract category
        Matcher categoryMatcher = CATEGORY_PATTERN.matcher(text);
        if (categoryMatcher.find()) {
            entry.setCategory(categoryMatcher.group(1));
        }

        // Extract insight
        Matcher insightMatcher = INSIGHT_PATTERN.matcher(text);
        if (insightMatcher.find()) {
            entry.setActionableInsight(insightMatcher.group(1));
        }

        return entry;
    }

    private LocalDateTime parseDate(String raw) {
        try {
            return LocalDateTime.parse(raw);
        } catch (DateTimeParseException e) {
            return LocalDate.parse(raw).atStartOfDay();
        }
    }

}
