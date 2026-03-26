package contentsyndication.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;

public class SelectContentWorker implements Worker {
    @Override public String getTaskDefName() { return "syn_select_content"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [select] Processing " + task.getInputData().getOrDefault("contentBody", ""));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("contentBody", "Workflow orchestration enables teams to build reliable distributed systems...");
        r.getOutputData().put("metadata", Map.of());
        r.getOutputData().put("category", "technology");
        r.getOutputData().put("wordCount", 1200);
        return r;
    }
}
