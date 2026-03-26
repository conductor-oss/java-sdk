package awsintegration.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AwsDynamoDbWriteWorkerTest {

    private final AwsDynamoDbWriteWorker worker = new AwsDynamoDbWriteWorker();

    @Test
    void taskDefName() {
        assertEquals("aws_dynamodb_write", worker.getTaskDefName());
    }

    @Test
    void writesItemWithIdFromPayload() {
        Task task = taskWith(Map.of(
                "tableName", "events-table",
                "item", Map.of("id", "evt-5001", "type", "order.created")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("evt-5001", result.getOutputData().get("itemId"));
        assertEquals("events-table", result.getOutputData().get("tableName"));
    }

    @Test
    void outputContainsConsumedCapacity() {
        Task task = taskWith(Map.of(
                "tableName", "test-table",
                "item", Map.of("id", "item-1")));
        TaskResult result = worker.execute(task);

        assertEquals(5, result.getOutputData().get("consumedCapacity"));
    }

    @Test
    void handlesItemWithoutId() {
        Task task = taskWith(Map.of(
                "tableName", "test-table",
                "item", Map.of("type", "order.created")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("item-default", result.getOutputData().get("itemId"));
    }

    @Test
    void handlesNullTableName() {
        Map<String, Object> input = new HashMap<>();
        input.put("tableName", null);
        input.put("item", Map.of("id", "evt-100"));
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("default-table", result.getOutputData().get("tableName"));
    }

    @Test
    void handlesNullItem() {
        Map<String, Object> input = new HashMap<>();
        input.put("tableName", "my-table");
        input.put("item", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("item-default", result.getOutputData().get("itemId"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("default-table", result.getOutputData().get("tableName"));
        assertEquals("item-default", result.getOutputData().get("itemId"));
    }

    @Test
    void writesItemWithNumericId() {
        Task task = taskWith(Map.of(
                "tableName", "users-table",
                "item", Map.of("id", 42, "name", "Alice")));
        TaskResult result = worker.execute(task);

        assertEquals("42", result.getOutputData().get("itemId"));
        assertEquals("users-table", result.getOutputData().get("tableName"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
