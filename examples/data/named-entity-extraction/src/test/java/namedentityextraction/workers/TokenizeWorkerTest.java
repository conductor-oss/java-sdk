package namedentityextraction.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class TokenizeWorkerTest {
    private final TokenizeWorker worker = new TokenizeWorker();
    @Test void taskDefName() { assertEquals("ner_tokenize", worker.getTaskDefName()); }
    @Test void completesSuccessfully() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        Map<String, Object> input = new HashMap<>();
        input.put("text", "Apple Inc. in California");
        input.put("tokens", List.of(Map.of("index", 0, "token", "Apple")));
        input.put("taggedTokens", List.of(Map.of("token", "Apple", "tag", "B-ORG")));
        input.put("entities", List.of(Map.of("text", "Apple Inc.", "type", "ORGANIZATION", "start", 0, "end", 1)));
        t.setInputData(input);
        TaskResult r = worker.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
    }
}
