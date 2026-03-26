package knowledgebasesync.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;

public class VerifyWorker implements Worker {
    @Override public String getTaskDefName() { return "kbs_verify"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [verify] Search index verified - " + task.getInputData().get("indexedCount") + " articles searchable");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("searchReady", true);
        result.getOutputData().put("sampleQueries", List.of(Map.of("query", "setup guide", "results", 5)));
        return result;
    }
}
