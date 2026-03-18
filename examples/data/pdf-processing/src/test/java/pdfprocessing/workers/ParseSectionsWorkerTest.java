package pdfprocessing.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ParseSectionsWorkerTest {

    private final ParseSectionsWorker worker = new ParseSectionsWorker();

    @Test
    void taskDefName() {
        assertEquals("pd_parse_sections", worker.getTaskDefName());
    }

    @SuppressWarnings("unchecked")
    @Test
    void parsesThreeSections() {
        String text = "Chapter 1: Intro\nSome intro text.\n\nChapter 2: Middle\nMiddle content.\n\nChapter 3: End\nEnd content.";
        Task task = taskWith(Map.of("rawText", text, "pageCount", 3));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        List<Map<String, String>> sections = (List<Map<String, String>>) result.getOutputData().get("sections");
        assertEquals(3, sections.size());
    }

    @Test
    void sectionCountMatchesList() {
        String text = "Chapter 1: Intro\nContent here.\n\nChapter 2: Body\nMore content.";
        Task task = taskWith(Map.of("rawText", text));
        TaskResult result = worker.execute(task);

        int count = (int) result.getOutputData().get("sectionCount");
        assertEquals(2, count);
    }

    @SuppressWarnings("unchecked")
    @Test
    void sectionHasTitleAndContent() {
        String text = "Chapter 1: Introduction\nThis is the intro.";
        Task task = taskWith(Map.of("rawText", text));
        TaskResult result = worker.execute(task);

        List<Map<String, String>> sections = (List<Map<String, String>>) result.getOutputData().get("sections");
        assertEquals("Introduction", sections.get(0).get("title"));
        assertEquals("This is the intro.", sections.get(0).get("content"));
    }

    @Test
    void handlesEmptyText() {
        Task task = taskWith(Map.of("rawText", ""));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("sectionCount"));
    }

    @Test
    void handlesNullText() {
        Map<String, Object> input = new HashMap<>();
        input.put("rawText", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("sectionCount"));
    }

    @Test
    void handlesMissingText() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("sectionCount"));
    }

    @Test
    void handlesTextWithNoChapters() {
        Task task = taskWith(Map.of("rawText", "Just some plain text without any chapter markers."));
        TaskResult result = worker.execute(task);

        assertEquals(0, result.getOutputData().get("sectionCount"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
