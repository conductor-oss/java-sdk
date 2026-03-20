package ragembeddingselection.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Worker that demonstrates embedding evaluation using Cohere embed-english-v3.0.
 * Returns pre-computed metrics for the benchmark dataset.
 */
public class EmbedCohereWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "es_embed_cohere";
    }

    @Override
    public TaskResult execute(Task task) {
        Map<String, Object> metrics = Map.of(
                "model", "cohere/embed-english-v3.0",
                "dimensions", 1024,
                "precisionAt1", 0.90,
                "precisionAt3", 0.87,
                "recallAt5", 0.92,
                "ndcg", 0.88,
                "latencyMs", 95,
                "costPerQuery", 0.0001
        );

        System.out.println("  [cohere] Evaluated cohere/embed-english-v3.0: ndcg=0.88, latency=95ms");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("metrics", metrics);
        return result;
    }
}
