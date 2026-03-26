package seoworkflow.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;

public class ResearchKeywordsWorker implements Worker {
    @Override public String getTaskDefName() { return "seo_research_keywords"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [keywords] Processing " + task.getInputData().getOrDefault("topKeywords", ""));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("topKeywords", List.of());
        r.getOutputData().put("keyword", "workflow automation");
        r.getOutputData().put("volume", 12000);
        r.getOutputData().put("difficulty", 45);
        r.getOutputData().put("currentRank", 15);
        return r;
    }
}
