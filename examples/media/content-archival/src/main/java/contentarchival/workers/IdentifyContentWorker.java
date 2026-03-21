package contentarchival.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class IdentifyContentWorker implements Worker {
    @Override public String getTaskDefName() { return "car_identify_content"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [identify] Processing " + task.getInputData().getOrDefault("itemCount", ""));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("itemCount", 2450);
        r.getOutputData().put("totalSizeMb", 15200);
        r.getOutputData().put("oldestItem", "2024-01-15");
        r.getOutputData().put("newestItem", "2025-09-08");
        return r;
    }
}
