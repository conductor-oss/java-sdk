package userbehavioranalytics.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;

public class SessionizeWorker implements Worker {
    @Override public String getTaskDefName() { return "uba_sessionize"; }
    @Override public TaskResult execute(Task task) {
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        System.out.println("  [sessionize] Grouping " + task.getInputData().get("eventCount") + " events into sessions");
        r.getOutputData().put("sessionCount", 8);
        r.getOutputData().put("avgSessionDuration", 900);
        return r;
    }
}
