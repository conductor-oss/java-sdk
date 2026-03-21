package logprocessing.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class IngestLogsWorkerTest {

    private final IngestLogsWorker worker = new IngestLogsWorker();

    @Test
    void taskDefName() {
        assertEquals("lp_ingest_logs", worker.getTaskDefName());
    }

    @SuppressWarnings("unchecked")
    @Test
    void ingestsSevenLogEntries() {
        Task task = taskWith(Map.of("logSource", "prod-cluster-east"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(7, result.getOutputData().get("entryCount"));
        List<Map<String, Object>> rawLogs = (List<Map<String, Object>>) result.getOutputData().get("rawLogs");
        assertNotNull(rawLogs);
        assertEquals(7, rawLogs.size());
    }

    @SuppressWarnings("unchecked")
    @Test
    void logEntriesContainRequiredFields() {
        Task task = taskWith(Map.of("logSource", "test"));
        TaskResult result = worker.execute(task);

        List<Map<String, Object>> rawLogs = (List<Map<String, Object>>) result.getOutputData().get("rawLogs");
        Map<String, Object> first = rawLogs.get(0);
        assertNotNull(first.get("ts"));
        assertNotNull(first.get("level"));
        assertNotNull(first.get("service"));
        assertNotNull(first.get("msg"));
    }

    @Test
    void handlesDefaultSource() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(7, result.getOutputData().get("entryCount"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
