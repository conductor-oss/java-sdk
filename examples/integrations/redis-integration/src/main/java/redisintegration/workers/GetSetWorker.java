package redisintegration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Performs GET/SET operations.
 * Input: connectionId, key, value
 * Output: setOk, retrievedValue
 */
public class GetSetWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "red_get_set";
    }

    @Override
    public TaskResult execute(Task task) {
        String key = (String) task.getInputData().get("key");
        Object value = task.getInputData().get("value");
        System.out.println("  [set] " + key + " = " + value);
        System.out.println("  [get] " + key + " -> " + value);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("setOk", true);
        result.getOutputData().put("retrievedValue", "" + value);
        return result;
    }
}
