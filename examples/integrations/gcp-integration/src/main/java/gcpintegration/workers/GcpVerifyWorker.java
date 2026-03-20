package gcpintegration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Verifies that all GCP services completed successfully.
 * Input: gcsObject, firestoreDoc, pubsubMsg
 * Output: verified (boolean)
 */
public class GcpVerifyWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "gcp_verify";
    }

    @Override
    public TaskResult execute(Task task) {
        String gcsObject = (String) task.getInputData().get("gcsObject");
        String firestoreDoc = (String) task.getInputData().get("firestoreDoc");
        String pubsubMsg = (String) task.getInputData().get("pubsubMsg");

        boolean allPresent = gcsObject != null && !gcsObject.isEmpty()
                && firestoreDoc != null && !firestoreDoc.isEmpty()
                && pubsubMsg != null && !pubsubMsg.isEmpty();

        System.out.println("  [verify] GCS: " + gcsObject + ", Firestore: " + firestoreDoc + ", PubSub: " + pubsubMsg);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("verified", allPresent);
        return result;
    }
}
