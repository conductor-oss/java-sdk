package billingmedical.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.*;

public class CodeProceduresWorker implements Worker {

    @Override
    public String getTaskDefName() { return "mbl_code_procedures"; }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [code] Coding procedures for encounter " + task.getInputData().get("encounterId"));

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> output = new LinkedHashMap<>();
        List<Map<String, Object>> cptCodes = new ArrayList<>();
        cptCodes.add(Map.of("code", "99213", "description", "Office visit, est. patient", "charge", 150));
        cptCodes.add(Map.of("code", "36415", "description", "Venipuncture", "charge", 25));
        cptCodes.add(Map.of("code", "80053", "description", "Comprehensive metabolic panel", "charge", 75));
        output.put("cptCodes", cptCodes);
        output.put("icdCodes", List.of("E11.9", "I10"));
        output.put("totalCharge", 250);
        result.setOutputData(output);
        return result;
    }
}
