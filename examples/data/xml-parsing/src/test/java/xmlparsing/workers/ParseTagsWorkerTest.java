package xmlparsing.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ParseTagsWorkerTest {

    private final ParseTagsWorker worker = new ParseTagsWorker();

    @Test
    void taskDefName() {
        assertEquals("xp_parse_tags", worker.getTaskDefName());
    }

    @Test
    void returnsThreeElements() {
        Task task = taskWith(Map.of(
                "xml", "<products><product/></products>",
                "rootElement", "products"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> elements = (List<Map<String, Object>>) result.getOutputData().get("elements");
        assertEquals(3, elements.size());
    }

    @Test
    void elementCountIsThree() {
        Task task = taskWith(Map.of(
                "xml", "<items/>",
                "rootElement", "items"));
        TaskResult result = worker.execute(task);

        assertEquals(3, result.getOutputData().get("elementCount"));
    }

    @Test
    void firstElementIsLaptop() {
        Task task = taskWith(Map.of(
                "xml", "<products/>",
                "rootElement", "products"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> elements = (List<Map<String, Object>>) result.getOutputData().get("elements");
        Map<String, Object> first = elements.get(0);
        assertEquals("product", first.get("tag"));

        @SuppressWarnings("unchecked")
        Map<String, Object> attrs = (Map<String, Object>) first.get("attributes");
        assertEquals("P-101", attrs.get("id"));

        @SuppressWarnings("unchecked")
        Map<String, Object> children = (Map<String, Object>) first.get("children");
        assertEquals("Laptop", children.get("name"));
        assertEquals("999.99", children.get("price"));
        assertEquals("electronics", children.get("category"));
    }

    @Test
    void secondElementIsHeadphones() {
        Task task = taskWith(Map.of(
                "xml", "<products/>",
                "rootElement", "products"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> elements = (List<Map<String, Object>>) result.getOutputData().get("elements");
        Map<String, Object> second = elements.get(1);

        @SuppressWarnings("unchecked")
        Map<String, Object> attrs = (Map<String, Object>) second.get("attributes");
        assertEquals("P-102", attrs.get("id"));

        @SuppressWarnings("unchecked")
        Map<String, Object> children = (Map<String, Object>) second.get("children");
        assertEquals("Headphones", children.get("name"));
        assertEquals("149.99", children.get("price"));
        assertEquals("audio", children.get("category"));
    }

    @Test
    void thirdElementIsKeyboard() {
        Task task = taskWith(Map.of(
                "xml", "<products/>",
                "rootElement", "products"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> elements = (List<Map<String, Object>>) result.getOutputData().get("elements");
        Map<String, Object> third = elements.get(2);

        @SuppressWarnings("unchecked")
        Map<String, Object> attrs = (Map<String, Object>) third.get("attributes");
        assertEquals("P-103", attrs.get("id"));

        @SuppressWarnings("unchecked")
        Map<String, Object> children = (Map<String, Object>) third.get("children");
        assertEquals("Keyboard", children.get("name"));
        assertEquals("79.99", children.get("price"));
        assertEquals("peripherals", children.get("category"));
    }

    @Test
    void handlesNullXml() {
        Map<String, Object> input = new HashMap<>();
        input.put("xml", null);
        input.put("rootElement", "root");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(3, result.getOutputData().get("elementCount"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("elements"));
        assertEquals(3, result.getOutputData().get("elementCount"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
