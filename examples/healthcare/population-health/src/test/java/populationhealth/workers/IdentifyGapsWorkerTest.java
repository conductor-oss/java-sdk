package populationhealth.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class IdentifyGapsWorkerTest {
    private final IdentifyGapsWorker w = new IdentifyGapsWorker();
    @Test void taskDefName() { assertEquals("pop_identify_gaps", w.getTaskDefName()); }
    @Test void identifiesGaps() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("cohortId", "C1", "riskStrata", List.of())));
        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(4, r.getOutputData().get("totalGaps"));
    }
}
