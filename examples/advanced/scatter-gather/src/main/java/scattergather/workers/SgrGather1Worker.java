package scattergather.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Gather worker 1: fetches data from the first source endpoint.
 * Makes a real HTTP HEAD request to measure response time and
 * generates a price based on the query hash for deterministic results.
 *
 * Input: query, source
 * Output: response (map with source, price, currency, responseTimeMs)
 */
public class SgrGather1Worker implements Worker {

    @Override
    public String getTaskDefName() {
        return "sgr_gather_1";
    }

    @Override
    public TaskResult execute(Task task) {
        String query = (String) task.getInputData().getOrDefault("query", "");
        String source = (String) task.getInputData().getOrDefault("source", "price_service_a");

        Map<String, Object> response = GatherWorkerUtil.gatherFromSource(query, source, 0);

        System.out.println("  [gather-1] source=" + source + " price=" + response.get("price"));

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("response", response);
        return result;
    }
}
