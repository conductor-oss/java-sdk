package podcastworkflow.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class EditWorkerTest {
    private final EditWorker worker = new EditWorker();
    @Test void taskDefName() { assertEquals("pod_edit", worker.getTaskDefName()); }
    @Test void executesSuccessfully() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("id", "test-1")));
        TaskResult r = worker.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals("s3://media/podcasts/514/edited.mp3", r.getOutputData().get("editedAudioUrl"));
        assertEquals(42, r.getOutputData().get("finalDuration"));
        assertEquals(38, r.getOutputData().get("fileSizeMb"));
    }
}
