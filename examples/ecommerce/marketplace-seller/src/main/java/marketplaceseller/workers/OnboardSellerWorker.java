package marketplaceseller.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.time.Instant;
import java.util.*;

public class OnboardSellerWorker implements Worker {
    @Override public String getTaskDefName() { return "mkt_onboard_seller"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [onboard] Registering seller " + task.getInputData().get("businessName"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> o = new LinkedHashMap<>();
        o.put("documents", Map.of("taxId", "EIN-12345", "license", "BL-67890"));
        o.put("registeredAt", Instant.now().toString());
        r.setOutputData(o); return r;
    }
}
