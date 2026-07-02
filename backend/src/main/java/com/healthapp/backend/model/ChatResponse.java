package com.healthapp.backend.model;

import java.util.List;

public class ChatResponse {
    private String reply;
    private List<String> suggestions;
    private String action;
    private String actionData;

    public ChatResponse() {}

    public ChatResponse(String reply, List<String> suggestions) {
        this.reply = reply;
        this.suggestions = suggestions;
    }

    public ChatResponse(String reply, List<String> suggestions, String action, String actionData) {
        this.reply = reply;
        this.suggestions = suggestions;
        this.action = action;
        this.actionData = actionData;
    }

    public String getReply() {
        return reply;
    }

    public void setReply(String reply) {
        this.reply = reply;
    }

    public List<String> getSuggestions() {
        return suggestions;
    }

    public void setSuggestions(List<String> suggestions) {
        this.suggestions = suggestions;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getActionData() {
        return actionData;
    }

    public void setActionData(String actionData) {
        this.actionData = actionData;
    }
}
