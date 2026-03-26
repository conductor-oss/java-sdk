package votingworkflow.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class CertifyWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "vtw_certify";
    }

    @Override
    public TaskResult execute(Task task) {
        String electionId = (String) task.getInputData().get("electionId");
        Object totalVotes = task.getInputData().get("totalVotes");
        System.out.printf("  [certify] Election %s certified with %s votes%n", electionId, totalVotes);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("certified", true);
        result.getOutputData().put("certificationId", "CERT-321");
        return result;
    }
}
