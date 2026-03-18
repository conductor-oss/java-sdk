package legalcontractreview.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class LcrFinalizeWorkerTest {
    @Test void taskDefName() { assertEquals("lcr_finalize", new LcrFinalizeWorker().getTaskDefName()); }

    @Test void finalizesContract() {
        LcrFinalizeWorker w = new LcrFinalizeWorker();
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("contractId", "CTR-001", "riskFlags", List.of("flag1", "flag2"))));
        TaskResult r = w.execute(t);
        assertEquals(true, r.getOutputData().get("finalized"));
        assertEquals(2, r.getOutputData().get("riskFlagCount"));
    }
}
