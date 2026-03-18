package leadscoring.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Collects lead signals for scoring. Real signal extraction and normalization.
 */
public class CollectSignalsWorker implements Worker {
    @Override public String getTaskDefName() { return "ls_collect_signals"; }

    @Override public TaskResult execute(Task task) {
        String companySize = (String) task.getInputData().get("companySize");
        String industry = (String) task.getInputData().get("industry");
        Object pageViewsObj = task.getInputData().get("pageViews");
        Object emailOpensObj = task.getInputData().get("emailOpens");
        Object demoRequestedObj = task.getInputData().get("demoRequested");

        if (companySize == null) companySize = "unknown";
        if (industry == null) industry = "unknown";
        int pageViews = pageViewsObj instanceof Number ? ((Number) pageViewsObj).intValue() : 0;
        int emailOpens = emailOpensObj instanceof Number ? ((Number) emailOpensObj).intValue() : 0;
        boolean demoRequested = Boolean.TRUE.equals(demoRequestedObj);

        Map<String, Object> signals = new LinkedHashMap<>();
        signals.put("companySize", companySize);
        signals.put("industry", industry);
        signals.put("pageViews", pageViews);
        signals.put("emailOpens", emailOpens);
        signals.put("demoRequested", demoRequested);
        signals.put("engagementLevel", pageViews > 20 ? "high" : pageViews > 5 ? "medium" : "low");

        System.out.println("  [signals] Company: " + companySize + " " + industry
                + ", engagement: " + signals.get("engagementLevel"));

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("signals", signals);
        return result;
    }
}
