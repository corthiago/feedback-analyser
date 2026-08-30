package com.thiago.feedback_analyser.service;

import com.thiago.feedback_analyser.model.EnhancedFeedback;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

@Service
public class FileService {

    private static final String FEEDBACKS_FILE_PATH = "sentiment_feedback.txt";

    private final FeedbackParser feedbackParser;

    public FileService(FeedbackParser feedbackParser) {
        this.feedbackParser = feedbackParser;
    }

    /**
     * Reads feedback data from the sentiment analysis output file.
     */
    public List<EnhancedFeedback> readFeedbackData() throws IOException {
        List<EnhancedFeedback> entries = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(FEEDBACKS_FILE_PATH))) {
            StringBuilder entryText = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                // Process each line
                if (line.trim().isEmpty() && entryText.length() > 0) {
                    // We've reached the end of an entry
                    EnhancedFeedback entry = feedbackParser.parseEnhancedFeedback(entryText.toString());
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
                EnhancedFeedback entry = feedbackParser.parseEnhancedFeedback(entryText.toString());
                if (entry != null) {
                    entries.add(entry);
                }
            }
        }

        return entries;
    }


    /**
     * Appends a feedback to the sentiment file in the same format readFeedbackData parses.
     */
    public void appendFeedback(EnhancedFeedback feedback) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(FEEDBACKS_FILE_PATH, true))) {
            writer.write("Feedback #" + feedback.getId() + "\n");
            writer.write("Customer: " + feedback.getCustomer() + "\n");
            writer.write("Department: " + feedback.getDepartment() + "\n");
            writer.write("Date: " + feedback.getDate() + "\n");
            writer.write("Comment: " + feedback.getComment() + "\n");
            writer.write("Sentiment: " + feedback.getSentiment() + "\n");
            writer.write("Category: " + feedback.getCategory() + "\n");
            writer.write("Insight: " + feedback.getActionableInsight() + "\n");
            writer.write("\n");
        }
    }
}
