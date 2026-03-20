package questionanswering.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class GenerateAnswerWorkerTest {
    private final GenerateAnswerWorker worker = new GenerateAnswerWorker();
    @Test void taskDefName() { assertEquals("qas_generate_answer", worker.getTaskDefName()); }
    @Test void completesSuccessfully() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("question", "How to configure?", "knowledgeBase", "docs", "parsedQuestion", Map.of(), "context", List.of("passage"))));
        assertEquals(TaskResult.Status.COMPLETED, worker.execute(t).getStatus());
    }
}
