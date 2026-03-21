package userfeedback.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ClassifyFeedbackWorkerTest {
    private final ClassifyFeedbackWorker w = new ClassifyFeedbackWorker();
    @Test void taskDefName() { assertEquals("ufb_classify", w.getTaskDefName()); }
    @Test void classifiesBug() {
        TaskResult r = w.execute(t(Map.of("feedbackText", "I found a bug")));
        assertEquals("bug", r.getOutputData().get("category"));
    }
    @Test void classifiesFeature() {
        TaskResult r = w.execute(t(Map.of("feedbackText", "Please add a feature for X")));
        assertEquals("feature_request", r.getOutputData().get("category"));
    }
    @Test void classifiesGeneral() {
        TaskResult r = w.execute(t(Map.of("feedbackText", "Great product")));
        assertEquals("general", r.getOutputData().get("category"));
    }
    @Test void highPriorityForCrash() {
        TaskResult r = w.execute(t(Map.of("feedbackText", "The app crash when I click")));
        assertEquals("high", r.getOutputData().get("priority"));
    }
    @Test void mediumPriorityDefault() {
        TaskResult r = w.execute(t(Map.of("feedbackText", "Nice UI")));
        assertEquals("medium", r.getOutputData().get("priority"));
    }
    private Task t(Map<String, Object> i) { Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(i)); return t; }
}
