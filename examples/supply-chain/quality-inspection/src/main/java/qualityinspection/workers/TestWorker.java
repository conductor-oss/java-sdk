package qualityinspection.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.*;

public class TestWorker implements Worker {
    @Override public String getTaskDefName() { return "qi_test"; }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<Map<String, Object>> samples = (List<Map<String, Object>>) task.getInputData().get("samples");
        if (samples == null) samples = List.of();
        int defects = 0;
        double defectRate = samples.isEmpty() ? 0 : (double) defects / samples.size();
        String testResult = defectRate < 0.05 ? "pass" : "fail";

        System.out.println("  [test] Tested " + samples.size() + " samples — " + defects + " defects (" +
                String.format("%.1f", defectRate * 100) + "%) -> " + testResult);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("result", testResult);
        result.getOutputData().put("defects", defects);
        result.getOutputData().put("defectRate", defectRate);
        return result;
    }
}
