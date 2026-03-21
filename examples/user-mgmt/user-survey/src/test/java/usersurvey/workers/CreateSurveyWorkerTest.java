package usersurvey.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class CreateSurveyWorkerTest {
    private final CreateSurveyWorker w = new CreateSurveyWorker();
    @Test void taskDefName() { assertEquals("usv_create", w.getTaskDefName()); }
    @Test void createsSurvey() {
        TaskResult r = w.execute(t(Map.of("title", "Test Survey", "questions", List.of("Q1", "Q2"))));
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertTrue(r.getOutputData().get("surveyId").toString().startsWith("SRV-"));
        assertEquals(2, r.getOutputData().get("questionCount"));
    }
    @Test void handlesNoQuestions() {
        Map<String, Object> input = new HashMap<>(); input.put("title", "Empty"); input.put("questions", null);
        TaskResult r = w.execute(t(input));
        assertEquals(0, r.getOutputData().get("questionCount"));
    }
    private Task t(Map<String, Object> i) { Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(i)); return t; }
}
