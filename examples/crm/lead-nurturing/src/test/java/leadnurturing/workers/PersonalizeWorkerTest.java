package leadnurturing.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class PersonalizeWorkerTest {
    private final PersonalizeWorker worker = new PersonalizeWorker();

    @Test void taskDefName() { assertEquals("nur_personalize", worker.getTaskDefName()); }

    @Test void personalizesContent() {
        TaskResult r = worker.execute(taskWith(Map.of("segment", "mid-funnel", "interests", List.of("automation"))));
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertNotNull(r.getOutputData().get("content"));
    }

    @SuppressWarnings("unchecked")
    @Test void contentHasSubject() {
        TaskResult r = worker.execute(taskWith(Map.of("segment", "top-funnel", "interests", List.of("analytics"))));
        Map<String, Object> content = (Map<String, Object>) r.getOutputData().get("content");
        assertEquals("Explore analytics", content.get("subject"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t;
    }
}
