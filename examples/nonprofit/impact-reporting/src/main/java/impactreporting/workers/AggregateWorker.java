package impactreporting.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;
public class AggregateWorker implements Worker {
    @Override public String getTaskDefName() { return "ipr_aggregate"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [aggregate] Aggregating program data");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.addOutputData("aggregated", Map.of("totalServed", 5200, "mealsProvided", 42000, "hoursVolunteered", 9600)); return r;
    }
}
