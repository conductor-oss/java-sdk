package populationhealth.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class InterveneWorkerTest {
    private final InterveneWorker w = new InterveneWorker();
    @Test void taskDefName() { assertEquals("pop_intervene", w.getTaskDefName()); }
    @Test void intervenes() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("cohortId", "C1", "careGaps", List.of(Map.of("gap", "Overdue HbA1c")), "riskStrata", List.of())));
        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(2327, r.getOutputData().get("totalPatientsTargeted"));
    }
}
