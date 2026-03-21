package ehrintegration.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class MergeRecordsWorkerTest {
    private final MergeRecordsWorker w = new MergeRecordsWorker();
    @Test void taskDefName() { assertEquals("ehr_merge_records", w.getTaskDefName()); }
    @Test void merges() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("patientId", "P1", "records", List.of(Map.of("source", "a"), Map.of("source", "b")))));
        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertNotNull(r.getOutputData().get("mergedRecord"));
        assertEquals(2, r.getOutputData().get("sourceCount"));
    }
}
