package datalakeingestion.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ValidateSchemaWorker implements Worker {
    @Override public String getTaskDefName() { return "li_validate_schema"; }

    @Override
    public TaskResult execute(Task task) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> records = (List<Map<String, Object>>) task.getInputData().get("records");
        if (records == null) records = List.of();

        List<Map<String, Object>> valid = new ArrayList<>();
        for (Map<String, Object> r : records) {
            if (r.get("event_date") != null && r.get("event_type") != null && r.get("user_id") != null) {
                valid.add(r);
            }
        }

        System.out.println("  [schema] Validated " + valid.size() + "/" + records.size() + " records against schema");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("validRecords", valid);
        result.getOutputData().put("validCount", valid.size());
        return result;
    }
}
