package livestreaming.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MonitorQualityWorkerTest {

    private final MonitorQualityWorker worker = new MonitorQualityWorker();

    @Test void taskDefName() { assertEquals("lsm_monitor_quality", worker.getTaskDefName()); }

    @Test void monitorsQuality() {
        TaskResult r = worker.execute(taskWith(Map.of("streamId","S-1","playbackUrl","https://test","viewerCount",2450)));
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
    }

    @Test void returnsPeakViewers() {
        TaskResult r = worker.execute(taskWith(Map.of("streamId","S-2","viewerCount",1000)));
        assertEquals(3200, r.getOutputData().get("peakViewers"));
    }

    @Test void returnsAvgBitrate() {
        TaskResult r = worker.execute(taskWith(Map.of("streamId","S-3","viewerCount",500)));
        assertEquals("4.2 Mbps", r.getOutputData().get("avgBitrate"));
    }

    @Test void returnsDuration() {
        TaskResult r = worker.execute(taskWith(Map.of("streamId","S-4","viewerCount",100)));
        assertEquals(5500, r.getOutputData().get("duration"));
    }

    @Test void returnsQualityScore() {
        TaskResult r = worker.execute(taskWith(Map.of("streamId","S-5","viewerCount",3000)));
        assertEquals(94, r.getOutputData().get("qualityScore"));
    }

    @Test void returnsBufferRatio() {
        TaskResult r = worker.execute(taskWith(Map.of("streamId","S-6","viewerCount",2000)));
        assertEquals(0.02, r.getOutputData().get("bufferRatio"));
    }

    @Test void handlesMissingInputs() {
        TaskResult r = worker.execute(taskWith(Map.of()));
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertNotNull(r.getOutputData().get("peakViewers"));
    }

    private Task taskWith(Map<String,Object> input) {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t;
    }
}
