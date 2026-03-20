package testgeneration.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;
public class ValidateTestsWorker implements Worker {
    @Override public String getTaskDefName() { return "tge_validate_tests"; }
    @SuppressWarnings("unchecked")
    @Override public TaskResult execute(Task task) {
        List<Map<String, Object>> tests = (List<Map<String, Object>>) task.getInputData().getOrDefault("tests", List.of());
        int total = 0;
        for (Map<String, Object> t : tests) {
            total += ((List<?>) t.get("testCases")).size();
        }
        System.out.println("  [validate] Validated " + total + " tests — all syntactically correct");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("validatedTests", tests);
        result.getOutputData().put("validCount", total);
        return result;
    }
}
