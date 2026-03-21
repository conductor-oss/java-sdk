package loadbalancerconfig.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Configures routing rules for the load balancer.
 * Input: configure_rulesData (discover output)
 * Output: configure_rules (boolean), processed (boolean)
 */
public class ConfigureRulesWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "lb_configure_rules";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [configure] Updated routing rules: weighted round-robin");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("configure_rules", true);
        output.put("processed", true);
        result.setOutputData(output);
        return result;
    }
}
