package com.thiago.feedback_analyser.model;

import java.time.LocalDateTime;

/**
 * Represents a feedback entry with sentiment analysis.
 */
public class FeedbackEntry {
    private int id;
    private String customer;
    private String department;
    private LocalDateTime date;
    private String comment;


    public FeedbackEntry() {
    }

    public FeedbackEntry(int id, String customer, String department, String comment) {
        this.id = id;
        this.customer = customer;
        this.department = department;
        this.date = LocalDateTime.now();
        this.comment = comment;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCustomer() {
        return customer;
    }

    public void setCustomer(String customer) {
        this.customer = customer;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

}
