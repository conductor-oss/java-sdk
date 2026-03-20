package slamonitoring.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Worker for sla_process task -- processes the request and returns processed=true.
 *
 * This is the first step in the SLA monitoring workflow, representing
 * the initial processing before a human approval wait step.
 */
public class SlaProcessWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "sla_process";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [sla_process] Processing request...");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("processed", true);

        System.out.println("  [sla_process] Processing complete.");
        return result;
    }
}
