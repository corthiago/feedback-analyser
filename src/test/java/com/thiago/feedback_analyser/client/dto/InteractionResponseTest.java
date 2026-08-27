package com.thiago.feedback_analyser.client.dto;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InteractionResponseTest {

    @Test
    void returnsRawOutputTextWhenPresent() {
        InteractionResponse response = new InteractionResponse();
        response.setOutputText("already set");

        assertThat(response.getOutputText()).isEqualTo("already set");
    }

    @Test
    void derivesOutputTextFromModelOutputSteps() {
        ContentPart thoughtNotIncluded = new ContentPart();
        thoughtNotIncluded.setType("text");
        thoughtNotIncluded.setText("should not appear");
        Step thoughtStep = new Step();
        thoughtStep.setType("thought");
        thoughtStep.setContent(List.of(thoughtNotIncluded));

        ContentPart firstPart = new ContentPart();
        firstPart.setType("text");
        firstPart.setText("Hello, ");
        ContentPart secondPart = new ContentPart();
        secondPart.setType("text");
        secondPart.setText("World!");
        Step modelOutputStep = new Step();
        modelOutputStep.setType("model_output");
        modelOutputStep.setContent(List.of(firstPart, secondPart));

        InteractionResponse response = new InteractionResponse();
        response.setSteps(List.of(thoughtStep, modelOutputStep));

        assertThat(response.getOutputText()).isEqualTo("Hello, World!");
    }

    @Test
    void returnsNullWhenNoStepsOrOutputText() {
        InteractionResponse response = new InteractionResponse();

        assertThat(response.getOutputText()).isNull();
    }
}
