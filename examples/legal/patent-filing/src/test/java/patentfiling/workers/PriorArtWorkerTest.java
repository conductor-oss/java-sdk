package patentfiling.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class PriorArtWorkerTest {
    @Test void taskDefName() { assertEquals("ptf_prior_art", new PriorArtWorker().getTaskDefName()); }
    @Test void executes() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(Map.of("inventionTitle", "Test", "inventors", "list", "description", "desc", "draftId", "D1", "applicationNumber", "A1", "priorArtClear", "true")));
        TaskResult r = new PriorArtWorker().execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertNotNull(r.getOutputData().get("priorArtClear"));
    }
}
