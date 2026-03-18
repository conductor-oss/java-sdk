package gdprcompliance;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import gdprcompliance.workers.ProcessRequestWorker;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class MainExampleTest {
    @Test void detectsEmailPII() {
        ProcessRequestWorker w = new ProcessRequestWorker();
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("requestType", "access",
                "data", "Contact john@example.com or call 555-123-4567")));
        TaskResult r = w.execute(t);
        assertTrue(((Number) r.getOutputData().get("piiCount")).intValue() >= 1);
    }

    @Test void masksDataOnErasure() {
        ProcessRequestWorker w = new ProcessRequestWorker();
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("requestType", "erasure",
                "data", "Email: test@example.com, SSN: 123-45-6789")));
        TaskResult r = w.execute(t);
        String processed = (String) r.getOutputData().get("processedData");
        assertFalse(processed.contains("test@example.com"));
        assertFalse(processed.contains("123-45-6789"));
    }
}
