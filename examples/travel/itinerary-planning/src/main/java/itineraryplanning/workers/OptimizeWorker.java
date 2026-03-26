package itineraryplanning.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;
public class OptimizeWorker implements Worker {
    @Override public String getTaskDefName() { return "itp_optimize"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [optimize] Optimized for cost and convenience");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("optimizedItinerary", Map.of("flight","DL-567","hotel","Marriott Downtown","savings",230));
        return r;
    }
}
