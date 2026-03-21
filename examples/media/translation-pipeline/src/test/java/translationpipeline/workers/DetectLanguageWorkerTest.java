package translationpipeline.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class DetectLanguageWorkerTest {
    private final DetectLanguageWorker worker = new DetectLanguageWorker();
    @Test void taskDefName() { assertEquals("trn_detect_language", worker.getTaskDefName()); }
    @Test void executesSuccessfully() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("id", "test-1")));
        TaskResult r = worker.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals("en", r.getOutputData().get("detectedLanguage"));
        assertNotNull(r.getOutputData().get("confidence"));
        assertNotNull(r.getOutputData().get("alternativeLanguages"));
    }
}
