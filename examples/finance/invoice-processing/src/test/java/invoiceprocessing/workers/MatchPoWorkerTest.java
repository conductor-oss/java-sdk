package invoiceprocessing.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class MatchPoWorkerTest {
    private final MatchPoWorker worker = new MatchPoWorker();

    @Test void taskDefName() { assertEquals("ivc_match_po", worker.getTaskDefName()); }

    @Test void computesVariance() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("extractedPoNumber", "PO-1234", "extractedAmount", 8750)));
        TaskResult r = worker.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertNotNull(r.getOutputData().get("variance"));
        assertNotNull(r.getOutputData().get("variancePercent"));
        assertTrue(r.getOutputData().get("matched") instanceof Boolean);
    }

    // ---- Failure path ---------------------------------------------------

    @Test void failsWithTerminalErrorOnMissingPoNumber() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("extractedAmount", 8750)));
        TaskResult r = worker.execute(t);
        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, r.getStatus());
        assertTrue(r.getReasonForIncompletion().contains("extractedPoNumber"));
    }

    @Test void failsWithTerminalErrorOnMissingAmount() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("extractedPoNumber", "PO-1234")));
        TaskResult r = worker.execute(t);
        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, r.getStatus());
        assertTrue(r.getReasonForIncompletion().contains("extractedAmount"));
    }

    @Test void failsWithTerminalErrorOnNegativeAmount() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("extractedPoNumber", "PO-1234", "extractedAmount", -100)));
        TaskResult r = worker.execute(t);
        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, r.getStatus());
        assertTrue(r.getReasonForIncompletion().contains("positive"));
    }

    @Test void failsWithTerminalErrorOnZeroAmount() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("extractedPoNumber", "PO-1234", "extractedAmount", 0)));
        TaskResult r = worker.execute(t);
        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, r.getStatus());
    }
}
