package logisticsoptimization.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.*;
import java.util.stream.*;

public class ScheduleWorker implements Worker {
    @Override public String getTaskDefName() { return "lo_schedule"; }

    @SuppressWarnings("unchecked")
    @Override public TaskResult execute(Task task) {
        List<Map<String, Object>> routes = (List<Map<String, Object>>) task.getInputData().get("routes");
        if (routes == null) routes = List.of();
        List<Map<String, Object>> schedule = routes.stream()
                .map(rt -> Map.<String, Object>of("route", rt.get("id"), "vehicle", "V-" + rt.get("id"), "departure", "06:00"))
                .collect(Collectors.toList());
        System.out.println("  [schedule] Scheduled " + schedule.size() + " vehicles for " + task.getInputData().get("date"));
        TaskResult r = new TaskResult(task);
        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("schedule", schedule);
        r.getOutputData().put("vehicleCount", schedule.size());
        return r;
    }
}
