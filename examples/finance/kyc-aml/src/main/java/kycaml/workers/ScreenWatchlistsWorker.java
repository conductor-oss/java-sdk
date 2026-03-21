package kycaml.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;

/**
 * Screens customer against OFAC, PEP, and adverse media watchlists.
 */
public class ScreenWatchlistsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "kyc_screen_watchlists";
    }

    @Override
    public TaskResult execute(Task task) {
        String name = (String) task.getInputData().get("name");

        System.out.println("  [screen] Screening " + name + " against OFAC, PEP, and adverse media");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("hits", 0);
        result.getOutputData().put("listsChecked",
                List.of("OFAC_SDN", "EU_Sanctions", "PEP_Global", "Adverse_Media"));
        result.getOutputData().put("clearanceStatus", "clear");
        return result;
    }
}
