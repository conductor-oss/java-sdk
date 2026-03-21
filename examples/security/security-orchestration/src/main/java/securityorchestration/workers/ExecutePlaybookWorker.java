package securityorchestration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class ExecutePlaybookWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "soar_execute_playbook";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [playbook] Host isolated, C2 domain blocked, forensic collection started");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("execute_playbook", true);
        return result;
    }
}
