package populationhealth.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.*;

public class StratifyRiskWorker implements Worker {

    @Override
    public String getTaskDefName() { return "pop_stratify_risk"; }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [stratify] Stratifying patients by risk");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> output = new LinkedHashMap<>();
        List<Map<String, Object>> strata = new ArrayList<>();
        strata.add(Map.of("level", "high", "count", 490, "percentage", 20, "criteria", "HbA1c > 9, complications"));
        strata.add(Map.of("level", "moderate", "count", 735, "percentage", 30, "criteria", "HbA1c 7-9, no complications"));
        strata.add(Map.of("level", "low", "count", 1225, "percentage", 50, "criteria", "HbA1c < 7, well controlled"));
        output.put("riskStrata", strata);
        result.setOutputData(output);
        return result;
    }
}
