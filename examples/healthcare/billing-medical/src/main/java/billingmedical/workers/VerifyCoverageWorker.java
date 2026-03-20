package billingmedical.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.*;

public class VerifyCoverageWorker implements Worker {

    @Override
    public String getTaskDefName() { return "mbl_verify_coverage"; }

    @Override
    public TaskResult execute(Task task) {
        List<?> codes = (List<?>) task.getInputData().getOrDefault("cptCodes", List.of());
        System.out.println("  [coverage] Verifying coverage for " + codes.size() + " procedure codes");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("coveragePercent", 80);
        output.put("copay", 30);
        output.put("deductibleApplied", 0);
        output.put("allCovered", true);
        result.setOutputData(output);
        return result;
    }
}
