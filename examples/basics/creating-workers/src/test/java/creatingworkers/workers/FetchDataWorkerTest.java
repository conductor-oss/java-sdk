package creatingworkers.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FetchDataWorkerTest {

    private final FetchDataWorker worker = new FetchDataWorker();

    @Test
    void taskDefName() {
        assertEquals("fetch_data", worker.getTaskDefName());
    }

    @Test
    void returnsRecords() {
        Task task = taskWith(Map.of("source", "test-api"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        Object records = result.getOutputData().get("records");
        assertNotNull(records);
        assertInstanceOf(List.class, records);
        assertEquals(3, ((List<?>) records).size());
    }

    @Test
    void recordsContainSourcePrefix() {
        Task task = taskWith(Map.of("source", "my-source"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> records = (List<Map<String, Object>>) result.getOutputData().get("records");
        assertEquals("my-source-record-1", records.get(0).get("value"));
    }

    @Test
    void outputHasSourceField() {
        Task task = taskWith(Map.of("source", "test-api"));
        TaskResult result = worker.execute(task);

        assertEquals("test-api", result.getOutputData().get("source"));
    }

    @Test
    void outputContainsRecordCount() {
        Task task = taskWith(Map.of("source", "test-api"));
        TaskResult result = worker.execute(task);

        assertEquals(3, result.getOutputData().get("recordCount"));
    }

    @Test
    void defaultsSourceWhenMissing() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("default", result.getOutputData().get("source"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
