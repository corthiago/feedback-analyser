package com.thiago.feedback_analyser.service;

import com.thiago.feedback_analyser.model.FeedbackEntry;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

@Service
public class FileService {

    private static final String FEEDBACKS_FILE_PATH = "sentiment_feedback.txt";

    private final FeedbackParserService feedbackParserService;

    public FileService(FeedbackParserService feedbackParserService) {
        this.feedbackParserService = feedbackParserService;
    }

    /**
     * Reads feedback data from the sentiment analysis output file.
     */
    public List<FeedbackEntry> readFeedbackData() throws IOException {
        List<FeedbackEntry> entries = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(FEEDBACKS_FILE_PATH))) {
            StringBuilder entryText = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                // Process each line
                if (line.trim().isEmpty() && entryText.length() > 0) {
                    // We've reached the end of an entry
                    FeedbackEntry entry = feedbackParserService.parseFeedbackEntry(entryText.toString());
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
                FeedbackEntry entry = feedbackParserService.parseFeedbackEntry(entryText.toString());
                if (entry != null) {
                    entries.add(entry);
                }
            }
        }

        return entries;
    }


    /**
     * Appends a feedback entry to the sentiment file in the same format readFeedbackData parses.
     */
    public void appendFeedbackEntry(FeedbackEntry entry) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(FEEDBACKS_FILE_PATH, true))) {
            writer.write("Feedback #" + entry.getId() + "\n");
            writer.write("Customer: " + entry.getCustomer() + "\n");
            writer.write("Department: " + entry.getDepartment() + "\n");
            writer.write("Date: " + entry.getDate() + "\n");
            writer.write("Comment: " + entry.getComment() + "\n");
            writer.write("\n");
        }
    }
}
