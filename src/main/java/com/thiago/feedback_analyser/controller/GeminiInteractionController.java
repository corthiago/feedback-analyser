package com.thiago.feedback_analyser.controller;

import com.thiago.feedback_analyser.client.GeminiInteractionsClient;
import com.thiago.feedback_analyser.client.dto.InteractionRequest;
import com.thiago.feedback_analyser.client.dto.InteractionResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/interactions")
public class GeminiInteractionController {

    private final GeminiInteractionsClient geminiInteractionsClient;

    public GeminiInteractionController(GeminiInteractionsClient geminiInteractionsClient) {
        this.geminiInteractionsClient = geminiInteractionsClient;
    }

    @PostMapping
    public InteractionResponse createInteraction(@RequestBody InteractionRequest request) {
        return geminiInteractionsClient.createInteraction(request);
    }
}
