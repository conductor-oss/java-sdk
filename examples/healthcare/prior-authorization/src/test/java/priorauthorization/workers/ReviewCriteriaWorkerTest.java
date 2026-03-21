package priorauthorization.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ReviewCriteriaWorkerTest {
    private final ReviewCriteriaWorker w = new ReviewCriteriaWorker();
    @Test void taskDefName() { assertEquals("pa_review_criteria", w.getTaskDefName()); }
    @Test void approvesWhenMedicallyNecessary() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("authId", "A1", "procedure", "MRI", "clinicalReason", "medically necessary")));
        TaskResult r = w.execute(t);
        assertEquals("approve", r.getOutputData().get("decision"));
        assertEquals(90, r.getOutputData().get("validDays"));
    }
    @Test void deniesCosmetic() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("authId", "A2", "procedure", "facelift", "clinicalReason", "cosmetic improvement")));
        TaskResult r = w.execute(t);
        assertEquals("deny", r.getOutputData().get("decision"));
    }
}
