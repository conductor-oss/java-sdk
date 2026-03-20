package supplychainiot.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class TrackShipmentWorker implements Worker {
    @Override public String getTaskDefName() { return "sci_track_shipment"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [track] Processing task");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("done", true);
        return r;
    }
}
