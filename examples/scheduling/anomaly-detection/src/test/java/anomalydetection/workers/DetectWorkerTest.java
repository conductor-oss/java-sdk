package anomalydetection.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DetectWorkerTest {

    @Test
    void detectsAnomalyWithHighSensitivity() {
        DetectWorker w = new DetectWorker();
        Task t = new Task();
        t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of(
                "mean", 220, "stdDev", 35, "latestValue", 450, "sensitivity", "high"
        )));

        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(true, r.getOutputData().get("isAnomaly"));
        assertTrue(((Number) r.getOutputData().get("zScore")).doubleValue() > 2);
        assertEquals("above", r.getOutputData().get("direction"));
        assertEquals("z-score", r.getOutputData().get("method"));
    }

    @Test
    void normalValueNotAnomaly() {
        DetectWorker w = new DetectWorker();
        Task t = new Task();
        t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of(
                "mean", 220, "stdDev", 35, "latestValue", 230, "sensitivity", "medium"
        )));

        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(false, r.getOutputData().get("isAnomaly"));
        assertEquals("none", r.getOutputData().get("direction"));
    }

    @Test
    void detectsBelowBaselineAnomaly() {
        DetectWorker w = new DetectWorker();
        Task t = new Task();
        t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of(
                "mean", 220, "stdDev", 35, "latestValue", 50, "sensitivity", "high"
        )));

        TaskResult r = w.execute(t);
        assertEquals(true, r.getOutputData().get("isAnomaly"));
        assertEquals("below", r.getOutputData().get("direction"));
        assertTrue(((Number) r.getOutputData().get("zScore")).doubleValue() < 0);
    }

    @Test
    void failsWithMissingInputs() {
        DetectWorker w = new DetectWorker();
        Task t = new Task();
        t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>());

        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.FAILED, r.getStatus());
    }

    @Test
    void handlesZeroStdDev() {
        DetectWorker w = new DetectWorker();
        Task t = new Task();
        t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of(
                "mean", 100, "stdDev", 0, "latestValue", 105
        )));

        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(true, r.getOutputData().get("isAnomaly"));
    }

    @Test
    void respectsSensitivityThreshold() {
        // z-score = (300-220)/35 = 2.28
        // high sensitivity threshold = 2.0 -> anomaly
        // low sensitivity threshold = 3.5 -> not anomaly
        DetectWorker w = new DetectWorker();

        Task tHigh = new Task();
        tHigh.setStatus(Task.Status.IN_PROGRESS);
        tHigh.setInputData(new HashMap<>(Map.of(
                "mean", 220, "stdDev", 35, "latestValue", 300, "sensitivity", "high"
        )));
        TaskResult rHigh = w.execute(tHigh);
        assertEquals(true, rHigh.getOutputData().get("isAnomaly"));

        Task tLow = new Task();
        tLow.setStatus(Task.Status.IN_PROGRESS);
        tLow.setInputData(new HashMap<>(Map.of(
                "mean", 220, "stdDev", 35, "latestValue", 300, "sensitivity", "low"
        )));
        TaskResult rLow = w.execute(tLow);
        assertEquals(false, rLow.getOutputData().get("isAnomaly"));
    }
}
