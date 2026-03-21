package usersurvey.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;

public class CollectResponsesWorker implements Worker {
    @Override public String getTaskDefName() { return "usv_collect"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [collect] Collected 847 responses (33.9% response rate)");
        TaskResult r = new TaskResult(task);
        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("responses", List.of(Map.of("q1", 4), Map.of("q1", 5)));
        r.getOutputData().put("responseCount", 847);
        r.getOutputData().put("responseRate", "33.9%");
        return r;
    }
}
