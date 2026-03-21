package rollingupdate.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Plans the rolling update strategy (batch size, max unavailable, etc.).
 */
public class PlanUpdate implements Worker {

    @Override
    public String getTaskDefName() {
        return "ru_plan";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        Map<String, Object> planData = (Map<String, Object>) task.getInputData().get("planData");

        int replicas = 3;
        if (planData != null && planData.get("currentReplicas") != null) {
            replicas = ((Number) planData.get("currentReplicas")).intValue();
        }

        System.out.println("[ru_plan] Rolling update: 1 at a time, 20% max unavailable");

        TaskResult result = new TaskResult(task);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("plan", true);
        output.put("processed", true);
        output.put("strategy", "rolling");
        output.put("batchSize", 1);
        output.put("maxUnavailable", Math.max(1, replicas / 5));
        output.put("totalBatches", replicas);
        output.put("rollbackOnFailure", true);

        result.setStatus(TaskResult.Status.COMPLETED);
        result.setOutputData(output);
        return result;
    }
}
