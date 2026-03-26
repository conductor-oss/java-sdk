package populationhealth.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class AggregateDataWorkerTest {
    private final AggregateDataWorker w = new AggregateDataWorker();
    @Test void taskDefName() { assertEquals("pop_aggregate_data", w.getTaskDefName()); }
    @Test void aggregates() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("cohortId", "C1", "condition", "DM2", "reportingPeriod", "Q1")));
        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(2450, r.getOutputData().get("totalPatients"));
    }
}
