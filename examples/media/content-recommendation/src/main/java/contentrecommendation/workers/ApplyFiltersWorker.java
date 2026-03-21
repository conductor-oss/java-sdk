package contentrecommendation.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class ApplyFiltersWorker implements Worker {
    @Override public String getTaskDefName() { return "crm_apply_filters"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [filter] Processing " + task.getInputData().getOrDefault("filteredItems", ""));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("filteredItems", "filtered");
        r.getOutputData().put("removedCount", "items.length - filtered.length");
        return r;
    }
}
