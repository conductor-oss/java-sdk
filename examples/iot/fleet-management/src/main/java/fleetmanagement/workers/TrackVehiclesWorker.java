package fleetmanagement.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;

public class TrackVehiclesWorker implements Worker {
    @Override public String getTaskDefName() { return "flt_track_vehicles"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [track] Processing " + task.getInputData().getOrDefault("availableVehicles", ""));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("availableVehicles", List.of());
        r.getOutputData().put("id", "VEH-001");
        r.getOutputData().put("lat", 37.7899);
        r.getOutputData().put("lng", -122.4194);
        r.getOutputData().put("fuelLevel", 85);
        r.getOutputData().put("status", "available");
        return r;
    }
}
