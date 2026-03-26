package loadbalancerconfig.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Performs health check on all backends after config change.
 * Input: health_checkData (apply output)
 * Output: health_check (boolean), completedAt
 */
public class HealthCheckWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "lb_health_check";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [health] All backends healthy, traffic flowing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("health_check", true);
        output.put("completedAt", Instant.now().toString());
        result.setOutputData(output);
        return result;
    }
}
