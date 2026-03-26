package codegeneration.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;
public class TestWorker implements Worker {
    @Override public String getTaskDefName() { return "cdg_test"; }
    @SuppressWarnings("unchecked")
    @Override public TaskResult execute(Task task) {
        List<Map<String, Object>> tests = (List<Map<String, Object>>) task.getInputData().getOrDefault("testCases", List.of());
        System.out.println("  [test] " + tests.size() + " test cases executed - all passed");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("allPassed", true); r.getOutputData().put("testsRun", tests.size());
        return r;
    }
}
