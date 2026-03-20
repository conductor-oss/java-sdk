package textclassification.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class ClassifyWorkerTest {
    private final ClassifyWorker worker = new ClassifyWorker();
    @Test void taskDefName() { assertEquals("txc_classify", worker.getTaskDefName()); }
    @Test void completesSuccessfully() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        Map<String, Object> input = new HashMap<>();
        input.put("text", "test text about technology");
        input.put("cleanedText", "test text about technology");
        input.put("features", Map.of("wordCount", 4));
        input.put("prediction", "technology");
        input.put("scores", Map.of("technology", 0.87, "science", 0.72));
        t.setInputData(input);
        TaskResult r = worker.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
    }
}
