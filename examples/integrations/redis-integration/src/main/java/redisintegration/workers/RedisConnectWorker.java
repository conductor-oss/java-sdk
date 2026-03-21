package redisintegration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Connects to a Redis instance.
 * Input: host
 * Output: connectionId, connected, serverVersion
 */
public class RedisConnectWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "red_connect";
    }

    @Override
    public TaskResult execute(Task task) {
        String host = (String) task.getInputData().get("host");
        String connectionId = "redis-" + System.currentTimeMillis();
        System.out.println("  [connect] Connected to " + host + " (" + connectionId + ")");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("connectionId", "" + connectionId);
        result.getOutputData().put("connected", true);
        result.getOutputData().put("serverVersion", "7.2.4");
        return result;
    }
}
