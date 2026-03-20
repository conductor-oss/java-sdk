package apitestgeneration.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
public class GenerateTestsWorker implements Worker {
    @Override public String getTaskDefName() { return "atg_generate_tests"; }
    @SuppressWarnings("unchecked")
    @Override public TaskResult execute(Task task) {
        List<Map<String, Object>> eps = (List<Map<String, Object>>) task.getInputData().getOrDefault("endpoints", List.of());
        List<Map<String, Object>> testSuite = new ArrayList<>();
        for (Map<String, Object> ep : eps) {
            testSuite.add(Map.of("endpoint", ep.get("method") + " " + ep.get("path"),
                "cases", List.of("happy_path", "invalid_input", "auth_required")));
        }
        int testCount = testSuite.size() * 3;
        System.out.println("  [generate] Generated " + testCount + " test cases");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("testSuite", testSuite);
        result.getOutputData().put("testCount", testCount);
        return result;
    }
}
