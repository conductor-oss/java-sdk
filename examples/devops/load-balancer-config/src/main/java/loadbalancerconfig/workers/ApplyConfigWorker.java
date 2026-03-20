package loadbalancerconfig.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Applies the load balancer configuration without dropping connections.
 * Input: apply_configData (configure output)
 * Output: apply_config (boolean), processed (boolean)
 */
public class ApplyConfigWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "lb_apply_config";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [apply] Configuration applied without connection drops");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("apply_config", true);
        output.put("processed", true);
        result.setOutputData(output);
        return result;
    }
}
