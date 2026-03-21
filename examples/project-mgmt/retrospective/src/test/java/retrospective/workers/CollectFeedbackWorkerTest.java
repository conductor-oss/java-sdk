package retrospective.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class CollectFeedbackWorkerTest {
    private final CollectFeedbackWorker worker = new CollectFeedbackWorker();

    @Test void taskDefName() { assertEquals("rsp_collect_feedback", worker.getTaskDefName()); }

    @Test void collectsFeedback() {
        Task task = taskWith(Map.of("sprintId", "SPR-42", "teamName", "Platform Engineering"));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("feedback"));
        assertEquals(4, result.getOutputData().get("responseCount"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t;
    }
}
