package apitestgeneration.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;
public class RunTestsWorker implements Worker {
    @Override public String getTaskDefName() { return "atg_run_tests"; }
    @SuppressWarnings("unchecked")
    @Override public TaskResult execute(Task task) {
        List<Map<String, Object>> suite = (List<Map<String, Object>>) task.getInputData().getOrDefault("testSuite", List.of());
        int total = suite.size() * 3;
        int passed = total - 1;
        System.out.println("  [run] Executed " + total + " tests — " + passed + " passed, 1 failed");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("results", Map.of("total", total, "passed", passed, "failed", 1));
        result.getOutputData().put("passRate", Math.round((double) passed / total * 100) + "%");
        return result;
    }
}
