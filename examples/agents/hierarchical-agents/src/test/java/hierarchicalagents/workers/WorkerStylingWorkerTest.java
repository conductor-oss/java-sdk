package hierarchicalagents.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class WorkerStylingWorkerTest {

    private final WorkerStylingWorker worker = new WorkerStylingWorker();

    @Test
    void taskDefName() {
        assertEquals("hier_worker_styling", worker.getTaskDefName());
    }

    @SuppressWarnings("unchecked")
    @Test
    void producesDesignSystemResponsiveDarkModeCustomTheme() {
        Map<String, Object> stylingTask = Map.of(
                "designSystem", "Material UI",
                "responsive", true,
                "darkMode", true
        );
        Task task = taskWith(Map.of("task", stylingTask));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        Map<String, Object> stylingResult = (Map<String, Object>) result.getOutputData().get("result");
        assertEquals("Material UI", stylingResult.get("designSystem"));
        assertEquals(true, stylingResult.get("responsive"));
        assertEquals(true, stylingResult.get("darkMode"));
        assertEquals(true, stylingResult.get("customTheme"));
        assertEquals(280, stylingResult.get("linesOfCode"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void respectsFalseResponsiveAndDarkMode() {
        Map<String, Object> stylingTask = Map.of(
                "designSystem", "Tailwind",
                "responsive", false,
                "darkMode", false
        );
        Task task = taskWith(Map.of("task", stylingTask));
        TaskResult result = worker.execute(task);

        Map<String, Object> stylingResult = (Map<String, Object>) result.getOutputData().get("result");
        assertEquals("Tailwind", stylingResult.get("designSystem"));
        assertEquals(false, stylingResult.get("responsive"));
        assertEquals(false, stylingResult.get("darkMode"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void acceptsUiOutputContext() {
        Map<String, Object> stylingTask = Map.of(
                "designSystem", "Bootstrap",
                "responsive", true,
                "darkMode", false
        );
        Map<String, Object> uiOutput = Map.of("components", java.util.List.of());
        Task task = taskWith(Map.of("task", stylingTask, "uiOutput", uiOutput));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        Map<String, Object> stylingResult = (Map<String, Object>) result.getOutputData().get("result");
        assertEquals("Bootstrap", stylingResult.get("designSystem"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void handlesNullTask() {
        Task task = taskWith(new HashMap<>());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        Map<String, Object> stylingResult = (Map<String, Object>) result.getOutputData().get("result");
        assertEquals("Material UI", stylingResult.get("designSystem"));
        assertEquals(false, stylingResult.get("responsive"));
        assertEquals(false, stylingResult.get("darkMode"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
