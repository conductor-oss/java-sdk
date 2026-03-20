package azureintegration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * perform  publishing an event to Azure Event Hub.
 * Input: eventHub, event
 * Output: sequenceNumber, eventHub
 */
public class AzEventHubPublishWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "az_eventhub_publish";
    }

    @Override
    public TaskResult execute(Task task) {
        String eventHub = (String) task.getInputData().get("eventHub");
        if (eventHub == null) eventHub = "default-hub";

        long sequenceNumber = 1700000000000L;

        System.out.println("  [EventHub] Published event #" + sequenceNumber + " to " + eventHub);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("sequenceNumber", sequenceNumber);
        result.getOutputData().put("eventHub", "" + eventHub);
        return result;
    }
}
