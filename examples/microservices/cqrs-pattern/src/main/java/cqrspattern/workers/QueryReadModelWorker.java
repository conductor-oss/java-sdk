package cqrspattern.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;

public class QueryReadModelWorker implements Worker {
    @Override public String getTaskDefName() { return "cqrs_query_read_model"; }
    @Override public TaskResult execute(Task task) {
        String aggregateId = (String) task.getInputData().getOrDefault("aggregateId", "unknown");
        TaskResult r = new TaskResult(task);
        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("result", Map.of("id", aggregateId, "name", "Widget"));
        return r;
    }
}
