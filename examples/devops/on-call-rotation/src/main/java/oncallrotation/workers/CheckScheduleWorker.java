package oncallrotation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class CheckScheduleWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "oc_check_schedule";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [schedule] platform-eng: rotation due — Alice -> Bob");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("check_scheduleId", "CHECK_SCHEDULE-1524");
        result.addOutputData("success", true);
        return result;
    }
}
