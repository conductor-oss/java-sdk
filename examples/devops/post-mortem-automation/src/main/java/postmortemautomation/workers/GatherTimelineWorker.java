package postmortemautomation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class GatherTimelineWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "pm_gather_timeline";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [timeline] Gathered 24 events for incident INC-2024-042");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("gather_timelineId", "GATHER_TIMELINE-1337");
        result.addOutputData("success", true);
        return result;
    }
}
