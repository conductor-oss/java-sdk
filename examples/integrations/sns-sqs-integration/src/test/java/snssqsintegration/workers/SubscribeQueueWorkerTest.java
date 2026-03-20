package snssqsintegration.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SubscribeQueueWorkerTest {

    private final SubscribeQueueWorker worker = new SubscribeQueueWorker();

    @Test
    void taskDefName() {
        assertEquals("sns_subscribe_queue", worker.getTaskDefName());
    }

    @Test
    void executes() {
        Task task = taskWith(Map.of("topicArn", "arn:aws:sns:us-east-1:123:topic", "queueUrl", "https://sqs.us-east-1.amazonaws.com/123/queue"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("subscriptionArn"));
        assertEquals("sqs", result.getOutputData().get("protocol"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
