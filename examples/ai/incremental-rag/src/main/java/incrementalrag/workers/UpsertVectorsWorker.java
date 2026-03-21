package incrementalrag.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Worker that upserts embedding vectors into the vector store,
 * counting inserts vs updates based on the action field.
 */
public class UpsertVectorsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ir_upsert_vectors";
    }

    @Override
    @SuppressWarnings("unchecked")
    public TaskResult execute(Task task) {
        List<Map<String, Object>> embeddings = (List<Map<String, Object>>) task.getInputData().get("embeddings");
        List<String> docIds = (List<String>) task.getInputData().get("docIds");

        int inserted = 0;
        int updated = 0;

        for (Map<String, Object> embedding : embeddings) {
            String action = (String) embedding.get("action");
            if ("insert".equals(action)) {
                inserted++;
            } else if ("update".equals(action)) {
                updated++;
            }
        }

        System.out.println("  [upsert_vectors] Upserted " + embeddings.size()
                + " (inserted: " + inserted + ", updated: " + updated + ")");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("upsertedCount", embeddings.size());
        result.getOutputData().put("docIds", docIds);
        result.getOutputData().put("inserted", inserted);
        result.getOutputData().put("updated", updated);
        return result;
    }
}
