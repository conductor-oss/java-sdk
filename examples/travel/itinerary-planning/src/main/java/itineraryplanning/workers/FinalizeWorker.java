package itineraryplanning.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class FinalizeWorker implements Worker {
    @Override public String getTaskDefName() { return "itp_finalize"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [finalize] Itinerary finalized and sent to " + task.getInputData().get("travelerId"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("itineraryId", "ITN-544"); r.getOutputData().put("finalized", true);
        return r;
    }
}
