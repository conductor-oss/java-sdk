package elasticsearchintegration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Searches documents in Elasticsearch.
 * Input: indexName, query
 * Output: hits, totalHits, took
 */
public class SearchWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "els_search";
    }

    @Override
    public TaskResult execute(Task task) {
        String query = (String) task.getInputData().get("query");
        String indexName = (String) task.getInputData().get("indexName");

        java.util.List<java.util.Map<String, Object>> hits = java.util.List.of(
                java.util.Map.of("id", "doc-1", "score", 9.5, "title", "Product Analytics Q4", "category", "analytics"),
                java.util.Map.of("id", "doc-2", "score", 8.2, "title", "User Behavior Report", "category", "analytics"),
                java.util.Map.of("id", "doc-3", "score", 7.1, "title", "Sales Dashboard Setup", "category", "sales"));
        System.out.println("  [search] Query \"" + query + "\" -> " + hits.size() + " hits in " + indexName);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("hits", hits);
        result.getOutputData().put("totalHits", hits.size());
        result.getOutputData().put("took", 12);
        return result;
    }
}
