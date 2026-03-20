package bulkuserimport.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class ParseFileWorkerTest {
    private final ParseFileWorker w = new ParseFileWorker();
    @Test void taskDefName() { assertEquals("bui_parse_file", w.getTaskDefName()); }
    @Test void parsesFile() {
        TaskResult r = w.execute(t(Map.of("fileUrl", "https://ex.com/f.csv", "format", "csv")));
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(1250, r.getOutputData().get("totalParsed"));
        assertNotNull(r.getOutputData().get("records"));
    }
    private Task t(Map<String, Object> i) { Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(i)); return t; }
}
