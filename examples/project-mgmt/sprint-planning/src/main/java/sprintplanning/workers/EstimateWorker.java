package sprintplanning.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List; import java.util.Map;
public class EstimateWorker implements Worker {
    @Override public String getTaskDefName() { return "spn_estimate"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [estimate] Estimating stories");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("estimatedStories", List.of(Map.of("id","US-101","points",5),Map.of("id","US-102","points",8),Map.of("id","US-103","points",3)));
        r.getOutputData().put("totalPoints", 16); return r;
    }
}
