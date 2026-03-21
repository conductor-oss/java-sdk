package databaseperservice.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class QueryOrderDbWorker implements Worker {
    @Override public String getTaskDefName() { return "dps_query_order_db"; }
    @Override public TaskResult execute(Task task) {
        String userId = (String) task.getInputData().getOrDefault("userId", "unknown");
        System.out.println("  [order-db] Querying orders for " + userId);
        TaskResult r = new TaskResult(task);
        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("orderCount", 12);
        r.getOutputData().put("lastOrder", "ORD-999");
        return r;
    }
}
