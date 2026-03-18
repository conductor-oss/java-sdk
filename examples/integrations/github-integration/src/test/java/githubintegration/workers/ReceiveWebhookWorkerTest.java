package githubintegration.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ReceiveWebhookWorkerTest {
    @Test void executesSuccessfully() {
        ReceiveWebhookWorker w = new ReceiveWebhookWorker();
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("repo", "owner/repo", "branch", "main",
                "commitMessage", "fix: bug", "head", "feature", "base", "main",
                "title", "Fix bug", "prNumber", 42, "sha", "abc123", "checksPass", true)));
        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
    }
}
