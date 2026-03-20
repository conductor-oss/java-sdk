package sentimentanalysis.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class AggregateWorkerTest {
    private final AggregateWorker worker = new AggregateWorker();
    @Test void taskDefName() { assertEquals("snt_aggregate", worker.getTaskDefName()); }
    @Test void completesSuccessfully() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        Map<String, Object> input = new HashMap<>();
        input.put("texts", List.of("good", "bad"));
        input.put("cleanedTexts", List.of("good", "bad"));
        input.put("sentiments", List.of(Map.of("text", "good", "score", 0.9, "label", "positive")));
        input.put("classifications", List.of(Map.of("text", "good", "label", "positive", "confidence", 0.9)));
        input.put("source", "test");
        t.setInputData(input);
        TaskResult r = worker.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
    }
}
