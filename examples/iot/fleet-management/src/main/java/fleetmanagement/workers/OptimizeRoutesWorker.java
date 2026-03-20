package fleetmanagement.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class OptimizeRoutesWorker implements Worker {
    @Override public String getTaskDefName() { return "flt_optimize_routes"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [route] Processing " + task.getInputData().getOrDefault("assignedVehicleId", ""));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("assignedVehicleId", "VEH-001");
        r.getOutputData().put("driverId", "DRV-042");
        r.getOutputData().put("routeId", "RTE-535-001");
        r.getOutputData().put("estimatedDistance", 28.5);
        r.getOutputData().put("estimatedDuration", 42);
        r.getOutputData().put("estimatedFuel", 3.2);
        return r;
    }
}
