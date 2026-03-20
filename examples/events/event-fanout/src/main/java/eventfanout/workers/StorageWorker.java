package eventfanout.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Stores the event payload to a data lake.
 * Input: eventId, payload
 * Output: result:"stored", storageLocation:"s3://events-lake/{eventId}"
 */
public class StorageWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "fo_storage";
    }

    @Override
    public TaskResult execute(Task task) {
        String eventId = (String) task.getInputData().get("eventId");
        if (eventId == null) {
            eventId = "unknown";
        }

        String storageLocation = "s3://events-lake/" + eventId;

        System.out.println("  [fo_storage] Storing event " + eventId + " to " + storageLocation);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("result", "stored");
        result.getOutputData().put("storageLocation", storageLocation);
        return result;
    }
}
