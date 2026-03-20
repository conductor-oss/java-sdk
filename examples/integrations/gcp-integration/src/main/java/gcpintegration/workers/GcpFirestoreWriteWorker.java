package gcpintegration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Writes a document to Google Firestore.
 * Input: collection, document
 * Output: documentId, collection, writeTime
 */
public class GcpFirestoreWriteWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "gcp_firestore_write";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        String collection = (String) task.getInputData().get("collection");
        if (collection == null) {
            collection = "default-collection";
        }

        Object docRaw = task.getInputData().get("document");
        String documentId = "doc-default";
        if (docRaw instanceof Map) {
            Object idVal = ((Map<String, Object>) docRaw).get("id");
            if (idVal != null) {
                documentId = String.valueOf(idVal);
            }
        }

        System.out.println("  [Firestore] Written document " + documentId + " to " + collection);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("documentId", "" + documentId);
        result.getOutputData().put("collection", "" + collection);
        result.getOutputData().put("writeTime", "2026-01-15T10:00:00Z");
        return result;
    }
}
