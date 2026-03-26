package milestonetracking.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;
public class CheckProgressWorker implements Worker {
    @Override public String getTaskDefName() { return "mst_check_progress"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [progress] Checking milestone " + task.getInputData().get("milestoneId"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("progress", Map.of("completed",7,"total",10,"pctDone",70)); return r;
    }
}
