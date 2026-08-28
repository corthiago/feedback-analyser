package com.thiago.feedback_analyser.model;

public record FeedbackAnalysis(String sentiment, String category, String actionableInsight) {

    public static FeedbackAnalysis fallback(String errorDetail) {
        return new FeedbackAnalysis(
                "Uncategorized",
                "Error in processing",
                "Could not generate insight due to API error: " + errorDetail
        );
    }
}
