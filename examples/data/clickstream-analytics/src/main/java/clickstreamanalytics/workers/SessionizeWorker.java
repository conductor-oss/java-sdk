package clickstreamanalytics.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Groups click events into user sessions.
 * Input: events, sessionTimeout
 * Output: sessions, sessionCount, avgDuration
 */
public class SessionizeWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ck_sessionize";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<Map<String, Object>> events = (List<Map<String, Object>>) task.getInputData().get("events");
        if (events == null) {
            events = List.of();
        }

        Map<String, List<Map<String, Object>>> byUser = new LinkedHashMap<>();
        for (Map<String, Object> e : events) {
            String userId = (String) e.get("userId");
            byUser.computeIfAbsent(userId, k -> new ArrayList<>()).add(e);
        }

        List<Map<String, Object>> sessions = new ArrayList<>();
        for (Map.Entry<String, List<Map<String, Object>>> entry : byUser.entrySet()) {
            List<Map<String, Object>> clicks = entry.getValue();
            List<String> pages = clicks.stream().map(c -> (String) c.get("page")).collect(Collectors.toList());
            int firstTs = ((Number) clicks.get(0).get("ts")).intValue();
            int lastTs = ((Number) clicks.get(clicks.size() - 1).get("ts")).intValue();
            int duration = clicks.size() > 1 ? lastTs - firstTs : 0;

            Map<String, Object> session = new LinkedHashMap<>();
            session.put("userId", entry.getKey());
            session.put("clicks", clicks.size());
            session.put("pages", pages);
            session.put("duration", duration);
            sessions.add(session);
        }

        long totalDuration = sessions.stream().mapToLong(s -> ((Number) s.get("duration")).longValue()).sum();
        long avgDuration = sessions.isEmpty() ? 0 : Math.round((double) totalDuration / sessions.size());

        System.out.println("  [sessionize] Created " + sessions.size() + " sessions, avg duration: " + avgDuration + "s");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("sessions", sessions);
        result.getOutputData().put("sessionCount", sessions.size());
        result.getOutputData().put("avgDuration", avgDuration + "s");
        return result;
    }
}
