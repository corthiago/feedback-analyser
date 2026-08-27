package com.thiago.feedback_analyser.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public class InteractionResponse {

    private String id;
    private String object;
    private String model;
    private String status;
    private String created;
    private String updated;
    private String outputText;
    private List<Step> steps;
    private Usage usage;
    private List<InteractionError> errors;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getObject() {
        return object;
    }

    public void setObject(String object) {
        this.object = object;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCreated() {
        return created;
    }

    public void setCreated(String created) {
        this.created = created;
    }

    public String getUpdated() {
        return updated;
    }

    public void setUpdated(String updated) {
        this.updated = updated;
    }

    /**
     * Returns the model's generated text. The Gemini API does not actually return an
     * "output_text" field on the raw REST response (that's a convenience property computed
     * client-side by the official SDKs) - so when it's absent, this derives the same value by
     * concatenating the text content of "model_output" steps.
     */
    public String getOutputText() {
        if (outputText != null) {
            return outputText;
        }
        if (steps == null) {
            return null;
        }

        StringBuilder text = new StringBuilder();
        for (Step step : steps) {
            if (!"model_output".equals(step.getType()) || step.getContent() == null) {
                continue;
            }
            for (ContentPart part : step.getContent()) {
                if ("text".equals(part.getType()) && part.getText() != null) {
                    text.append(part.getText());
                }
            }
        }

        return text.isEmpty() ? null : text.toString();
    }

    public void setOutputText(String outputText) {
        this.outputText = outputText;
    }

    public List<Step> getSteps() {
        return steps;
    }

    public void setSteps(List<Step> steps) {
        this.steps = steps;
    }

    public Usage getUsage() {
        return usage;
    }

    public void setUsage(Usage usage) {
        this.usage = usage;
    }

    public List<InteractionError> getErrors() {
        return errors;
    }

    public void setErrors(List<InteractionError> errors) {
        this.errors = errors;
    }
}
