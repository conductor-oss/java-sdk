package deliverytracking.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;

public class TrackWorker implements Worker {
    @Override public String getTaskDefName() { return "dlt_track"; }

    @Override public TaskResult execute(Task task) {
        System.out.println("  [track] Tracking driver " + task.getInputData().get("driverId") + " to " + task.getInputData().get("destination"));
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("location", Map.of("lat", 37.78, "lng", -122.41));
        result.addOutputData("eta", "12 min");
        return result;
    }
}
