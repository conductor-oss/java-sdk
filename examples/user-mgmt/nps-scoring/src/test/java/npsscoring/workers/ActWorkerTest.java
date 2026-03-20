package npsscoring.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ActWorkerTest {

    private final ActWorker worker = new ActWorker();

    @Test
    void taskDefName() {
        assertEquals("nps_act", worker.getTaskDefName());
    }

    @Test
    void triggersActions() {
        Task task = taskWith(Map.of(
                "npsScore", 45,
                "segments", List.of(
                        Map.of("name", "Promoters", "action", "referral_program"),
                        Map.of("name", "Passives", "action", "engagement_boost"),
                        Map.of("name", "Detractors", "action", "outreach_call")
                )
        ));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("actionsTriggered"));
    }

    @Test
    void triggersThreeActions() {
        Task task = taskWith(Map.of(
                "npsScore", 45,
                "segments", List.of(Map.of("name", "Promoters"))
        ));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> actions = (List<Map<String, Object>>) result.getOutputData().get("actionsTriggered");
        assertEquals(3, actions.size());
        assertEquals(3, result.getOutputData().get("totalActions"));
    }

    @Test
    void allActionsAreTriggered() {
        Task task = taskWith(Map.of("npsScore", 45, "segments", List.of()));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> actions = (List<Map<String, Object>>) result.getOutputData().get("actionsTriggered");
        for (Map<String, Object> action : actions) {
            assertEquals("triggered", action.get("status"));
        }
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
