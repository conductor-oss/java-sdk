package codegeneration.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;
public class ParseRequirementsWorker implements Worker {
    @Override public String getTaskDefName() { return "cdg_parse_requirements"; }
    @Override public TaskResult execute(Task task) {
        String req = (String) task.getInputData().getOrDefault("requirements", "");
        System.out.println("  [parse] Requirements parsed: " + req.substring(0, Math.min(60, req.length())) + "...");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("parsed", Map.of("entities", List.of("User", "Order"), "operations", List.of("create", "read", "update"), "constraints", List.of("auth required", "pagination")));
        return r;
    }
}
