package couponengine.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.*;

public class ValidateCodeWorker implements Worker {
    @Override public String getTaskDefName() { return "cpn_validate_code"; }

    @Override
    public TaskResult execute(Task task) {
        String code = (String) task.getInputData().get("couponCode");
        System.out.println("  [validate] Code \"" + code + "\": valid=true, 20% off");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("valid", true);
        output.put("discountType", "percentage");
        output.put("discountValue", 20);
        output.put("rules", Map.of("minCartTotal", 50, "maxUses", 100, "currentUses", 42, "expiresAt", "2025-12-31", "categories", List.of("all")));
        result.setOutputData(output);
        return result;
    }
}
