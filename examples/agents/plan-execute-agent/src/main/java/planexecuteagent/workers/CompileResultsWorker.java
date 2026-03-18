package planexecuteagent.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Compiles the results from all executed steps into a final report.
 */
public class CompileResultsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "pe_compile_results";
    }

    @Override
    public TaskResult execute(Task task) {
        String objective = (String) task.getInputData().get("objective");
        if (objective == null || objective.isBlank()) {
            objective = "";
        }

        String result1 = (String) task.getInputData().get("result1");
        if (result1 == null || result1.isBlank()) {
            result1 = "";
        }

        String result2 = (String) task.getInputData().get("result2");
        if (result2 == null || result2.isBlank()) {
            result2 = "";
        }

        String result3 = (String) task.getInputData().get("result3");
        if (result3 == null || result3.isBlank()) {
            result3 = "";
        }

        System.out.println("  [pe_compile_results] Compiling results for objective: " + objective);

        String report = "Objective: " + objective
                + " | Step 1: " + result1
                + " | Step 2: " + result2
                + " | Step 3: " + result3;

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("report", report);
        return result;
    }
}
