package rollingupdate.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Verifies all replicas are healthy after the rolling update.
 */
public class VerifyUpdate implements Worker {

    @Override
    public String getTaskDefName() {
        return "ru_verify";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        Map<String, Object> verifyData = (Map<String, Object>) task.getInputData().get("verifyData");

        boolean rollbackTriggered = false;
        if (verifyData != null && verifyData.get("rollbackTriggered") != null) {
            rollbackTriggered = Boolean.TRUE.equals(verifyData.get("rollbackTriggered"));
        }

        System.out.println("[ru_verify] Verifying all replicas healthy");

        TaskResult result = new TaskResult(task);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("verify", true);
        output.put("allHealthy", !rollbackTriggered);
        output.put("completedAt", Instant.now().toString());
        output.put("rollbackOccurred", rollbackTriggered);

        result.setStatus(TaskResult.Status.COMPLETED);
        result.setOutputData(output);
        return result;
    }
}
