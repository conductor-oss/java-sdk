package npsscoring.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CalculateNpsWorkerTest {

    private final CalculateNpsWorker worker = new CalculateNpsWorker();

    @Test
    void taskDefName() {
        assertEquals("nps_calculate", worker.getTaskDefName());
    }

    @Test
    void calculatesNpsScore() {
        Task task = taskWith(Map.of("responses", List.of(Map.of("score", 9), Map.of("score", 3))));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("npsScore"));
    }

    @Test
    void returnsPromotersPassivesDetractors() {
        Task task = taskWith(Map.of("responses", List.of(Map.of("score", 9))));
        TaskResult result = worker.execute(task);

        assertEquals(912, result.getOutputData().get("promoters"));
        assertEquals(300, result.getOutputData().get("passives"));
        assertEquals(228, result.getOutputData().get("detractors"));
    }

    @Test
    void npsScoreIsReasonable() {
        Task task = taskWith(Map.of("responses", List.of(Map.of("score", 10))));
        TaskResult result = worker.execute(task);

        int nps = ((Number) result.getOutputData().get("npsScore")).intValue();
        assertTrue(nps >= -100 && nps <= 100, "NPS should be between -100 and 100");
    }

    @Test
    void handlesEmptyResponses() {
        Task task = taskWith(Map.of("responses", List.of()));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
