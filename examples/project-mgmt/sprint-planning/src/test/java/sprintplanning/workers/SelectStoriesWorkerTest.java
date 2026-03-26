package sprintplanning.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap; import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class SelectStoriesWorkerTest {
    private final SelectStoriesWorker w = new SelectStoriesWorker();
    @Test void taskDefName() { assertEquals("spn_select_stories", w.getTaskDefName()); }
    @Test void executes() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("sprintNumber","14","teamCapacity","20","stories","[]","estimatedStories","[]","assignments","[]")));
        assertEquals(TaskResult.Status.COMPLETED, w.execute(t).getStatus());
    }
}
