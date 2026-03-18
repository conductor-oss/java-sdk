package agenthandoff.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Triage agent — classifies an incoming customer message into a category
 * (billing, technical, or general) based on keyword detection. Returns a
 * deterministic confidence score computed from the number of matching keywords.
 */
public class TriageWorker implements Worker {

    private static final String[] TECHNICAL_KEYWORDS = {"api", "error", "timeout", "crash", "bug", "500", "latency", "outage"};
    private static final String[] BILLING_KEYWORDS = {"bill", "charge", "invoice", "refund", "payment", "subscription", "overcharged", "credit"};

    @Override
    public String getTaskDefName() {
        return "ah_triage";
    }

    @Override
    public TaskResult execute(Task task) {
        String customerId = (String) task.getInputData().get("customerId");
        String message = (String) task.getInputData().get("message");

        if (customerId == null || customerId.isBlank()) {
            customerId = "unknown";
        }
        if (message == null || message.isBlank()) {
            message = "";
        }

        System.out.println("  [ah_triage] Classifying message from customer " + customerId);

        String lowerMessage = message.toLowerCase();

        List<String> techMatches = matchKeywords(lowerMessage, TECHNICAL_KEYWORDS);
        List<String> billingMatches = matchKeywords(lowerMessage, BILLING_KEYWORDS);

        String category;
        String notes;
        List<String> matchedKeywords;

        if (techMatches.size() >= billingMatches.size() && !techMatches.isEmpty()) {
            category = "technical";
            matchedKeywords = techMatches;
            notes = "Detected technical keywords (" + String.join(", ", techMatches) + "); routing to tech support.";
        } else if (!billingMatches.isEmpty()) {
            category = "billing";
            matchedKeywords = billingMatches;
            notes = "Detected billing keywords (" + String.join(", ", billingMatches) + "); routing to billing team.";
        } else {
            category = "general";
            matchedKeywords = List.of();
            notes = "No specific category detected; routing to general support.";
        }

        // Confidence: base 0.6 for general (no keyword match), 0.75 for single keyword,
        // +0.05 per additional keyword, capped at 0.95.
        double confidence;
        if (matchedKeywords.isEmpty()) {
            confidence = 0.6;
        } else {
            confidence = Math.min(0.95, 0.75 + (matchedKeywords.size() - 1) * 0.05);
        }

        // Urgency: "high" when 3+ keywords match or message contains escalation signals.
        String urgency;
        if (matchedKeywords.size() >= 3
                || lowerMessage.contains("urgent")
                || lowerMessage.contains("critical")
                || lowerMessage.contains("emergency")) {
            urgency = "high";
        } else {
            urgency = "normal";
        }

        Map<String, Object> output = new LinkedHashMap<>();
        output.put("category", category);
        output.put("urgency", urgency);
        output.put("notes", notes);
        output.put("confidence", confidence);
        output.put("matchedKeywords", matchedKeywords);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().putAll(output);
        return result;
    }

    /** Returns the subset of keywords found in the message, preserving order. */
    static List<String> matchKeywords(String lowerMessage, String[] keywords) {
        List<String> matches = new ArrayList<>();
        for (String kw : keywords) {
            if (lowerMessage.contains(kw)) {
                matches.add(kw);
            }
        }
        return matches;
    }
}
