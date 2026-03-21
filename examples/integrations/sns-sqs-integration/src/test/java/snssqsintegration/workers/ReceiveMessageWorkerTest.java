package snssqsintegration.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ReceiveMessageWorkerTest {

    private final ReceiveMessageWorker worker = new ReceiveMessageWorker();

    @Test
    void taskDefName() {
        assertEquals("sns_receive_message", worker.getTaskDefName());
    }

    @Test
    void executes() {
        Task task = taskWith(Map.of("queueUrl", "https://sqs.us-east-1.amazonaws.com/123/queue", "messageId", "sns-msg-123"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("receiptHandle"));
        assertEquals("sns-msg-123", result.getOutputData().get("body"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
