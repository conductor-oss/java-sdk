package loadbalancerconfig.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Discovers backend servers for the load balancer.
 * Input: loadBalancer, action
 * Output: discover_backendsId, success
 */
public class DiscoverBackendsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "lb_discover_backends";
    }

    @Override
    public TaskResult execute(Task task) {
        String loadBalancer = (String) task.getInputData().getOrDefault("loadBalancer", "unknown");

        System.out.println("  [discover] " + loadBalancer + ": 6 backend servers found");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("discover_backendsId", "DISCOVER_BACKENDS-1354");
        output.put("success", true);
        result.setOutputData(output);
        return result;
    }
}
