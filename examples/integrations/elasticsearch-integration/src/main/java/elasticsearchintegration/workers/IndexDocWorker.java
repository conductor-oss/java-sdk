package elasticsearchintegration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Indexes a document in Elasticsearch.
 * Input: indexName, document
 * Output: documentId, result, version
 */
public class IndexDocWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "els_index_doc";
    }

    @Override
    public TaskResult execute(Task task) {
        String indexName = (String) task.getInputData().get("indexName");
        String documentId = "doc-" + Long.toString(System.currentTimeMillis(), 36);
        System.out.println("  [index] Indexed " + documentId + " into " + indexName);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("documentId", documentId);
        result.getOutputData().put("result", "created");
        result.getOutputData().put("version", 1);
        return result;
    }
}
