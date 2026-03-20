package hierarchicalagents.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Styling developer worker that applies the design system, responsive layout,
 * and theme support based on the styling task and the UI output.
 */
public class WorkerStylingWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "hier_worker_styling";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        Map<String, Object> stylingTask = (Map<String, Object>) task.getInputData().get("task");
        // uiOutput is available but used for context only
        task.getInputData().get("uiOutput");

        String designSystem = stylingTask != null ? (String) stylingTask.get("designSystem") : "Material UI";
        boolean responsive = stylingTask != null && Boolean.TRUE.equals(stylingTask.get("responsive"));
        boolean darkMode = stylingTask != null && Boolean.TRUE.equals(stylingTask.get("darkMode"));

        System.out.println("  [hier_worker_styling] Applying design system: " + designSystem);

        Map<String, Object> stylingResult = new LinkedHashMap<>();
        stylingResult.put("designSystem", designSystem);
        stylingResult.put("responsive", responsive);
        stylingResult.put("darkMode", darkMode);
        stylingResult.put("customTheme", true);
        stylingResult.put("linesOfCode", 280);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("result", stylingResult);
        return result;
    }
}
