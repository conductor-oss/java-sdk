package loadbalancing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Processes a partition of a batch on a specific instance.
 * Input: instanceId, host, partition
 * Output: result (instanceId, host, partition, recordsProcessed, latencyMs)
 */
public class CallInstanceWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "lb_call_instance";
    }

    @Override
    public TaskResult execute(Task task) {
        String instanceId = (String) task.getInputData().get("instanceId");
        String host = (String) task.getInputData().get("host");
        String partition = (String) task.getInputData().get("partition");
        if (instanceId == null) instanceId = "unknown";
        if (host == null) host = "0.0.0.0:0";
        if (partition == null) partition = "unknown";

        System.out.println("  [" + instanceId + "] Processing " + partition + " on " + host + "...");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("result", Map.of(
                "instanceId", instanceId,
                "host", host,
                "partition", partition,
                "recordsProcessed", 50,
                "latencyMs", 45
        ));
        return result;
    }
}
