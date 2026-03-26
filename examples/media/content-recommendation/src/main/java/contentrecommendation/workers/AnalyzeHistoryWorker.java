package contentrecommendation.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;

public class AnalyzeHistoryWorker implements Worker {
    @Override public String getTaskDefName() { return "crm_analyze_history"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [history] Processing " + task.getInputData().getOrDefault("viewedItems", ""));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("viewedItems", List.of("VID-100"));
        r.getOutputData().put("likedItems", List.of("VID-100"));
        r.getOutputData().put("topCategories", List.of("technology"));
        r.getOutputData().put("activityScore", 72);
        return r;
    }
}
