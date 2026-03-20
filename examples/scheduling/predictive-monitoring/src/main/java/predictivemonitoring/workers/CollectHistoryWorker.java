package predictivemonitoring.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;

public class CollectHistoryWorker implements Worker {
    @Override public String getTaskDefName() { return "pdm_collect_history"; }
    @Override public TaskResult execute(Task task) {
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        System.out.println("  [collect] Collecting " + task.getInputData().get("historyDays") + " days of history");
        r.getOutputData().put("dataPoints", 43200);
        return r;
    }
}
