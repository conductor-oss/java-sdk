package usersurvey.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class CollectResponsesWorkerTest {
    private final CollectResponsesWorker w = new CollectResponsesWorker();
    @Test void taskDefName() { assertEquals("usv_collect", w.getTaskDefName()); }
    @Test void collectsResponses() {
        TaskResult r = w.execute(t(Map.of("surveyId", "SRV-123")));
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(847, r.getOutputData().get("responseCount"));
        assertEquals("33.9%", r.getOutputData().get("responseRate"));
    }
    @Test void includesResponses() {
        TaskResult r = w.execute(t(Map.of("surveyId", "SRV-123")));
        assertNotNull(r.getOutputData().get("responses"));
    }
    private Task t(Map<String, Object> i) { Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(i)); return t; }
}
