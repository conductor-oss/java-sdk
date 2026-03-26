package usermigration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;

public class TransformWorker implements Worker {
    @Override public String getTaskDefName() { return "umg_transform"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [transform] Transformed user schema (v1 -> v2)");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("transformed", List.of(Map.of("id", 1, "version", 2)));
        result.getOutputData().put("transformedCount", 500);
        result.getOutputData().put("fieldsAdded", List.of("avatar", "timezone"));
        return result;
    }
}
