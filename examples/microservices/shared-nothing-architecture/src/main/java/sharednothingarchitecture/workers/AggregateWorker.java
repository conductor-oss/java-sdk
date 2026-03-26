package sharednothingarchitecture.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;

public class AggregateWorker implements Worker {
    @Override public String getTaskDefName() { return "sn_aggregate"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [aggregate] Combining all results");
        TaskResult r = new TaskResult(task);
        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("aggregated", Map.of(
            "a", task.getInputData().getOrDefault("a", ""),
            "b", task.getInputData().getOrDefault("b", ""),
            "c", task.getInputData().getOrDefault("c", "")));
        return r;
    }
}
