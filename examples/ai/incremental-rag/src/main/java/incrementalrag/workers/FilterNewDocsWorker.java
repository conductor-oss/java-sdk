package incrementalrag.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Worker that separates changed documents into new (no existing hash)
 * and updated (has existing hash) categories, producing a list of
 * documents to embed with their action type.
 */
public class FilterNewDocsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ir_filter_new_docs";
    }

    @Override
    @SuppressWarnings("unchecked")
    public TaskResult execute(Task task) {
        List<String> changedDocIds = (List<String>) task.getInputData().get("changedDocIds");
        Map<String, String> existingHashes = (Map<String, String>) task.getInputData().get("existingHashes");

        List<Map<String, String>> docsToEmbed = new ArrayList<>();
        int newCount = 0;
        int updatedCount = 0;

        for (String docId : changedDocIds) {
            String hash = existingHashes.get(docId);
            if (hash == null) {
                docsToEmbed.add(Map.of(
                        "id", docId,
                        "text", "Content of " + docId,
                        "action", "insert"
                ));
                newCount++;
            } else {
                docsToEmbed.add(Map.of(
                        "id", docId,
                        "text", "Updated content of " + docId,
                        "action", "update"
                ));
                updatedCount++;
            }
        }

        System.out.println("  [filter_new_docs] New: " + newCount + ", updated: " + updatedCount);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("docsToEmbed", docsToEmbed);
        result.getOutputData().put("newCount", newCount);
        result.getOutputData().put("updatedCount", updatedCount);
        return result;
    }
}
