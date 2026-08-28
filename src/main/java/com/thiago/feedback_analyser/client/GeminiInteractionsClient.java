package com.thiago.feedback_analyser.client;

import com.thiago.feedback_analyser.config.GeminiFeignConfig;
import com.thiago.feedback_analyser.client.dto.InteractionRequest;
import com.thiago.feedback_analyser.client.dto.InteractionResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(
        name = "gemini-interactions",
        url = "${gemini.api.base-url}",
        configuration = GeminiFeignConfig.class
)
public interface GeminiInteractionsClient {

    @PostMapping("/interactions")
    InteractionResponse createInteraction(@RequestBody InteractionRequest request);

}
