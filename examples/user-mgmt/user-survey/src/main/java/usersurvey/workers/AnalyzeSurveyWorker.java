package usersurvey.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;

public class AnalyzeSurveyWorker implements Worker {
    @Override public String getTaskDefName() { return "usv_analyze"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [analyze] Survey analysis complete");
        TaskResult r = new TaskResult(task);
        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("analysis", Map.of(
                "avgSatisfaction", 4.2,
                "topThemes", List.of("ease of use", "performance", "pricing"),
                "sentimentBreakdown", Map.of("positive", 62, "neutral", 28, "negative", 10)));
        return r;
    }
}
