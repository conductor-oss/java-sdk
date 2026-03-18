package ragcitation.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ExtractCitationsWorkerTest {

    private final ExtractCitationsWorker worker = new ExtractCitationsWorker();

    @Test
    void taskDefName() {
        assertEquals("cr_extract_citations", worker.getTaskDefName());
    }

    @Test
    void extractsCitationsFoundInAnswer() {
        String answer = "Conductor uses a task model [1] with multi-language workers [2].";
        List<Map<String, Object>> citations = new ArrayList<>();
        citations.add(new HashMap<>(Map.of("marker", "[1]", "docId", "doc-1", "claim", "task model")));
        citations.add(new HashMap<>(Map.of("marker", "[2]", "docId", "doc-2", "claim", "multi-language workers")));

        Task task = taskWith(new HashMap<>(Map.of("answer", answer, "citations", citations)));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> extracted = (List<Map<String, Object>>) result.getOutputData().get("extractedCitations");
        assertNotNull(extracted);
        assertEquals(2, extracted.size());
        assertEquals(true, extracted.get(0).get("foundInAnswer"));
        assertEquals(true, extracted.get(1).get("foundInAnswer"));
        assertEquals(2, result.getOutputData().get("citationCount"));
    }

    @Test
    void detectsMissingCitationMarkers() {
        String answer = "Conductor uses a task model [1] for workflows.";
        List<Map<String, Object>> citations = new ArrayList<>();
        citations.add(new HashMap<>(Map.of("marker", "[1]", "docId", "doc-1", "claim", "task model")));
        citations.add(new HashMap<>(Map.of("marker", "[3]", "docId", "doc-3", "claim", "missing claim")));

        Task task = taskWith(new HashMap<>(Map.of("answer", answer, "citations", citations)));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> extracted = (List<Map<String, Object>>) result.getOutputData().get("extractedCitations");

        assertEquals(true, extracted.get(0).get("foundInAnswer"));
        assertEquals(false, extracted.get(1).get("foundInAnswer"));
        assertEquals(1, result.getOutputData().get("citationCount"));
    }

    @Test
    void extractedCitationsHaveRequiredFields() {
        String answer = "Some text [1].";
        List<Map<String, Object>> citations = new ArrayList<>();
        citations.add(new HashMap<>(Map.of("marker", "[1]", "docId", "doc-1", "claim", "some claim")));

        Task task = taskWith(new HashMap<>(Map.of("answer", answer, "citations", citations)));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> extracted = (List<Map<String, Object>>) result.getOutputData().get("extractedCitations");

        for (Map<String, Object> item : extracted) {
            assertNotNull(item.get("marker"));
            assertNotNull(item.get("docId"));
            assertNotNull(item.get("claim"));
            assertNotNull(item.get("foundInAnswer"));
        }
    }

    @Test
    void handlesNullAnswer() {
        List<Map<String, Object>> citations = new ArrayList<>();
        citations.add(new HashMap<>(Map.of("marker", "[1]", "docId", "doc-1", "claim", "claim")));

        Task task = taskWith(new HashMap<>(Map.of("citations", citations)));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("citationCount"));
    }

    @Test
    void handlesNullCitations() {
        Task task = taskWith(new HashMap<>(Map.of("answer", "Some answer text")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> extracted = (List<Map<String, Object>>) result.getOutputData().get("extractedCitations");
        assertNotNull(extracted);
        assertTrue(extracted.isEmpty());
        assertEquals(0, result.getOutputData().get("citationCount"));
    }

    @Test
    void handlesEmptyInputs() {
        Task task = taskWith(new HashMap<>());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("citationCount"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
