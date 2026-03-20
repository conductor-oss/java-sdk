package xmlparsing.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ReceiveXmlWorkerTest {

    private final ReceiveXmlWorker worker = new ReceiveXmlWorker();

    @Test
    void taskDefName() {
        assertEquals("xp_receive_xml", worker.getTaskDefName());
    }

    @Test
    void receivesXmlContent() {
        Task task = taskWith(Map.of(
                "xmlContent", "<products><product/></products>",
                "rootElement", "products"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("<products><product/></products>", result.getOutputData().get("xml"));
        assertEquals("products", result.getOutputData().get("rootElement"));
    }

    @Test
    void outputContainsXmlSize() {
        String xml = "<data><item>test</item></data>";
        Task task = taskWith(Map.of(
                "xmlContent", xml,
                "rootElement", "data"));
        TaskResult result = worker.execute(task);

        assertEquals(xml.length(), result.getOutputData().get("xmlSize"));
    }

    @Test
    void passesRootElementThrough() {
        Task task = taskWith(Map.of(
                "xmlContent", "<catalog/>",
                "rootElement", "catalog"));
        TaskResult result = worker.execute(task);

        assertEquals("catalog", result.getOutputData().get("rootElement"));
    }

    @Test
    void xmlSizeMatchesContentLength() {
        String xml = "<root><a>1</a><b>2</b></root>";
        Task task = taskWith(Map.of(
                "xmlContent", xml,
                "rootElement", "root"));
        TaskResult result = worker.execute(task);

        assertEquals(29, result.getOutputData().get("xmlSize"));
    }

    @Test
    void handlesNullXmlContent() {
        Map<String, Object> input = new HashMap<>();
        input.put("xmlContent", null);
        input.put("rootElement", "root");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("", result.getOutputData().get("xml"));
        assertEquals(0, result.getOutputData().get("xmlSize"));
    }

    @Test
    void handlesNullRootElement() {
        Map<String, Object> input = new HashMap<>();
        input.put("xmlContent", "<test/>");
        input.put("rootElement", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("root", result.getOutputData().get("rootElement"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("", result.getOutputData().get("xml"));
        assertEquals("root", result.getOutputData().get("rootElement"));
        assertEquals(0, result.getOutputData().get("xmlSize"));
    }

    @Test
    void handlesEmptyXmlContent() {
        Task task = taskWith(Map.of(
                "xmlContent", "",
                "rootElement", "items"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("", result.getOutputData().get("xml"));
        assertEquals(0, result.getOutputData().get("xmlSize"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
