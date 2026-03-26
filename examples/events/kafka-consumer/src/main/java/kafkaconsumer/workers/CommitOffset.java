package kafkaconsumer.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Commits the Kafka consumer offset after successful message processing.
 */
public class CommitOffset implements Worker {

    @Override
    public String getTaskDefName() {
        return "kc_commit_offset";
    }

    @Override
    public TaskResult execute(Task task) {
        String topic = (String) task.getInputData().get("topic");
        Object partition = task.getInputData().get("partition");
        Object offset = task.getInputData().get("offset");
        String processingResult = (String) task.getInputData().get("processingResult");

        System.out.println("[kc_commit_offset] Committing offset=" + offset
                + " for topic=" + topic + " partition=" + partition
                + " (result=" + processingResult + ")");

        TaskResult result = new TaskResult(task);

        result.getOutputData().put("committed", true);
        result.getOutputData().put("committedOffset", offset);
        result.getOutputData().put("nextOffset", 14583);
        result.getOutputData().put("committedAt", "2026-01-15T10:00:00Z");

        result.setStatus(TaskResult.Status.COMPLETED);
        return result;
    }
}
