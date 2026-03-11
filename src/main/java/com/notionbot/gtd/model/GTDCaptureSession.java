package com.notionbot.gtd.model;

public class GTDCaptureSession {
    private final String userId;
    private final String originalText;
    private String refinedText;
    private String description;
    private String githubContext;
    private String dueDate;
    private FlowStep currentStep;

    public enum FlowStep {
        START,
        REFINING,
        REFINED,
        DECIDING_ACTIONABLE,
        DECIDING_MULTISTEP,
        DECIDING_2MIN,
        DECIDING_DELEGATE_DEFER,
        PROCESSING_NOTION,
        COMPLETED
    }

    public GTDCaptureSession(String userId, String originalText) {
        this.userId = userId;
        this.originalText = originalText;
        this.currentStep = FlowStep.START;
    }

    public String getUserId() {
        return userId;
    }

    public String getOriginalText() {
        return originalText;
    }

    public String getRefinedText() {
        return refinedText;
    }

    public void setRefinedText(String refinedText) {
        this.refinedText = refinedText;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getGithubContext() {
        return githubContext;
    }

    public void setGithubContext(String githubContext) {
        this.githubContext = githubContext;
    }

    public String getDueDate() {
        return dueDate;
    }

    public void setDueDate(String dueDate) {
        this.dueDate = dueDate;
    }

    public FlowStep getCurrentStep() {
        return currentStep;
    }

    public void setCurrentStep(FlowStep currentStep) {
        this.currentStep = currentStep;
    }
}
