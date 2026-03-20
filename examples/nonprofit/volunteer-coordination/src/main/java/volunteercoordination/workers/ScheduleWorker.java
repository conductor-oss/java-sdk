package volunteercoordination.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class ScheduleWorker implements Worker {
    @Override public String getTaskDefName() { return "vol_schedule"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [schedule] Scheduling " + task.getInputData().get("volunteerId"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.addOutputData("scheduled", true); r.addOutputData("date", "2026-03-15"); r.addOutputData("shift", "9:00 AM - 1:00 PM"); return r;
    }
}
