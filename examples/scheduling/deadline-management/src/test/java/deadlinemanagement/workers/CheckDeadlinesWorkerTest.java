package deadlinemanagement.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CheckDeadlinesWorkerTest {

    @Test
    void classifiesOverdueTask() {
        CheckDeadlinesWorker w = new CheckDeadlinesWorker();
        Task t = new Task();
        t.setStatus(Task.Status.IN_PROGRESS);

        // Due date in the past
        String pastDue = Instant.now().minus(48, ChronoUnit.HOURS).toString();
        t.setInputData(new HashMap<>(Map.of("taskId", "TASK-1", "dueDate", pastDue, "projectId", "proj-alpha")));

        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals("overdue", r.getOutputData().get("urgency"));
        assertTrue(((Number) r.getOutputData().get("hoursOverdue")).longValue() >= 47);
        assertEquals(0L, r.getOutputData().get("hoursRemaining"));
    }

    @Test
    void classifiesUrgentTask() {
        CheckDeadlinesWorker w = new CheckDeadlinesWorker();
        Task t = new Task();
        t.setStatus(Task.Status.IN_PROGRESS);

        // Due in 4 hours
        String soonDue = Instant.now().plus(4, ChronoUnit.HOURS).toString();
        t.setInputData(new HashMap<>(Map.of("taskId", "TASK-2", "dueDate", soonDue, "projectId", "proj-beta")));

        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals("urgent", r.getOutputData().get("urgency"));
        assertTrue(((Number) r.getOutputData().get("hoursRemaining")).longValue() >= 3);
    }

    @Test
    void classifiesNormalTask() {
        CheckDeadlinesWorker w = new CheckDeadlinesWorker();
        Task t = new Task();
        t.setStatus(Task.Status.IN_PROGRESS);

        // Due in 5 days
        String farDue = Instant.now().plus(5 * 24, ChronoUnit.HOURS).toString();
        t.setInputData(new HashMap<>(Map.of("taskId", "TASK-3", "dueDate", farDue)));

        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals("normal", r.getOutputData().get("urgency"));
        assertTrue(((Number) r.getOutputData().get("hoursRemaining")).longValue() > 24);
    }

    @Test
    void failsWithMissingDueDate() {
        CheckDeadlinesWorker w = new CheckDeadlinesWorker();
        Task t = new Task();
        t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("taskId", "TASK-4")));

        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.FAILED, r.getStatus());
        assertTrue(r.getReasonForIncompletion().contains("dueDate"));
    }

    @Test
    void failsWithInvalidDueDateFormat() {
        CheckDeadlinesWorker w = new CheckDeadlinesWorker();
        Task t = new Task();
        t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("taskId", "TASK-5", "dueDate", "not-a-date")));

        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.FAILED, r.getStatus());
        assertTrue(r.getReasonForIncompletion().contains("Invalid dueDate format"));
    }
}
