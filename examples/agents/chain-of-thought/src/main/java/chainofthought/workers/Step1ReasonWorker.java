package chainofthought.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Takes the problem understanding and produces a reasoning step identifying
 * the formula and variables to use.
 */
public class Step1ReasonWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ct_step_1_reason";
    }

    @Override
    public TaskResult execute(Task task) {
        String problem = (String) task.getInputData().get("problem");
        String understanding = (String) task.getInputData().get("understanding");

        System.out.println("  [ct_step_1_reason] Reasoning about: " + understanding);

        Map<String, Object> variables = Map.of(
                "P", 10000,
                "r", 0.05,
                "t", 3
        );

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("reasoning",
                "Use compound interest formula: A = P(1 + r)^t where P=10000, r=0.05, t=3");
        result.getOutputData().put("variables", variables);
        return result;
    }
}
