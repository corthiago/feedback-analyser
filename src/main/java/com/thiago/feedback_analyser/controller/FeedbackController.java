package com.thiago.feedback_analyser.controller;

import com.thiago.feedback_analyser.model.EnhancedFeedback;
import com.thiago.feedback_analyser.model.FeedbackSummary;
import com.thiago.feedback_analyser.service.FeedbackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.List;

/**
 * Controller for feedback-related endpoints.
 */
@Controller
public class FeedbackController {

    @Autowired
    private FeedbackService feedbackService;

    /**
     * Displays the dashboard page.
     *
     * @param model Model to add attributes to
     * @return The name of the view to render
     */
    @GetMapping("/")
    public String dashboard(Model model) {
        try {
            FeedbackSummary summary = feedbackService.generateFeedbackSummary();
            model.addAttribute("summary", summary);
            return "dashboard";
        } catch (IOException e) {
            model.addAttribute("error", "Error loading feedback data: " + e.getMessage());
            return "error";
        }
    }

    /**
     * Displays the form for creating a new feedback entry.
     *
     * @return The name of the view to render
     */
    @GetMapping("/feedback/new")
    public String newFeedbackForm() {
        return "create-feedback";
    }

    /**
     * Creates a new feedback entry and redirects back to the dashboard.
     *
     * @return Redirect to the dashboard
     */
    @PostMapping("/feedback")
    public String createFeedback(@RequestParam String customer,
                                  @RequestParam String department,
                                  @RequestParam String date,
                                  @RequestParam String comment,
                                  @RequestParam(required = false) String sentiment,
                                  RedirectAttributes redirectAttributes) {
        try {
            feedbackService.createFeedback(customer, department, date, comment, sentiment);
            redirectAttributes.addFlashAttribute("success", "Feedback submitted successfully.");
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("error", "Error creating feedback: " + e.getMessage());
        }
        return "redirect:/";
    }

    /**
     * Returns all feedback data in JSON format.
     *
     * @return List of enhanced feedback entries
     */
    @GetMapping("/getfeedback")
    @ResponseBody
    public ResponseEntity<List<EnhancedFeedback>> getFeedback() {
        try {
            List<EnhancedFeedback> feedback = feedbackService.getEnhancedFeedback();
            return ResponseEntity.ok(feedback);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
