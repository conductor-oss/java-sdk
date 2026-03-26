package connectedvehicles.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class GeolocationWorker implements Worker {
    @Override public String getTaskDefName() { return "veh_geolocation"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [geo] Processing task");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("location", "location.address");
        r.getOutputData().put("coordinates", "location");
        return r;
    }
}
