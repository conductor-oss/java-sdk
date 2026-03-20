package marketplaceseller.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.*;

public class ManageOrdersWorker implements Worker {
    @Override public String getTaskDefName() { return "mkt_manage_orders"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [orders] Order management activated for store " + task.getInputData().get("storeId"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> o = new LinkedHashMap<>(); o.put("active", true); o.put("fulfillmentMode", "seller_fulfilled"); o.put("notificationsEnabled", true);
        r.setOutputData(o); return r;
    }
}
