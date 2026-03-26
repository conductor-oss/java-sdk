package seoworkflow.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;

public class SubmitSitemapWorker implements Worker {
    @Override public String getTaskDefName() { return "seo_submit_sitemap"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [submit] Processing " + task.getInputData().getOrDefault("submissionId", ""));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("submissionId", "SITEMAP-525-001");
        r.getOutputData().put("submittedTo", List.of("Google Search Console"));
        r.getOutputData().put("pagesInSitemap", 145);
        return r;
    }
}
