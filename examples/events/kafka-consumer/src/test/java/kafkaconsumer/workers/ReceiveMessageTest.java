package kafkaconsumer.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class ReceiveMessageTest {

    private final ReceiveMessage worker = new ReceiveMessage();

    @Test
    void taskDefName() {
        assertEquals("kc_receive_message", worker.getTaskDefName());
    }

    @Test
    void completesSuccessfully() {
        Task task = taskWith("user-events", 3, "14582", "U-4421", "{\"action\":\"update\"}");
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void outputContainsRawMessage() {
        Task task = taskWith("user-events", 3, "14582", "U-4421", "{\"action\":\"update\"}");
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> rawMessage = (Map<String, Object>) result.getOutputData().get("rawMessage");
        assertNotNull(rawMessage);
        assertEquals("U-4421", rawMessage.get("key"));
        assertEquals("{\"action\":\"update\"}", rawMessage.get("value"));
    }

    @Test
    void rawMessageContainsHeaders() {
        Task task = taskWith("user-events", 3, "14582", "U-4421", "{\"action\":\"update\"}");
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> rawMessage = (Map<String, Object>) result.getOutputData().get("rawMessage");
        @SuppressWarnings("unchecked")
        Map<String, Object> headers = (Map<String, Object>) rawMessage.get("headers");
        assertNotNull(headers);
        assertEquals("application/json", headers.get("content-type"));
        assertEquals("corr-88221", headers.get("correlation-id"));
    }

    @Test
    void rawMessageContainsTimestamp() {
        Task task = taskWith("user-events", 3, "14582", "U-4421", "{\"action\":\"update\"}");
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> rawMessage = (Map<String, Object>) result.getOutputData().get("rawMessage");
        assertEquals(1710900000000L, rawMessage.get("timestamp"));
    }

    @Test
    void outputContainsFormatJson() {
        Task task = taskWith("user-events", 3, "14582", "U-4421", "{\"action\":\"update\"}");
        TaskResult result = worker.execute(task);
        assertEquals("json", result.getOutputData().get("format"));
    }

    @Test
    void messageKeyPreservedInOutput() {
        Task task = taskWith("orders", 0, "100", "order-123", "{\"id\":\"order-123\"}");
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> rawMessage = (Map<String, Object>) result.getOutputData().get("rawMessage");
        assertEquals("order-123", rawMessage.get("key"));
    }

    @Test
    void messageValuePreservedInOutput() {
        String value = "{\"complex\":true,\"nested\":{\"a\":1}}";
        Task task = taskWith("events", 1, "50", "key1", value);
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> rawMessage = (Map<String, Object>) result.getOutputData().get("rawMessage");
        assertEquals(value, rawMessage.get("value"));
    }

    @Test
    void deterministicOutputForSameInput() {
        Task task1 = taskWith("topic-a", 2, "999", "k1", "v1");
        Task task2 = taskWith("topic-a", 2, "999", "k1", "v1");

        TaskResult r1 = worker.execute(task1);
        TaskResult r2 = worker.execute(task2);

        assertEquals(r1.getOutputData().get("rawMessage"), r2.getOutputData().get("rawMessage"));
        assertEquals(r1.getOutputData().get("format"), r2.getOutputData().get("format"));
    }

    private Task taskWith(String topic, int partition, String offset, String messageKey, String messageValue) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        Map<String, Object> input = new HashMap<>();
        input.put("topic", topic);
        input.put("partition", partition);
        input.put("offset", offset);
        input.put("messageKey", messageKey);
        input.put("messageValue", messageValue);
        task.setInputData(input);
        return task;
    }
}
