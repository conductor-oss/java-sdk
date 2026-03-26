package slamonitoring.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class ReportWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "sla_report";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [report] SLA report generated for stakeholders");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("report", true);
        return result;
    }
}
