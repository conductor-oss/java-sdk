package clickstreamanalytics.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GenerateReportWorkerTest {

    private final GenerateReportWorker worker = new GenerateReportWorker();

    @Test
    void taskDefName() {
        assertEquals("ck_generate_report", worker.getTaskDefName());
    }

    @SuppressWarnings("unchecked")
    @Test
    void generatesReportWithAllFields() {
        List<Map<String, Object>> sessions = List.of(
                Map.of("userId", "U1", "clicks", 3),
                Map.of("userId", "U2", "clicks", 2));
        Map<String, Object> analysis = Map.of("converted", 1, "bounced", 0);

        Task task = taskWith(Map.of("sessions", sessions, "journeyAnalysis", analysis, "analysisType", "conversion_funnel"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        Map<String, Object> report = (Map<String, Object>) result.getOutputData().get("report");
        assertNotNull(report);
        assertEquals(2, report.get("totalSessions"));
        assertEquals(1, report.get("conversions"));
        assertEquals(0, report.get("bounces"));
        assertNotNull(report.get("topPages"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void handlesEmptyInput() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        Map<String, Object> report = (Map<String, Object>) result.getOutputData().get("report");
        assertEquals(0, report.get("totalSessions"));
        assertEquals(0, report.get("conversions"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
