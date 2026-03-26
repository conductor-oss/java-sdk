package travelanalytics.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;
public class AggregateWorker implements Worker {
    @Override public String getTaskDefName() { return "tan_aggregate"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [aggregate] Aggregated bookings across categories");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("aggregated", Map.of("totalSpend",600000,"flights",195000,"hotels",125000,"carRentals",50000,"meals",30000)); return r;
    }
}
