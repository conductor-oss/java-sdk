package calendarintegration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;

public class CompareSchedulesWorker implements Worker {
    @Override public String getTaskDefName() { return "cal_compare_schedules"; }

    @Override
    public TaskResult execute(Task task) {
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        System.out.println("  [compare] Comparing " + task.getInputData().get("eventCount") + " external events with internal schedule");
        result.getOutputData().put("additions", 1);
        result.getOutputData().put("updates", 1);
        result.getOutputData().put("deletions", 0);
        result.getOutputData().put("conflicts", 0);
        return result;
    }
}
