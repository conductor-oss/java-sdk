package documentqa.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class AnswerWorkerTest {
    private final AnswerWorker worker = new AnswerWorker();
    @Test void taskDefName() { assertEquals("dqa_answer", worker.getTaskDefName()); }
    @Test void completesSuccessfully() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        Map<String, Object> input = new HashMap<>();
        input.put("documentUrl", "https://test.com/doc.pdf"); input.put("document", Map.of("title", "test"));
        input.put("chunks", List.of(Map.of("id", 0, "text", "test"))); input.put("indexId", "IDX-1");
        input.put("question", "test?"); input.put("relevantChunks", List.of(Map.of("text", "test")));
        t.setInputData(input);
        assertEquals(TaskResult.Status.COMPLETED, worker.execute(t).getStatus());
    }
}
