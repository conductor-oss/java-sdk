package workflowtesting.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Checks assertions against expected output.
 * Input: actualOutput, expectedOutput
 * Output: assertions (list), allPassed (boolean)
 */
public class AssertWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "wft_assert";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        Map<String, Object> actual = (Map<String, Object>) task.getInputData().get("actualOutput");
        if (actual == null) {
            actual = Map.of();
        }
        Map<String, Object> expected = (Map<String, Object>) task.getInputData().get("expectedOutput");
        if (expected == null) {
            expected = Map.of();
        }

        System.out.println("  [assert] Checking assertions against expected output");

        List<Map<String, Object>> assertions = new ArrayList<>();

        Object expectedStatus = expected.get("status");
        Object actualStatus = actual.get("status");
        boolean statusPassed = expectedStatus != null && expectedStatus.equals(actualStatus);
        assertions.add(Map.of(
                "name", "status_check",
                "expected", String.valueOf(expectedStatus),
                "actual", String.valueOf(actualStatus),
                "passed", statusPassed
        ));

        Object expectedProcessed = expected.get("processed");
        Object actualProcessed = actual.get("processed");
        boolean countPassed = expectedProcessed != null
                && String.valueOf(expectedProcessed).equals(String.valueOf(actualProcessed));
        assertions.add(Map.of(
                "name", "count_check",
                "expected", String.valueOf(expectedProcessed),
                "actual", String.valueOf(actualProcessed),
                "passed", countPassed
        ));

        boolean allPassed = assertions.stream()
                .allMatch(a -> Boolean.TRUE.equals(a.get("passed")));

        long passedCount = assertions.stream()
                .filter(a -> Boolean.TRUE.equals(a.get("passed"))).count();
        System.out.println("    Assertions: " + passedCount + "/" + assertions.size() + " passed");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("assertions", assertions);
        result.getOutputData().put("allPassed", allPassed);
        return result;
    }
}
