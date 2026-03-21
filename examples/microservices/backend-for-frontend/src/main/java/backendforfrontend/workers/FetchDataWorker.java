package backendforfrontend.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;

public class FetchDataWorker implements Worker {
    @Override public String getTaskDefName() { return "bff_fetch_data"; }
    @Override public TaskResult execute(Task task) {
        String userId = (String) task.getInputData().getOrDefault("userId", "unknown");
        System.out.println("  [fetch] Loading data for user " + userId);
        TaskResult r = new TaskResult(task);
        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("profile", Map.of("name", "Alice"));
        r.getOutputData().put("orders", 5);
        r.getOutputData().put("notifications", 3);
        return r;
    }
}
