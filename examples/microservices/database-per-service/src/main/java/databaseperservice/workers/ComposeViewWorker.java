package databaseperservice.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;

public class ComposeViewWorker implements Worker {
    @Override public String getTaskDefName() { return "dps_compose_view"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [compose] Building unified view from 3 databases");
        TaskResult r = new TaskResult(task);
        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("view", Map.of(
            "user", task.getInputData().getOrDefault("user", Map.of()),
            "orders", task.getInputData().getOrDefault("orders", Map.of()),
            "products", task.getInputData().getOrDefault("products", Map.of())));
        return r;
    }
}
