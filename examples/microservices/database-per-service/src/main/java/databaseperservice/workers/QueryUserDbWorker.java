package databaseperservice.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class QueryUserDbWorker implements Worker {
    @Override public String getTaskDefName() { return "dps_query_user_db"; }
    @Override public TaskResult execute(Task task) {
        String userId = (String) task.getInputData().getOrDefault("userId", "unknown");
        System.out.println("  [user-db] Querying user " + userId);
        TaskResult r = new TaskResult(task);
        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("name", "Alice");
        r.getOutputData().put("tier", "premium");
        return r;
    }
}
