package knowledgebasesync.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;

public class CrawlWorker implements Worker {
    @Override public String getTaskDefName() { return "kbs_crawl"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [crawl] Crawled " + task.getInputData().get("sourceUrl") + " - found 156 pages");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("pages", List.of("faq.html", "setup.html", "api.html"));
        result.getOutputData().put("pageCount", 156);
        return result;
    }
}
