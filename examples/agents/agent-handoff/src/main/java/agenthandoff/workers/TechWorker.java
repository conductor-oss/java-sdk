package agenthandoff.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Technical support agent — handles technical issues such as API errors,
 * timeouts, and rate limiting. Performs deterministic root-cause analysis
 * based on the keywords in the customer message and returns diagnostics.
 */
public class TechWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ah_tech";
    }

    @Override
    public TaskResult execute(Task task) {
        String customerId = (String) task.getInputData().get("customerId");
        String message = (String) task.getInputData().get("message");
        String triageNotes = (String) task.getInputData().get("triageNotes");
        String urgency = (String) task.getInputData().get("urgency");

        if (customerId == null || customerId.isBlank()) {
            customerId = "unknown";
        }
        if (message == null || message.isBlank()) {
            message = "";
        }
        if (triageNotes == null) {
            triageNotes = "";
        }
        if (urgency == null || urgency.isBlank()) {
            urgency = "normal";
        }

        System.out.println("  [ah_tech] Handling technical issue for customer " + customerId);

        // Deterministic ticket ID based on customer ID hash.
        int ticketNum = Math.abs(customerId.hashCode() % 9000) + 1000;
        String ticketId = "TECH-" + ticketNum;

        // Determine root cause and diagnostics based on message content.
        String lowerMessage = message.toLowerCase();
        String rootCause;
        String resolution;
        Map<String, Object> diagnostics = new LinkedHashMap<>();

        if (lowerMessage.contains("rate") || lowerMessage.contains("limit") || lowerMessage.contains("throttl")) {
            rootCause = "rate_limiting";
            resolution = "Customer " + customerId + " exceeded API rate limits. Increased quota from 100 to 500 requests/min and cleared the throttle backlog.";
            diagnostics.put("apiLatencyMs", 85);
            diagnostics.put("errorRate", "0.0%");
            diagnostics.put("rootCause", "rate_limit_exceeded");
            diagnostics.put("rateLimitRemaining", 500);
        } else if (lowerMessage.contains("timeout")) {
            rootCause = "timeout";
            resolution = "Detected elevated API latency for " + customerId + ". Upstream service response times exceeded threshold. Restarted connection pool; customer should retry in 60 seconds.";
            diagnostics.put("apiLatencyMs", 4200);
            diagnostics.put("errorRate", "0.3%");
            diagnostics.put("rootCause", "upstream_timeout");
        } else if (lowerMessage.contains("500") || lowerMessage.contains("error")) {
            rootCause = "server_error";
            resolution = "Traced 500 errors for " + customerId + " to a failed deployment on the payments service. Rolled back to the previous stable version.";
            diagnostics.put("apiLatencyMs", 120);
            diagnostics.put("errorRate", "12.4%");
            diagnostics.put("rootCause", "bad_deployment");
        } else {
            rootCause = "unclassified";
            resolution = "Investigated technical issue for " + customerId + ". No specific root cause identified from message content. Escalated to engineering for deeper analysis.";
            diagnostics.put("apiLatencyMs", 200);
            diagnostics.put("errorRate", "1.1%");
            diagnostics.put("rootCause", "pending_investigation");
        }

        if ("high".equals(urgency)) {
            diagnostics.put("priority", "P1");
        }

        Map<String, Object> output = new LinkedHashMap<>();
        output.put("specialist", "technical");
        output.put("ticketId", ticketId);
        output.put("resolution", resolution);
        output.put("diagnostics", diagnostics);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().putAll(output);
        return result;
    }
}
