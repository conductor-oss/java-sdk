package liveops.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class ScheduleEventWorker implements Worker {
    @Override public String getTaskDefName() { return "lop_schedule_event"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [schedule] Scheduling event: " + task.getInputData().get("eventName"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.addOutputData("eventId", "EVT-748"); r.addOutputData("scheduled", true);
        return r;
    }
}
