package interviewscheduling.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
public class AvailabilityWorker implements Worker {
    @Override public String getTaskDefName() { return "ivs_availability"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [availability] Checked calendars for " + task.getInputData().get("interviewers") + " interviewers");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("availableSlots", List.of("2024-03-25 10:00", "2024-03-25 14:00", "2024-03-26 11:00"));
        return r;
    }
}
