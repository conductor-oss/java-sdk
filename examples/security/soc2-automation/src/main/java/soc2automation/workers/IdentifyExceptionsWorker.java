package soc2automation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class IdentifyExceptionsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "soc2_identify_exceptions";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [exceptions] 2 exceptions: MFA enforcement gap, backup testing overdue");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("identify_exceptions", true);
        result.addOutputData("processed", true);
        return result;
    }
}
