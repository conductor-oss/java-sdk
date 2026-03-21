package druginteraction.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class FlagConflictsWorkerTest {
    private final FlagConflictsWorker w = new FlagConflictsWorker();
    @Test void taskDefName() { assertEquals("drg_flag_conflicts", w.getTaskDefName()); }
    @Test void flagsMajor() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        List<Map<String, Object>> interactions = new ArrayList<>();
        interactions.add(new HashMap<>(Map.of("drug1", "A", "drug2", "B", "severity", "major", "effect", "bad")));
        interactions.add(new HashMap<>(Map.of("drug1", "C", "drug2", "B", "severity", "none", "effect", "ok")));
        t.setInputData(new HashMap<>(Map.of("interactions", interactions)));
        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(1, r.getOutputData().get("conflictCount"));
    }
}
