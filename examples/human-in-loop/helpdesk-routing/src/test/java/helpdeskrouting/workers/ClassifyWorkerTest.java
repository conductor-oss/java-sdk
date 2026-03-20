package helpdeskrouting.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ClassifyWorkerTest {
    private final ClassifyWorker worker = new ClassifyWorker();
    @Test void taskDefName() { assertEquals("hdr_classify", worker.getTaskDefName()); }
    @Test void classifiesTier2() {
        TaskResult r = worker.execute(taskWith(Map.of("description", "API integration error", "customerId", "C1")));
        assertEquals("tier2", r.getOutputData().get("tier"));
    }
    @Test void classifiesTier3() {
        TaskResult r = worker.execute(taskWith(Map.of("description", "Complete outage of production", "customerId", "C1")));
        assertEquals("tier3", r.getOutputData().get("tier"));
    }
    @Test void defaultsTier1() {
        TaskResult r = worker.execute(taskWith(Map.of("description", "How do I reset my password", "customerId", "C1")));
        assertEquals("tier1", r.getOutputData().get("tier"));
    }
    private Task taskWith(Map<String, Object> input) { Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t; }
}
