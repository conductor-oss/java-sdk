package agentcollaboration.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ExecutorWorkerTest {

    private final ExecutorWorker worker = new ExecutorWorker();

    @Test
    void taskDefName() {
        assertEquals("ac_executor", worker.getTaskDefName());
    }

    @Test
    void returnsSixActionItems() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> actionItems =
                (List<Map<String, Object>>) result.getOutputData().get("actionItems");
        assertNotNull(actionItems);
        assertEquals(6, actionItems.size());
    }

    @Test
    void actionItemsHaveRequiredFields() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> actionItems =
                (List<Map<String, Object>>) result.getOutputData().get("actionItems");

        for (Map<String, Object> item : actionItems) {
            assertTrue(item.containsKey("id"), "missing id");
            assertTrue(item.containsKey("task"), "missing task");
            assertTrue(item.containsKey("owner"), "missing owner");
            assertTrue(item.containsKey("deadline"), "missing deadline");
            assertTrue(item.containsKey("priority"), "missing priority");
        }
    }

    @Test
    void actionItemIdsAreUnique() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> actionItems =
                (List<Map<String, Object>>) result.getOutputData().get("actionItems");

        long distinctIds = actionItems.stream()
                .map(i -> (String) i.get("id"))
                .distinct()
                .count();
        assertEquals(6, distinctIds);
    }

    @Test
    void returnsTimeline() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> timeline =
                (Map<String, Object>) result.getOutputData().get("timeline");
        assertNotNull(timeline);
        assertEquals(8, timeline.get("totalWeeks"));
    }

    @Test
    void timelineHasPhases() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> timeline =
                (Map<String, Object>) result.getOutputData().get("timeline");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> phases =
                (List<Map<String, Object>>) timeline.get("phases");
        assertNotNull(phases);
        assertEquals(3, phases.size());

        for (Map<String, Object> phase : phases) {
            assertTrue(phase.containsKey("name"), "missing name");
            assertTrue(phase.containsKey("weeks"), "missing weeks");
            assertTrue(phase.containsKey("focus"), "missing focus");
        }
    }

    @Test
    void returnsActionItemCount() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(6, result.getOutputData().get("actionItemCount"));
    }

    @Test
    void completesWithEmptyInput() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("actionItems"));
        assertNotNull(result.getOutputData().get("timeline"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
