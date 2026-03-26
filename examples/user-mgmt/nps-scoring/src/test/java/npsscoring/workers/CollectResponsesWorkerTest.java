package npsscoring.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CollectResponsesWorkerTest {

    private final CollectResponsesWorker worker = new CollectResponsesWorker();

    @Test
    void taskDefName() {
        assertEquals("nps_collect_responses", worker.getTaskDefName());
    }

    @Test
    void collectsResponses() {
        Task task = taskWith(Map.of("campaignId", "NPS-Q4", "period", "2024-Q4"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("responses"));
        assertTrue(result.getOutputData().get("responses") instanceof List);
    }

    @Test
    void returnsTotalResponses() {
        Task task = taskWith(Map.of("campaignId", "NPS-Q4", "period", "2024-Q4"));
        TaskResult result = worker.execute(task);
        assertEquals(1520, result.getOutputData().get("totalResponses"));
    }

    @Test
    void responsesHaveExpectedCount() {
        Task task = taskWith(Map.of("campaignId", "NPS-Q4", "period", "2024-Q4"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> responses = (List<Map<String, Object>>) result.getOutputData().get("responses");
        assertEquals(1520, responses.size());
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
