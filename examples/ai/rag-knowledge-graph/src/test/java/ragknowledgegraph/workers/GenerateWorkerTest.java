package ragknowledgegraph.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GenerateWorkerTest {

    private final GenerateWorker worker = new GenerateWorker();

    @Test
    void taskDefName() {
        assertEquals("kg_generate", worker.getTaskDefName());
    }

    @Test
    void generatesAnswerWithContext() {
        Map<String, Object> mergedContext = new HashMap<>(Map.of(
                "graphSummary", "Conductor developed_by Netflix",
                "vectorSummary", "Netflix created Conductor in 2016",
                "totalSources", 11
        ));
        List<Map<String, String>> entities = List.of(
                Map.of("name", "Conductor", "type", "Software", "id", "entity-1"),
                Map.of("name", "Netflix", "type", "Organization", "id", "entity-2"),
                Map.of("name", "microservices", "type", "Architecture", "id", "entity-3"),
                Map.of("name", "workflow orchestration", "type", "Concept", "id", "entity-4")
        );

        Task task = taskWith(new HashMap<>(Map.of(
                "question", "What is Conductor?",
                "mergedContext", mergedContext,
                "entities", entities
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        String answer = (String) result.getOutputData().get("answer");
        assertNotNull(answer);
        assertTrue(answer.contains("Conductor"));
        assertTrue(answer.contains("Netflix"));
        assertTrue(answer.contains("11 sources"));
        assertTrue(answer.contains("4 identified entities"));
    }

    @Test
    void answerContainsConductorHistory() {
        Map<String, Object> mergedContext = new HashMap<>(Map.of(
                "graphSummary", "summary",
                "vectorSummary", "docs",
                "totalSources", 5
        ));

        Task task = taskWith(new HashMap<>(Map.of(
                "question", "Tell me about Conductor",
                "mergedContext", mergedContext
        )));
        TaskResult result = worker.execute(task);

        String answer = (String) result.getOutputData().get("answer");
        assertTrue(answer.contains("workflow orchestration engine"));
        assertTrue(answer.contains("2016"));
        assertTrue(answer.contains("Orkes"));
        assertTrue(answer.contains("JSON"));
    }

    @Test
    void handlesNullMergedContext() {
        Task task = taskWith(new HashMap<>(Map.of(
                "question", "test"
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String answer = (String) result.getOutputData().get("answer");
        assertNotNull(answer);
        assertTrue(answer.contains("0 sources"));
    }

    @Test
    void handlesNullEntities() {
        Map<String, Object> mergedContext = new HashMap<>(Map.of(
                "totalSources", 3
        ));

        Task task = taskWith(new HashMap<>(Map.of(
                "question", "test",
                "mergedContext", mergedContext
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String answer = (String) result.getOutputData().get("answer");
        assertTrue(answer.contains("0 identified entities"));
    }

    @Test
    void handlesMissingQuestion() {
        Task task = taskWith(new HashMap<>());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("answer"));
    }

    @Test
    void sourcesAndEntitiesReflectInput() {
        Map<String, Object> mergedContext = new HashMap<>(Map.of(
                "totalSources", 7
        ));
        List<Map<String, String>> entities = List.of(
                Map.of("name", "A", "type", "B", "id", "1"),
                Map.of("name", "C", "type", "D", "id", "2")
        );

        Task task = taskWith(new HashMap<>(Map.of(
                "question", "test",
                "mergedContext", mergedContext,
                "entities", entities
        )));
        TaskResult result = worker.execute(task);

        String answer = (String) result.getOutputData().get("answer");
        assertTrue(answer.contains("7 sources"));
        assertTrue(answer.contains("2 identified entities"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
