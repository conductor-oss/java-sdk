package foodsafety.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;

public class CertifyWorker implements Worker {
    @Override public String getTaskDefName() { return "fsf_certify"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [certify] All checks passed - issuing certification");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("certification", Map.of("grade", "A", "score", 98, "validUntil", "2027-03-08"));
        return result;
    }
}
