package multiagentplanning.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PmTimelineWorkerTest {

    private final PmTimelineWorker worker = new PmTimelineWorker();

    @Test
    void taskDefName() {
        assertEquals("pp_pm_timeline", worker.getTaskDefName());
    }

    @Test
    void computesCorrectTimeline() {
        // infra: calendar=7, total=14; fe: calendar=8, total=15; be: calendar=9, total=26
        // totalProjectWeeks = 7 + max(8,9) + 2 = 18
        // totalEffort = 14+15+26 = 55 => high risk
        Task task = taskWith(Map.of(
                "projectName", "E-Commerce Platform",
                "architectureSummary", "Microservices architecture",
                "frontendEstimate", Map.of("calendarWeeks", 8, "totalWeeks", 15),
                "backendEstimate", Map.of("calendarWeeks", 9, "totalWeeks", 26),
                "infraEstimate", Map.of("calendarWeeks", 7, "totalWeeks", 14)));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        Map<String, Object> timeline = (Map<String, Object>) result.getOutputData().get("timeline");
        assertNotNull(timeline);
        assertEquals(18, timeline.get("totalProjectWeeks"));
        assertEquals(7, timeline.get("infraCalendarWeeks"));
        assertEquals(8, timeline.get("frontendCalendarWeeks"));
        assertEquals(9, timeline.get("backendCalendarWeeks"));
        assertEquals(2, timeline.get("bufferWeeks"));
        assertEquals(55, timeline.get("totalEffortWeeks"));
        assertEquals("E-Commerce Platform", timeline.get("projectName"));
    }

    @Test
    void createsFiveMilestones() {
        Task task = taskWith(Map.of(
                "projectName", "Test Project",
                "architectureSummary", "summary",
                "frontendEstimate", Map.of("calendarWeeks", 4, "totalWeeks", 8),
                "backendEstimate", Map.of("calendarWeeks", 4, "totalWeeks", 10),
                "infraEstimate", Map.of("calendarWeeks", 3, "totalWeeks", 6)));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> milestones = (List<Map<String, Object>>) result.getOutputData().get("milestones");
        assertNotNull(milestones);
        assertEquals(5, milestones.size());

        assertEquals("Infrastructure Ready", milestones.get(0).get("name"));
        assertEquals("Frontend Alpha", milestones.get(1).get("name"));
        assertEquals("Backend Alpha", milestones.get(2).get("name"));
        assertEquals("Integration Testing", milestones.get(3).get("name"));
        assertEquals("Production Launch", milestones.get(4).get("name"));
    }

    @Test
    void milestoneWeeksAreCorrect() {
        // infra calendar=3, fe calendar=5, be calendar=4
        // totalProjectWeeks = 3 + max(5,4) + 2 = 10
        Task task = taskWith(Map.of(
                "projectName", "Test",
                "architectureSummary", "summary",
                "frontendEstimate", Map.of("calendarWeeks", 5, "totalWeeks", 10),
                "backendEstimate", Map.of("calendarWeeks", 4, "totalWeeks", 12),
                "infraEstimate", Map.of("calendarWeeks", 3, "totalWeeks", 6)));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> milestones = (List<Map<String, Object>>) result.getOutputData().get("milestones");

        // Infrastructure Ready: week 3 (infraCalendar)
        assertEquals(3, milestones.get(0).get("week"));
        // Frontend Alpha: week 3+5=8
        assertEquals(8, milestones.get(1).get("week"));
        // Backend Alpha: week 3+4=7
        assertEquals(7, milestones.get(2).get("week"));
        // Integration Testing: week 3+max(5,4)=8
        assertEquals(8, milestones.get(3).get("week"));
        // Production Launch: week 10
        assertEquals(10, milestones.get(4).get("week"));
    }

    @Test
    void highRiskWhenTotalEffortAbove40() {
        Task task = taskWith(Map.of(
                "projectName", "Big Project",
                "architectureSummary", "summary",
                "frontendEstimate", Map.of("calendarWeeks", 8, "totalWeeks", 15),
                "backendEstimate", Map.of("calendarWeeks", 9, "totalWeeks", 26),
                "infraEstimate", Map.of("calendarWeeks", 7, "totalWeeks", 14)));
        TaskResult result = worker.execute(task);

        // totalEffort = 15+26+14 = 55 > 40 => high
        assertEquals("high", result.getOutputData().get("riskLevel"));
    }

    @Test
    void mediumRiskWhenTotalEffortBetween21And40() {
        Task task = taskWith(Map.of(
                "projectName", "Medium Project",
                "architectureSummary", "summary",
                "frontendEstimate", Map.of("calendarWeeks", 4, "totalWeeks", 8),
                "backendEstimate", Map.of("calendarWeeks", 4, "totalWeeks", 10),
                "infraEstimate", Map.of("calendarWeeks", 3, "totalWeeks", 6)));
        TaskResult result = worker.execute(task);

        // totalEffort = 8+10+6 = 24 => medium
        assertEquals("medium", result.getOutputData().get("riskLevel"));
    }

    @Test
    void lowRiskWhenTotalEffort20OrBelow() {
        Task task = taskWith(Map.of(
                "projectName", "Small Project",
                "architectureSummary", "summary",
                "frontendEstimate", Map.of("calendarWeeks", 2, "totalWeeks", 4),
                "backendEstimate", Map.of("calendarWeeks", 3, "totalWeeks", 8),
                "infraEstimate", Map.of("calendarWeeks", 2, "totalWeeks", 6)));
        TaskResult result = worker.execute(task);

        // totalEffort = 4+8+6 = 18 <= 20 => low
        assertEquals("low", result.getOutputData().get("riskLevel"));
    }

    @Test
    void handlesNullProjectName() {
        Map<String, Object> input = new HashMap<>();
        input.put("projectName", null);
        input.put("architectureSummary", "summary");
        input.put("frontendEstimate", Map.of("calendarWeeks", 4, "totalWeeks", 8));
        input.put("backendEstimate", Map.of("calendarWeeks", 4, "totalWeeks", 10));
        input.put("infraEstimate", Map.of("calendarWeeks", 3, "totalWeeks", 6));
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        @SuppressWarnings("unchecked")
        Map<String, Object> timeline = (Map<String, Object>) result.getOutputData().get("timeline");
        assertEquals("Unnamed Project", timeline.get("projectName"));
    }

    @Test
    void handlesNullEstimates() {
        Map<String, Object> input = new HashMap<>();
        input.put("projectName", "Test");
        input.put("architectureSummary", "summary");
        input.put("frontendEstimate", null);
        input.put("backendEstimate", null);
        input.put("infraEstimate", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        @SuppressWarnings("unchecked")
        Map<String, Object> timeline = (Map<String, Object>) result.getOutputData().get("timeline");
        assertEquals(2, timeline.get("totalProjectWeeks")); // 0 + max(0,0) + 2
        assertEquals("low", result.getOutputData().get("riskLevel"));
    }

    @Test
    void handlesMissingEstimateFields() {
        Task task = taskWith(Map.of(
                "projectName", "Test",
                "architectureSummary", "summary",
                "frontendEstimate", Map.of(),
                "backendEstimate", Map.of(),
                "infraEstimate", Map.of()));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        @SuppressWarnings("unchecked")
        Map<String, Object> timeline = (Map<String, Object>) result.getOutputData().get("timeline");
        assertEquals(2, timeline.get("totalProjectWeeks"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
