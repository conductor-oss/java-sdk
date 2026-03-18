package scattergather.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Gather worker 2: fetches data from the second source endpoint.
 *
 * Input: query, source
 * Output: response (map with source, price, currency, responseTimeMs)
 */
public class SgrGather2Worker implements Worker {

    @Override
    public String getTaskDefName() {
        return "sgr_gather_2";
    }

    @Override
    public TaskResult execute(Task task) {
        String query = (String) task.getInputData().getOrDefault("query", "");
        String source = (String) task.getInputData().getOrDefault("source", "price_service_b");

        Map<String, Object> response = GatherWorkerUtil.gatherFromSource(query, source, 1);

        System.out.println("  [gather-2] source=" + source + " price=" + response.get("price"));

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("response", response);
        return result;
    }
}
