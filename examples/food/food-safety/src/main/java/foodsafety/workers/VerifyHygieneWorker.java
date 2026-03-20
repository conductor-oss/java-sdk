package foodsafety.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;

public class VerifyHygieneWorker implements Worker {
    @Override public String getTaskDefName() { return "fsf_verify_hygiene"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [hygiene] Verifying hygiene at " + task.getInputData().get("restaurantId"));
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("hygiene", Map.of("handwashing", "pass", "surfaces", "pass", "crossContamination", "pass", "score", 98));
        return result;
    }
}
