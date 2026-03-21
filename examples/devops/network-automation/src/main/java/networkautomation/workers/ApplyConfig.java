package networkautomation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Applies planned configuration to network devices.
 * Input: apply_configData (from plan_changes output)
 * Output: applied, devicesConfigured, processed
 */
public class ApplyConfig implements Worker {

    @Override
    public String getTaskDefName() {
        return "na_apply_config";
    }

    @Override
    @SuppressWarnings("unchecked")
    public TaskResult execute(Task task) {
        Map<String, Object> data = (Map<String, Object>) task.getInputData().get("apply_configData");
        boolean planned = true;
        if (data != null && data.get("planned") != null) {
            planned = Boolean.TRUE.equals(data.get("planned"));
        }

        System.out.println("[na_apply_config] Configuration pushed to 12 devices (planned: " + planned + ")");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("applied", true);
        output.put("devicesConfigured", 12);
        output.put("processed", true);
        result.setOutputData(output);
        return result;
    }
}
