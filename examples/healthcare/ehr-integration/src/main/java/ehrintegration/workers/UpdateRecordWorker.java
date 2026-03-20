package ehrintegration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public class UpdateRecordWorker implements Worker {

    @Override
    public String getTaskDefName() { return "ehr_update"; }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [update] Master record updated for patient " + task.getInputData().get("patientId"));

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("updated", true);
        output.put("updatedAt", Instant.now().toString());
        output.put("version", 3);
        result.setOutputData(output);
        return result;
    }
}
