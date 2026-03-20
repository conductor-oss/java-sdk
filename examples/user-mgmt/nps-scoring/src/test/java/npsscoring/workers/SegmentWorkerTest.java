package npsscoring.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SegmentWorkerTest {

    private final SegmentWorker worker = new SegmentWorker();

    @Test
    void taskDefName() {
        assertEquals("nps_segment", worker.getTaskDefName());
    }

    @Test
    void segmentsUsers() {
        Task task = taskWith(Map.of("promoters", 912, "passives", 300, "detractors", 228));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("segments"));
    }

    @Test
    void createsThreeSegments() {
        Task task = taskWith(Map.of("promoters", 912, "passives", 300, "detractors", 228));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> segments = (List<Map<String, Object>>) result.getOutputData().get("segments");
        assertEquals(3, segments.size());
        assertEquals(3, result.getOutputData().get("totalSegments"));
    }

    @Test
    void segmentsContainActions() {
        Task task = taskWith(Map.of("promoters", 100, "passives", 50, "detractors", 30));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> segments = (List<Map<String, Object>>) result.getOutputData().get("segments");
        assertEquals("referral_program", segments.get(0).get("action"));
        assertEquals("engagement_boost", segments.get(1).get("action"));
        assertEquals("outreach_call", segments.get(2).get("action"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
