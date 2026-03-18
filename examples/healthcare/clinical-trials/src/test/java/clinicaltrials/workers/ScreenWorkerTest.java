package clinicaltrials.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ScreenWorkerTest {
    private final ScreenWorker w = new ScreenWorker();

    @Test void taskDefName() { assertEquals("clt_screen", w.getTaskDefName()); }

    @Test void eligiblePatient() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("participantId", "P1", "condition", "hypertension", "age", 35)));
        TaskResult r = w.execute(t);
        assertEquals(true, r.getOutputData().get("eligible"));
    }

    @Test void tooYoung() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("participantId", "P2", "condition", "hypertension", "age", 16)));
        TaskResult r = w.execute(t);
        assertEquals(false, r.getOutputData().get("eligible"));
    }

    @Test void excludedCondition() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("participantId", "P3", "condition", "pregnancy", "age", 30)));
        TaskResult r = w.execute(t);
        assertEquals(false, r.getOutputData().get("eligible"));
    }
}
