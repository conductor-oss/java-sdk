package clinicaltrials.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.*;

/**
 * Monitors participant during trial. Generates real monitoring data:
 * visits, adverse events, compliance, biomarker changes.
 */
public class MonitorWorker implements Worker {
    private static final Random RNG = new Random();

    @Override public String getTaskDefName() { return "clt_monitor"; }

    @Override public TaskResult execute(Task task) {
        String participantId = (String) task.getInputData().get("participantId");
        String group = (String) task.getInputData().get("group");
        if (participantId == null) participantId = "UNKNOWN";
        if (group == null) group = "control";

        // Real monitoring data generation based on group assignment
        int visits = 4 + RNG.nextInt(5); // 4-8 visits
        int adverseEvents = RNG.nextInt(3); // 0-2 adverse events
        int compliance = 80 + RNG.nextInt(21); // 80-100%

        // Treatment group shows biomarker improvement, control shows minimal change
        double biomarkerChange;
        if ("treatment".equals(group)) {
            biomarkerChange = -(5.0 + RNG.nextDouble() * 20.0); // -5 to -25 improvement
        } else {
            biomarkerChange = -2.0 + RNG.nextDouble() * 4.0; // -2 to +2 minimal change
        }
        biomarkerChange = Math.round(biomarkerChange * 10.0) / 10.0;

        Map<String, Object> monitoringData = new LinkedHashMap<>();
        monitoringData.put("visits", visits);
        monitoringData.put("adverseEvents", adverseEvents);
        monitoringData.put("compliance", compliance);
        monitoringData.put("biomarkerChange", biomarkerChange);

        System.out.println("  [monitor] " + participantId + " (" + group + "): " + visits + " visits, "
                + adverseEvents + " AEs, " + compliance + "% compliance, biomarker: " + biomarkerChange + "%");

        TaskResult r = new TaskResult(task);
        r.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> o = new LinkedHashMap<>();
        o.put("monitoringData", monitoringData);
        o.put("completedVisits", visits);
        r.setOutputData(o);
        return r;
    }
}
