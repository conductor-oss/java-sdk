package socialmedia.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class SchedulePostWorker implements Worker {
    @Override public String getTaskDefName() { return "soc_schedule_post"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [schedule] Processing " + task.getInputData().getOrDefault("scheduledTime", ""));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("scheduledTime", task.getInputData().get("optimalTime"));
        r.getOutputData().put("scheduleId", "SCH-515-001");
        r.getOutputData().put("timezone", "America/New_York");
        return r;
    }
}
