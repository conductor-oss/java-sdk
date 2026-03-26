package bulkuserimport.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class BatchInsertWorkerTest {
    private final BatchInsertWorker w = new BatchInsertWorker();
    @Test void taskDefName() { assertEquals("bui_batch_insert", w.getTaskDefName()); }
    @Test void inserts() {
        TaskResult r = w.execute(t(Map.of("validRecords", List.of())));
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(1238, r.getOutputData().get("insertedCount"));
        assertEquals(13, r.getOutputData().get("batches"));
    }
    private Task t(Map<String, Object> i) { Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(i)); return t; }
}
