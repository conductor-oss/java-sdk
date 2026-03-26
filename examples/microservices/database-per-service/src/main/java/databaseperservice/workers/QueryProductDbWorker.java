package databaseperservice.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;

public class QueryProductDbWorker implements Worker {
    @Override public String getTaskDefName() { return "dps_query_product_db"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [product-db] Querying viewed products");
        TaskResult r = new TaskResult(task);
        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("recentProducts", List.of("Widget", "Gadget"));
        return r;
    }
}
