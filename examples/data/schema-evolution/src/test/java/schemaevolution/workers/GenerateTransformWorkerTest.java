package schemaevolution.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GenerateTransformWorkerTest {

    private final GenerateTransformWorker worker = new GenerateTransformWorker();

    @Test
    void taskDefName() {
        assertEquals("sh_generate_transform", worker.getTaskDefName());
    }

    @SuppressWarnings("unchecked")
    @Test
    void generatesAddColumnTransform() {
        List<Map<String, Object>> changes = List.of(
                Map.of("type", "ADD_FIELD", "field", "middle_name", "dataType", "string"));
        Task task = taskWith(Map.of("changes", changes));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        List<Map<String, Object>> transforms = (List<Map<String, Object>>) result.getOutputData().get("transforms");
        assertEquals(1, transforms.size());
        assertEquals("addColumn", transforms.get(0).get("action"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void generatesRenameTransform() {
        List<Map<String, Object>> changes = List.of(
                Map.of("type", "RENAME_FIELD", "from", "phone", "to", "phone_number"));
        Task task = taskWith(Map.of("changes", changes));
        TaskResult result = worker.execute(task);

        List<Map<String, Object>> transforms = (List<Map<String, Object>>) result.getOutputData().get("transforms");
        assertEquals("renameColumn", transforms.get(0).get("action"));
        assertEquals("phone", transforms.get(0).get("from"));
        assertEquals("phone_number", transforms.get(0).get("to"));
    }

    @Test
    void handlesNullChanges() {
        Map<String, Object> input = new HashMap<>();
        input.put("changes", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("transformCount"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
