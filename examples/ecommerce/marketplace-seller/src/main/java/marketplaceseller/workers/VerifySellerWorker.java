package marketplaceseller.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.time.Instant;
import java.util.*;

public class VerifySellerWorker implements Worker {
    @Override public String getTaskDefName() { return "mkt_verify_seller"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [verify] Verifying documents for seller " + task.getInputData().get("sellerId"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> o = new LinkedHashMap<>();
        o.put("verified", true); o.put("storeId", "STORE-" + task.getInputData().get("sellerId"));
        o.put("verifiedAt", Instant.now().toString());
        r.setOutputData(o); return r;
    }
}
