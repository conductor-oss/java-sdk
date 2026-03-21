package druginteraction.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class RecommendAlternativesWorkerTest {
    private final RecommendAlternativesWorker w = new RecommendAlternativesWorker();
    @Test void taskDefName() { assertEquals("drg_recommend_alternatives", w.getTaskDefName()); }
    @Test void recommendsWhenConflicts() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("conflicts", List.of(Map.of("drug1", "A")), "newMedication", "Aspirin")));
        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(true, r.getOutputData().get("reviewed"));
    }
}
