package carrental.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List; import java.util.Map;
public class SearchWorker implements Worker {
    @Override public String getTaskDefName() { return "crl_search"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [search] Searching rentals at " + task.getInputData().get("location"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("available", List.of(Map.of("model","Toyota Camry","class","midsize","dailyRate",65),Map.of("model","Honda CR-V","class","suv","dailyRate",85)));
        return r;
    }
}
