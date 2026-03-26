package awsintegration.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AwsSnsNotifyWorkerTest {

    private final AwsSnsNotifyWorker worker = new AwsSnsNotifyWorker();

    @Test
    void taskDefName() {
        assertEquals("aws_sns_notify", worker.getTaskDefName());
    }

    @Test
    void publishesToTopic() {
        Task task = taskWith(Map.of(
                "topicArn", "arn:aws:sns:us-east-1:123456789:data-events",
                "message", Map.of("id", "evt-5001")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("arn:aws:sns:us-east-1:123456789:data-events", result.getOutputData().get("topicArn"));
    }

    @Test
    void outputContainsMessageId() {
        Task task = taskWith(Map.of(
                "topicArn", "arn:aws:sns:us-east-1:123456789:test-topic",
                "message", "hello"));
        TaskResult result = worker.execute(task);

        assertNotNull(result.getOutputData().get("messageId"));
        assertEquals("sns-msg-fixed-001", result.getOutputData().get("messageId"));
    }

    @Test
    void handlesNullTopicArn() {
        Map<String, Object> input = new HashMap<>();
        input.put("topicArn", null);
        input.put("message", "test");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("arn:aws:sns:us-east-1:000000000:default", result.getOutputData().get("topicArn"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("messageId"));
    }

    @Test
    void topicArnPassedThrough() {
        Task task = taskWith(Map.of(
                "topicArn", "arn:aws:sns:eu-west-1:999:orders",
                "message", "order created"));
        TaskResult result = worker.execute(task);

        assertEquals("arn:aws:sns:eu-west-1:999:orders", result.getOutputData().get("topicArn"));
    }

    @Test
    void messageIdIsDeterministic() {
        Task task1 = taskWith(Map.of("topicArn", "arn:aws:sns:us-east-1:1:t1"));
        Task task2 = taskWith(Map.of("topicArn", "arn:aws:sns:us-east-1:2:t2"));
        TaskResult r1 = worker.execute(task1);
        TaskResult r2 = worker.execute(task2);

        assertEquals(r1.getOutputData().get("messageId"), r2.getOutputData().get("messageId"));
    }

    @Test
    void completesWithMapMessage() {
        Task task = taskWith(Map.of(
                "topicArn", "arn:aws:sns:us-east-1:123:topic",
                "message", Map.of("event", "signup", "userId", "u-1")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
