package podcastworkflow.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class RecordWorkerTest {
    private final RecordWorker worker = new RecordWorker();
    @Test void taskDefName() { assertEquals("pod_record", worker.getTaskDefName()); }
    @Test void executesSuccessfully() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("id", "test-1")));
        TaskResult r = worker.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals("s3://media/podcasts/514/raw.wav", r.getOutputData().get("rawAudioUrl"));
        assertEquals(48, r.getOutputData().get("durationMinutes"));
        assertEquals(48000, r.getOutputData().get("sampleRate"));
    }
}
