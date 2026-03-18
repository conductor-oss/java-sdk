package agentcollaboration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Analyst agent — examines business context and produces structured insights
 * with severity ratings and a metrics summary.
 */
public class AnalystWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ac_analyst";
    }

    @Override
    public TaskResult execute(Task task) {
        String businessContext = (String) task.getInputData().get("businessContext");
        if (businessContext == null || businessContext.isBlank()) {
            businessContext = "unspecified business context";
        }

        System.out.println("  [ac_analyst] Analyzing: " + businessContext);

        List<Map<String, Object>> insights = new ArrayList<>();

        Map<String, Object> insight1 = new LinkedHashMap<>();
        insight1.put("id", "INS-001");
        insight1.put("finding", "Customer churn concentrated in 90-day post-acquisition window");
        insight1.put("severity", "high");
        insight1.put("category", "retention");
        insights.add(insight1);

        Map<String, Object> insight2 = new LinkedHashMap<>();
        insight2.put("id", "INS-002");
        insight2.put("finding", "Repeat purchase rate declined 22% quarter-over-quarter");
        insight2.put("severity", "critical");
        insight2.put("category", "revenue");
        insights.add(insight2);

        Map<String, Object> insight3 = new LinkedHashMap<>();
        insight3.put("id", "INS-003");
        insight3.put("finding", "Customer support response time averaging 48 hours");
        insight3.put("severity", "medium");
        insight3.put("category", "operations");
        insights.add(insight3);

        Map<String, Object> insight4 = new LinkedHashMap<>();
        insight4.put("id", "INS-004");
        insight4.put("finding", "Loyalty program engagement below industry benchmark");
        insight4.put("severity", "high");
        insight4.put("category", "engagement");
        insights.add(insight4);

        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("churnRate", "15%");
        metrics.put("repeatPurchaseDecline", "22%");
        metrics.put("avgSupportResponseHours", 48);
        metrics.put("loyaltyEngagementRate", "12%");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("insights", insights);
        result.getOutputData().put("metrics", metrics);
        result.getOutputData().put("insightCount", 4);
        return result;
    }
}
