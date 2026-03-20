package changemanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Submits a change request and generates a tracking ID.
 * Input: changeType, description, system
 * Output: submitId, changeType, description, system, success
 */
public class SubmitChange implements Worker {

    @Override
    public String getTaskDefName() {
        return "cm_submit";
    }

    @Override
    public TaskResult execute(Task task) {
        String changeType = (String) task.getInputData().get("changeType");
        if (changeType == null) changeType = "standard";

        String description = (String) task.getInputData().get("description");
        if (description == null) description = "No description provided";

        String system = (String) task.getInputData().get("system");
        if (system == null) system = "unknown";

        System.out.println("[cm_submit] Change request: " + changeType + " — " + description);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("submitId", "SUBMIT-1351");
        output.put("changeType", changeType);
        output.put("description", description);
        output.put("system", system);
        output.put("success", true);
        result.setOutputData(output);
        return result;
    }
}
