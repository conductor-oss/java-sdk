package fanoutfanin.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PrepareWorkerTest {

    private final PrepareWorker worker = new PrepareWorker();

    @Test
    void taskDefName() {
        assertEquals("fo_prepare", worker.getTaskDefName());
    }

    @Test
    void preparesTasksForMultipleImages() {
        Task task = taskWith(Map.of("images", List.of(
                Map.of("name", "hero.jpg", "size", 2400),
                Map.of("name", "banner.png", "size", 3600),
                Map.of("name", "thumb.jpg", "size", 800)
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> dynamicTasks =
                (List<Map<String, Object>>) result.getOutputData().get("dynamicTasks");
        assertNotNull(dynamicTasks);
        assertEquals(3, dynamicTasks.size());

        // Verify first task
        assertEquals("fo_process_image", dynamicTasks.get(0).get("name"));
        assertEquals("img_0_ref", dynamicTasks.get(0).get("taskReferenceName"));
        assertEquals("SIMPLE", dynamicTasks.get(0).get("type"));

        // Verify second task
        assertEquals("fo_process_image", dynamicTasks.get(1).get("name"));
        assertEquals("img_1_ref", dynamicTasks.get(1).get("taskReferenceName"));

        // Verify third task
        assertEquals("img_2_ref", dynamicTasks.get(2).get("taskReferenceName"));
    }

    @Test
    void preparesInputMapForMultipleImages() {
        Task task = taskWith(Map.of("images", List.of(
                Map.of("name", "hero.jpg", "size", 2400),
                Map.of("name", "banner.png", "size", 3600)
        )));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Map<String, Object>> dynamicTasksInput =
                (Map<String, Map<String, Object>>) result.getOutputData().get("dynamicTasksInput");
        assertNotNull(dynamicTasksInput);
        assertEquals(2, dynamicTasksInput.size());

        // Verify input for first task
        Map<String, Object> input0 = dynamicTasksInput.get("img_0_ref");
        assertNotNull(input0);
        @SuppressWarnings("unchecked")
        Map<String, Object> image0 = (Map<String, Object>) input0.get("image");
        assertEquals("hero.jpg", image0.get("name"));
        assertEquals(2400, image0.get("size"));
        assertEquals(0, input0.get("index"));

        // Verify input for second task
        Map<String, Object> input1 = dynamicTasksInput.get("img_1_ref");
        assertNotNull(input1);
        @SuppressWarnings("unchecked")
        Map<String, Object> image1 = (Map<String, Object>) input1.get("image");
        assertEquals("banner.png", image1.get("name"));
        assertEquals(1, input1.get("index"));
    }

    @Test
    void preparesSingleImage() {
        Task task = taskWith(Map.of("images", List.of(
                Map.of("name", "only.jpg", "size", 1000)
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> dynamicTasks =
                (List<Map<String, Object>>) result.getOutputData().get("dynamicTasks");
        assertEquals(1, dynamicTasks.size());
        assertEquals("img_0_ref", dynamicTasks.get(0).get("taskReferenceName"));

        @SuppressWarnings("unchecked")
        Map<String, Map<String, Object>> dynamicTasksInput =
                (Map<String, Map<String, Object>>) result.getOutputData().get("dynamicTasksInput");
        assertEquals(1, dynamicTasksInput.size());
        assertNotNull(dynamicTasksInput.get("img_0_ref"));
    }

    @Test
    void handlesEmptyImagesList() {
        Task task = taskWith(Map.of("images", List.of()));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> dynamicTasks =
                (List<Map<String, Object>>) result.getOutputData().get("dynamicTasks");
        assertNotNull(dynamicTasks);
        assertTrue(dynamicTasks.isEmpty());

        @SuppressWarnings("unchecked")
        Map<String, Map<String, Object>> dynamicTasksInput =
                (Map<String, Map<String, Object>>) result.getOutputData().get("dynamicTasksInput");
        assertNotNull(dynamicTasksInput);
        assertTrue(dynamicTasksInput.isEmpty());
    }

    @Test
    void handlesNullImages() {
        Task task = taskWith(new HashMap<>());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> dynamicTasks =
                (List<Map<String, Object>>) result.getOutputData().get("dynamicTasks");
        assertNotNull(dynamicTasks);
        assertTrue(dynamicTasks.isEmpty());
    }

    @Test
    void taskReferencesAreDeterministic() {
        List<Map<String, Object>> images = List.of(
                Map.of("name", "a.jpg", "size", 100),
                Map.of("name", "b.png", "size", 200)
        );
        Task task1 = taskWith(Map.of("images", images));
        TaskResult result1 = worker.execute(task1);

        Task task2 = taskWith(Map.of("images", images));
        TaskResult result2 = worker.execute(task2);

        assertEquals(result1.getOutputData().get("dynamicTasks"),
                result2.getOutputData().get("dynamicTasks"));
        assertEquals(result1.getOutputData().get("dynamicTasksInput"),
                result2.getOutputData().get("dynamicTasksInput"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
