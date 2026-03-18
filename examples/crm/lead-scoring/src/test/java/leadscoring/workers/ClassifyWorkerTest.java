package leadscoring.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ClassifyWorkerTest {
    @Test void hotClassification() {
        ClassifyWorker w = new ClassifyWorker();
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("totalScore", 85)));
        TaskResult r = w.execute(t);
        assertEquals("hot", r.getOutputData().get("classification"));
        assertEquals("P1", r.getOutputData().get("priority"));
    }
}
