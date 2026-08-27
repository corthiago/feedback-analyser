package com.thiago.feedback_analyser.client.dto;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class Step {

    private String type;
    private List<ContentPart> content;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<ContentPart> getContent() {
        return content;
    }

    public void setContent(List<ContentPart> content) {
        this.content = content;
    }
}
