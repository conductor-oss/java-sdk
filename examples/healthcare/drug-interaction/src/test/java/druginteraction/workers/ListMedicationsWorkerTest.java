package druginteraction.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ListMedicationsWorkerTest {
    private final ListMedicationsWorker w = new ListMedicationsWorker();
    @Test void taskDefName() { assertEquals("drg_list_medications", w.getTaskDefName()); }
    @Test void listsMeds() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("patientId", "P1", "newMedication", "Aspirin")));
        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertNotNull(r.getOutputData().get("medications"));
    }
}
