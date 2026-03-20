package postmortemautomation;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MainExampleTest {

    @Test
    void workflowJsonIsLoadable() {
        var is = getClass().getClassLoader().getResourceAsStream("workflow.json");
        assertNotNull(is, "workflow.json should be loadable from resources");
    }

    @Test
    void workerInstantiation() {
        assertEquals("pm_gather_timeline", new postmortemautomation.workers.GatherTimelineWorker().getTaskDefName());
        assertEquals("pm_collect_metrics", new postmortemautomation.workers.CollectMetricsWorker().getTaskDefName());
        assertEquals("pm_draft_document", new postmortemautomation.workers.DraftDocumentWorker().getTaskDefName());
        assertEquals("pm_schedule_review", new postmortemautomation.workers.ScheduleReviewWorker().getTaskDefName());
    }
}
