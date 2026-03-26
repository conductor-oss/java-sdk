package billingmedical.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class CodeProceduresWorkerTest {
    private final CodeProceduresWorker w = new CodeProceduresWorker();
    @Test void taskDefName() { assertEquals("mbl_code_procedures", w.getTaskDefName()); }
    @Test void codesProcedures() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("encounterId", "E1", "procedures", List.of("visit"))));
        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(250, r.getOutputData().get("totalCharge"));
    }
}
