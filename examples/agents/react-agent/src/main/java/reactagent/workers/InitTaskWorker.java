package reactagent.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.List;

/**
 * Initializes the ReAct agent loop by capturing the question and
 * creating an empty context list for subsequent iterations.
 */
public class InitTaskWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "rx_init_task";
    }

    @Override
    public TaskResult execute(Task task) {
        String question = (String) task.getInputData().get("question");
        if (question == null || question.isBlank()) {
            question = "";
        }

        System.out.println("  [rx_init_task] Initializing ReAct agent for: " + question);

        List<String> context = new ArrayList<>();

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("question", question);
        result.getOutputData().put("context", context);
        return result;
    }
}
