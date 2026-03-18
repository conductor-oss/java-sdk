package jiraintegration.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class TrackStatusWorkerTest {
    @Test void executesSuccessfully() {
        TrackStatusWorker w = new TrackStatusWorker();
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("project", "PROJ", "summary", "Test issue",
                "description", "Test", "assignee", "dev@example.com",
                "issueKey", "PROJ-123", "fromStatus", "To Do", "toStatus", "In Progress",
                "newStatus", "In Progress")));
        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
    }
}
