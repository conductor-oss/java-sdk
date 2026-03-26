package buildingautomation.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class ScheduleWorker implements Worker {
    @Override public String getTaskDefName() { return "bld_schedule"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [schedule] Processing task");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("scheduledCount", "schedule.length");
        return r;
    }
}
