package legalbilling.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class TrackTimeWorker implements Worker {
    @Override public String getTaskDefName() { return "lgb_track_time"; }

    @Override public TaskResult execute(Task task) {
        String matterId = (String) task.getInputData().get("matterId");
        System.out.println("  [track-time] Collecting time entries for matter " + matterId);
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("timeEntries", java.util.List.of(
            java.util.Map.of("attorney", "J. Smith", "hours", 4.5, "description", "Contract review"),
            java.util.Map.of("attorney", "A. Jones", "hours", 2.0, "description", "Research")
        ));
        result.getOutputData().put("totalHours", 6.5);
        return result;
    }
}
