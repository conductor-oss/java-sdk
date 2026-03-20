package ragembeddingselection.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Worker that demonstrates embedding evaluation using a local all-MiniLM-L6-v2 model.
 * Returns pre-computed metrics for the benchmark dataset.
 */
public class EmbedLocalWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "es_embed_local";
    }

    @Override
    public TaskResult execute(Task task) {
        Map<String, Object> metrics = Map.of(
                "model", "local/all-MiniLM-L6-v2",
                "dimensions", 384,
                "precisionAt1", 0.82,
                "precisionAt3", 0.78,
                "recallAt5", 0.86,
                "ndcg", 0.80,
                "latencyMs", 15,
                "costPerQuery", 0.0
        );

        System.out.println("  [local] Evaluated local/all-MiniLM-L6-v2: ndcg=0.80, latency=15ms");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("metrics", metrics);
        return result;
    }
}
