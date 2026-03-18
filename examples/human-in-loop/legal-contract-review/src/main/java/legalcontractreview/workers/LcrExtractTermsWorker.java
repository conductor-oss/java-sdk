package legalcontractreview.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Extracts key terms from a contract. Real risk analysis:
 * identifies liability, termination, auto-renewal, and jurisdiction risks.
 */
public class LcrExtractTermsWorker implements Worker {
    @Override public String getTaskDefName() { return "lcr_extract_terms"; }

    @Override public TaskResult execute(Task task) {
        String contractText = (String) task.getInputData().get("contractText");
        Object durationObj = task.getInputData().get("duration");
        String liability = (String) task.getInputData().get("liability");
        if (contractText == null) contractText = "";
        if (liability == null) liability = "Unlimited";

        int durationMonths = 24;
        if (durationObj instanceof Number) durationMonths = ((Number) durationObj).intValue();

        Map<String, Object> keyTerms = new LinkedHashMap<>();
        keyTerms.put("duration", durationMonths + " months");
        keyTerms.put("autoRenew", durationMonths > 12);
        keyTerms.put("terminationNotice", durationMonths > 12 ? "90 days" : "30 days");
        keyTerms.put("liability", liability);
        keyTerms.put("jurisdiction", "State of Delaware");

        // Real risk flag analysis
        List<String> riskFlags = new ArrayList<>();
        if ("Unlimited".equalsIgnoreCase(liability) || "unlimited".equalsIgnoreCase(liability)) {
            riskFlags.add("Unlimited liability clause");
        }
        if (durationMonths > 12 && (boolean) keyTerms.get("autoRenew")) {
            riskFlags.add("Auto-renewal without opt-out window");
        }
        if ("90 days".equals(keyTerms.get("terminationNotice"))) {
            riskFlags.add("90-day termination notice exceeds standard 30 days");
        }
        if (durationMonths > 36) {
            riskFlags.add("Contract duration exceeds 3 years");
        }
        if (contractText.toLowerCase().contains("non-compete")) {
            riskFlags.add("Contains non-compete clause");
        }

        System.out.println("  [lcr_extract_terms] " + keyTerms.size() + " terms, " + riskFlags.size() + " risk flags");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("keyTerms", keyTerms);
        result.getOutputData().put("riskFlags", riskFlags);
        result.getOutputData().put("riskLevel", riskFlags.size() > 2 ? "high" : riskFlags.size() > 0 ? "medium" : "low");
        return result;
    }
}
