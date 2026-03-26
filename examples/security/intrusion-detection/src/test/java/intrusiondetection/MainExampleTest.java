package intrusiondetection;

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
        assertEquals("id_analyze_events", new intrusiondetection.workers.AnalyzeEventsWorker().getTaskDefName());
        assertEquals("id_correlate_threats", new intrusiondetection.workers.CorrelateThreatsWorker().getTaskDefName());
        assertEquals("id_assess_severity", new intrusiondetection.workers.AssessSeverityWorker().getTaskDefName());
        assertEquals("id_respond", new intrusiondetection.workers.RespondWorker().getTaskDefName());
    }
}
