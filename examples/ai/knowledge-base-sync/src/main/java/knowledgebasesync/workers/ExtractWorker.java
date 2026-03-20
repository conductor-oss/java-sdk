package knowledgebasesync.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;

public class ExtractWorker implements Worker {
    @Override public String getTaskDefName() { return "kbs_extract"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [extract] Extracted 89 articles from crawled pages");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("articles", List.of(Map.of("title", "Getting Started"), Map.of("title", "API Reference")));
        result.getOutputData().put("articleCount", 89);
        return result;
    }
}
