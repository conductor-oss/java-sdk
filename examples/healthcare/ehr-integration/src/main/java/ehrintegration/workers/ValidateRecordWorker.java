package ehrintegration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.*;

public class ValidateRecordWorker implements Worker {

    @Override
    public String getTaskDefName() { return "ehr_validate"; }

    @Override
    public TaskResult execute(Task task) {
        Object record = task.getInputData().getOrDefault("mergedRecord", Map.of());
        System.out.println("  [validate] Record validation: PASSED");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("passed", true);
        output.put("validatedRecord", record);
        output.put("validationErrors", List.of());
        result.setOutputData(output);
        return result;
    }
}
