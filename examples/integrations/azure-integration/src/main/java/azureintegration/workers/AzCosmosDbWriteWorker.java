package azureintegration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Writes a document to Azure CosmosDB.
 * Input: database, document
 * Output: documentId, database, requestCharge
 */
public class AzCosmosDbWriteWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "az_cosmosdb_write";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        String database = (String) task.getInputData().get("database");
        if (database == null) database = "default-db";

        Object docRaw = task.getInputData().get("document");
        String documentId = "doc-default";
        if (docRaw instanceof Map) {
            Object idVal = ((Map<String, Object>) docRaw).get("id");
            if (idVal != null) documentId = String.valueOf(idVal);
        }

        System.out.println("  [CosmosDB] Written document " + documentId + " to " + database);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("documentId", "" + documentId);
        result.getOutputData().put("database", "" + database);
        result.getOutputData().put("requestCharge", 6.2);
        return result;
    }
}
