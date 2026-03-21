package emailcampaign.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class PersonalizeWorkerTest {
    private final PersonalizeWorker worker = new PersonalizeWorker();
    @Test void taskDefName() { assertEquals("eml_personalize", worker.getTaskDefName()); }
    @Test void executesSuccessfully() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("id", "test-1", "recipientCount", 500)));
        TaskResult r = worker.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertNotNull(r.getOutputData().get("personalizedCount"));
        assertEquals(3, r.getOutputData().get("variantsCreated"));
        assertNotNull(r.getOutputData().get("mergeFieldsUsed"));
    }
}
