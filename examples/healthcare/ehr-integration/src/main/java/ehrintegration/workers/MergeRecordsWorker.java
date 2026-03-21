package ehrintegration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.*;

public class MergeRecordsWorker implements Worker {

    @Override
    public String getTaskDefName() { return "ehr_merge_records"; }

    @Override
    public TaskResult execute(Task task) {
        List<?> records = (List<?>) task.getInputData().getOrDefault("records", List.of());
        System.out.println("  [merge] Merging " + records.size() + " records from different sources");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> output = new LinkedHashMap<>();
        Map<String, Object> merged = new LinkedHashMap<>();
        merged.put("name", "Sarah Johnson");
        merged.put("dob", "1985-06-15");
        merged.put("allergies", List.of("penicillin", "sulfa"));
        merged.put("medications", List.of("Lisinopril"));
        output.put("mergedRecord", merged);
        output.put("sourceCount", records.size());
        output.put("conflictsResolved", 1);
        result.setOutputData(output);
        return result;
    }
}
