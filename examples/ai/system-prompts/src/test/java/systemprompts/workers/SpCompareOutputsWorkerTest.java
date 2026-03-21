package systemprompts.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SpCompareOutputsWorkerTest {

    private final SpCompareOutputsWorker worker = new SpCompareOutputsWorker();

    private static final String FORMAL_RESPONSE =
            "Conductor is a distributed workflow orchestration engine that provides durable execution semantics, comprehensive observability, and declarative workflow definitions for enterprise-grade microservice coordination.";
    private static final String CASUAL_RESPONSE =
            "Conductor is basically a traffic controller for your code — it makes sure all your services talk to each other in the right order, and if something crashes, it picks up right where it left off.";

    @Test
    void taskDefName() {
        assertEquals("sp_compare_outputs", worker.getTaskDefName());
    }

    @Test
    void comparesResponseLengths() {
        Task task = taskWith(Map.of("formalResponse", FORMAL_RESPONSE, "casualResponse", CASUAL_RESPONSE));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        Map<String, Object> comparison = (Map<String, Object>) result.getOutputData().get("comparison");
        assertNotNull(comparison);

        assertEquals(FORMAL_RESPONSE.length(), comparison.get("formalLength"));
        assertEquals(CASUAL_RESPONSE.length(), comparison.get("casualLength"));
    }

    @Test
    void identifiesLongerStyle() {
        Task task = taskWith(Map.of("formalResponse", FORMAL_RESPONSE, "casualResponse", CASUAL_RESPONSE));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> comparison = (Map<String, Object>) result.getOutputData().get("comparison");

        String expectedLonger = FORMAL_RESPONSE.length() >= CASUAL_RESPONSE.length() ? "formal" : "casual";
        assertEquals(expectedLonger, comparison.get("longerStyle"));
    }

    @Test
    void includesInsight() {
        Task task = taskWith(Map.of("formalResponse", FORMAL_RESPONSE, "casualResponse", CASUAL_RESPONSE));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> comparison = (Map<String, Object>) result.getOutputData().get("comparison");
        assertEquals("System prompts dramatically change tone while preserving factual content", comparison.get("insight"));
    }

    @Test
    void handlesNullResponses() {
        Task task = taskWith(new HashMap<>(Map.of()));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        Map<String, Object> comparison = (Map<String, Object>) result.getOutputData().get("comparison");
        assertEquals(0, comparison.get("formalLength"));
        assertEquals(0, comparison.get("casualLength"));
        assertEquals("formal", comparison.get("longerStyle"));
    }

    @Test
    void casualLongerWhenCasualIsLonger() {
        String shortFormal = "Short.";
        String longCasual = "This is a much longer casual response that exceeds the formal one by a lot of characters.";

        Task task = taskWith(Map.of("formalResponse", shortFormal, "casualResponse", longCasual));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> comparison = (Map<String, Object>) result.getOutputData().get("comparison");
        assertEquals("casual", comparison.get("longerStyle"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
