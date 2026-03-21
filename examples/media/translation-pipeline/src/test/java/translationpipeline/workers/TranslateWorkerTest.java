package translationpipeline.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class TranslateWorkerTest {
    private final TranslateWorker worker = new TranslateWorker();
    @Test void taskDefName() { assertEquals("trn_translate", worker.getTaskDefName()); }
    @Test void executesSuccessfully() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("id", "test-1")));
        TaskResult r = worker.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals("Automatisez vos flux de travail avec Conductor pour une orchestration efficace.", r.getOutputData().get("translatedText"));
        assertNotNull(r.getOutputData().get("qualityScore"));
        assertEquals(11, r.getOutputData().get("wordCount"));
    }
}
