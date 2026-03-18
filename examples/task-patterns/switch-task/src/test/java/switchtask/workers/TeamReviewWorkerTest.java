package switchtask.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TeamReviewWorkerTest {

    private final TeamReviewWorker worker = new TeamReviewWorker();

    @Test
    void taskDefName() {
        assertEquals("sw_team_review", worker.getTaskDefName());
    }

    @Test
    void handlesMediumPriorityTicket() {
        Task task = taskWith(Map.of("ticketId", "TKT-002", "priority", "MEDIUM", "description", "Perf issue"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("team", result.getOutputData().get("handler"));
        assertEquals("support-team-1", result.getOutputData().get("assignedTo"));
    }

    @Test
    void outputContainsExactlyTwoFields() {
        Task task = taskWith(Map.of("ticketId", "TKT-020", "priority", "MEDIUM", "description", "Test"));
        TaskResult result = worker.execute(task);

        assertEquals(2, result.getOutputData().size());
        assertTrue(result.getOutputData().containsKey("handler"));
        assertTrue(result.getOutputData().containsKey("assignedTo"));
    }

    @Test
    void assignedToIsAlwaysSupportTeam1() {
        Task task = taskWith(Map.of("ticketId", "TKT-814", "priority", "MEDIUM", "description", "Any issue"));
        TaskResult result = worker.execute(task);

        assertEquals("support-team-1", result.getOutputData().get("assignedTo"));
    }

    @Test
    void handlerIsAlwaysTeam() {
        Task task = taskWith(Map.of("ticketId", "TKT-888", "priority", "MEDIUM", "description", "Another issue"));
        TaskResult result = worker.execute(task);

        assertEquals("team", result.getOutputData().get("handler"));
    }

    @Test
    void handlesNullTicketId() {
        Map<String, Object> input = new HashMap<>();
        input.put("ticketId", null);
        input.put("priority", "MEDIUM");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("team", result.getOutputData().get("handler"));
    }

    @Test
    void handlesEmptyInput() {
        Task task = taskWith(new HashMap<>());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("team", result.getOutputData().get("handler"));
        assertEquals("support-team-1", result.getOutputData().get("assignedTo"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
