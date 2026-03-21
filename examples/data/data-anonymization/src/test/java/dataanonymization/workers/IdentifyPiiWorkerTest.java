package dataanonymization.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class IdentifyPiiWorkerTest {

    private final IdentifyPiiWorker worker = new IdentifyPiiWorker();

    @Test
    void taskDefName() {
        assertEquals("an_identify_pii", worker.getTaskDefName());
    }

    @SuppressWarnings("unchecked")
    @Test
    void identifiesSixPiiFields() {
        Task task = taskWith(Map.of("dataset", List.of()));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(6, result.getOutputData().get("piiFieldCount"));
        List<Map<String, Object>> piiFields = (List<Map<String, Object>>) result.getOutputData().get("piiFields");
        assertEquals(6, piiFields.size());
    }

    @SuppressWarnings("unchecked")
    @Test
    void classifiesDirectAndQuasiIdentifiers() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        List<Map<String, Object>> piiFields = (List<Map<String, Object>>) result.getOutputData().get("piiFields");
        long directCount = piiFields.stream().filter(f -> "direct_identifier".equals(f.get("type"))).count();
        long quasiCount = piiFields.stream().filter(f -> "quasi_identifier".equals(f.get("type"))).count();
        assertEquals(4, directCount);
        assertEquals(2, quasiCount);
    }

    @SuppressWarnings("unchecked")
    @Test
    void assignsCorrectActions() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        List<Map<String, Object>> piiFields = (List<Map<String, Object>>) result.getOutputData().get("piiFields");
        long generalizeCount = piiFields.stream().filter(f -> "generalize".equals(f.get("action"))).count();
        long suppressCount = piiFields.stream().filter(f -> "suppress".equals(f.get("action"))).count();
        assertEquals(3, generalizeCount);
        assertEquals(3, suppressCount);
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
