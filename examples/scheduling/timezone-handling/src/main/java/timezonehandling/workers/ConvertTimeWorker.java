package timezonehandling.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class ConvertTimeWorker implements Worker {
    @Override public String getTaskDefName() { return "tz_convert_time"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [convert] Converting " + task.getInputData().get("requestedTime") + " from " + task.getInputData().get("sourceTimezone") + " to " + task.getInputData().get("targetTimezone"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("convertedTime", "2026-03-08T17:00:00Z");
        r.getOutputData().put("originalTime", task.getInputData().get("requestedTime"));
        r.getOutputData().put("offset", "-9h");
        return r;
    }
}
