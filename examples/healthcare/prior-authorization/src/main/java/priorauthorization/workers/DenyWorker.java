package priorauthorization.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.Map;

public class DenyWorker implements Worker {

    @Override
    public String getTaskDefName() { return "pa_deny"; }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [deny] Auth " + task.getInputData().get("authId")
                + " DENIED: " + task.getInputData().get("denyReason"));

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("denied", true);
        output.put("appealDeadline", "30 days");
        result.setOutputData(output);
        return result;
    }
}
