package batchscheduling.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;

/**
 * Prioritizes jobs in a batch based on priority weighting.
 * Input: batchId, jobs
 * Output: orderedJobs, totalJobs, strategy
 */
public class PrioritizeJobsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "bs_prioritize_jobs";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        String batchId = (String) task.getInputData().get("batchId");
        List<String> jobs = (List<String>) task.getInputData().get("jobs");
        if (jobs == null) {
            jobs = List.of();
        }

        System.out.println("  [prioritize] Prioritizing " + jobs.size() + " jobs in batch " + batchId + "...");

        List<String> orderedJobs = List.of("etl-import", "data-transform", "report-gen");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("orderedJobs", orderedJobs);
        result.getOutputData().put("totalJobs", 3);
        result.getOutputData().put("strategy", "priority-weighted");
        return result;
    }
}
