package agentcollaboration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Strategist agent — takes analyst insights and metrics, then formulates
 * a named strategy with a thesis, pillars, and ranked priorities.
 */
public class StrategistWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ac_strategist";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        String businessContext = (String) task.getInputData().get("businessContext");
        if (businessContext == null || businessContext.isBlank()) {
            businessContext = "unspecified business context";
        }

        System.out.println("  [ac_strategist] Formulating strategy for: " + businessContext);

        Map<String, Object> strategy = new LinkedHashMap<>();
        strategy.put("name", "Stabilize & Retain");
        strategy.put("thesis", "Address churn by improving post-acquisition experience, accelerating support response, and revitalizing the loyalty program to rebuild repeat purchase momentum.");

        List<String> pillars = new ArrayList<>();
        pillars.add("Customer Experience Overhaul");
        pillars.add("Support Response Acceleration");
        pillars.add("Loyalty Program Revitalization");
        strategy.put("pillars", pillars);

        List<Map<String, Object>> priorities = new ArrayList<>();

        Map<String, Object> p1 = new LinkedHashMap<>();
        p1.put("rank", 1);
        p1.put("area", "Onboarding & Early Retention");
        p1.put("effort", "high");
        p1.put("impact", "critical");
        priorities.add(p1);

        Map<String, Object> p2 = new LinkedHashMap<>();
        p2.put("rank", 2);
        p2.put("area", "Support SLA Improvement");
        p2.put("effort", "medium");
        p2.put("impact", "high");
        priorities.add(p2);

        Map<String, Object> p3 = new LinkedHashMap<>();
        p3.put("rank", 3);
        p3.put("area", "Loyalty & Rewards Redesign");
        p3.put("effort", "medium");
        p3.put("impact", "high");
        priorities.add(p3);

        Map<String, Object> p4 = new LinkedHashMap<>();
        p4.put("rank", 4);
        p4.put("area", "Revenue Recovery Campaigns");
        p4.put("effort", "low");
        p4.put("impact", "medium");
        priorities.add(p4);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("strategy", strategy);
        result.getOutputData().put("priorities", priorities);
        result.getOutputData().put("strategyName", "Stabilize & Retain");
        return result;
    }
}
