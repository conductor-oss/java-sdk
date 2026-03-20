package testgeneration.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
public class GenerateTestsWorker implements Worker {
    @Override public String getTaskDefName() { return "tge_generate_tests"; }
    @SuppressWarnings("unchecked")
    @Override public TaskResult execute(Task task) {
        List<Map<String, Object>> fns = (List<Map<String, Object>>) task.getInputData().getOrDefault("functions", List.of());
        List<Map<String, Object>> tests = new ArrayList<>();
        for (Map<String, Object> f : fns) {
            String name = (String) f.get("name");
            tests.add(Map.of("functionName", name, "testCases",
                List.of("test_" + name + "_happy_path", "test_" + name + "_edge_case", "test_" + name + "_null_input")));
        }
        int testCount = tests.size() * 3;
        System.out.println("  [generate] Created " + testCount + " test cases for " + fns.size() + " functions");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("tests", tests);
        result.getOutputData().put("testCount", testCount);
        return result;
    }
}
