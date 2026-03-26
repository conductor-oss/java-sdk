package postmortemautomation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class ScheduleReviewWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "pm_schedule_review";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [review] Review meeting scheduled for next Tuesday");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("schedule_review", true);
        return result;
    }
}
