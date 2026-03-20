package customersegmentation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.*;

public class TargetWorker implements Worker {
    @Override public String getTaskDefName() { return "seg_target"; }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<?> segments = (List<?>) task.getInputData().getOrDefault("segments", List.of());
        System.out.println("  [target] Creating campaigns for " + segments.size() + " segments");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("campaigns", List.of(
                Map.of("segment", "VIP", "strategy", "Exclusive early access & loyalty rewards"),
                Map.of("segment", "Regular", "strategy", "Upsell premium products with discounts"),
                Map.of("segment", "At-Risk", "strategy", "Re-engagement offers & win-back emails")));
        result.setOutputData(output);
        return result;
    }
}
