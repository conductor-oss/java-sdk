package eventwindowing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Computes aggregate statistics (count, min, max, sum, avg) over the value
 * field of windowed events. Returns consistent values.
 */
public class ComputeStatsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ew_compute_stats";
    }

    @Override
    public TaskResult execute(Task task) {
        String windowId = (String) task.getInputData().get("windowId");
        if (windowId == null || windowId.isBlank()) {
            windowId = "win_fixed_001";
        }

        System.out.println("  [ew_compute_stats] Computing stats for window: " + windowId);

        Map<String, Object> stats = Map.of(
                "count", 5,
                "min", 10,
                "max", 30,
                "sum", 100,
                "avg", "20.00"
        );

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("stats", stats);
        result.getOutputData().put("windowId", windowId);
        return result;
    }
}
