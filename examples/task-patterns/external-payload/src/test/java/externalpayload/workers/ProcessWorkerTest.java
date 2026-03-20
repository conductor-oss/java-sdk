package externalpayload.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ProcessWorkerTest {

    private final ProcessWorker worker = new ProcessWorker();

    @Test
    void taskDefName() {
        assertEquals("ep_process", worker.getTaskDefName());
    }

    @Test
    void processesRecordsFromSummary() {
        Map<String, Object> summary = Map.of(
                "recordCount", 10000,
                "avgSize", 1024,
                "totalBytes", 10240000L
        );

        Task task = taskWith(Map.of("summary", summary));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("Processed 10000 records", result.getOutputData().get("result"));
    }

    @Test
    void handlesSmallRecordCount() {
        Map<String, Object> summary = Map.of(
                "recordCount", 1,
                "avgSize", 1024,
                "totalBytes", 1024L
        );

        Task task = taskWith(Map.of("summary", summary));
        TaskResult result = worker.execute(task);

        assertEquals("Processed 1 records", result.getOutputData().get("result"));
    }

    @Test
    void handlesNullSummary() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("Processed 0 records", result.getOutputData().get("result"));
    }

    @Test
    void handlesZeroRecordCount() {
        Map<String, Object> summary = Map.of(
                "recordCount", 0,
                "avgSize", 1024,
                "totalBytes", 0L
        );

        Task task = taskWith(Map.of("summary", summary));
        TaskResult result = worker.execute(task);

        assertEquals("Processed 0 records", result.getOutputData().get("result"));
    }

    @Test
    void handlesLargeRecordCount() {
        Map<String, Object> summary = Map.of(
                "recordCount", 1000000,
                "avgSize", 1024,
                "totalBytes", 1024000000L
        );

        Task task = taskWith(Map.of("summary", summary));
        TaskResult result = worker.execute(task);

        assertEquals("Processed 1000000 records", result.getOutputData().get("result"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
