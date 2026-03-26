package programevaluation.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;
public class BenchmarkWorker implements Worker {
    @Override public String getTaskDefName() { return "pev_benchmark"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [benchmark] Comparing against sector benchmarks");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.addOutputData("benchmarks", Map.of("sectorAvg", 72, "topQuartile", 85, "ranking", "above-average")); return r;
    }
}
