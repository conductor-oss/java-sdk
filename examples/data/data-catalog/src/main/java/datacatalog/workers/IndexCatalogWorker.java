package datacatalog.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Indexes tagged assets into the catalog.
 * Input: taggedAssets
 * Output: indexedCount, catalogId, searchable
 */
public class IndexCatalogWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "cg_index_catalog";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<Map<String, Object>> assets = (List<Map<String, Object>>) task.getInputData().get("taggedAssets");
        if (assets == null) {
            assets = List.of();
        }

        String catalogId = "CAT-2024-0315";

        System.out.println("  [index] Indexed " + assets.size() + " assets into catalog " + catalogId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("indexedCount", assets.size());
        result.getOutputData().put("catalogId", catalogId);
        result.getOutputData().put("searchable", true);
        return result;
    }
}
