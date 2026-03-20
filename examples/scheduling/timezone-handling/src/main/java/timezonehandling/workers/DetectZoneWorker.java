package timezonehandling.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class DetectZoneWorker implements Worker {
    @Override public String getTaskDefName() { return "tz_detect_zone"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [detect] Detecting timezone for user " + task.getInputData().get("userId"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("timezone", "Asia/Tokyo");
        r.getOutputData().put("utcOffset", "+09:00");
        r.getOutputData().put("dstActive", false);
        return r;
    }
}
