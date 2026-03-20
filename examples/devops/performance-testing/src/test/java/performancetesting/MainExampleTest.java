package performancetesting;

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
        assertEquals("pt_prepare_env", new performancetesting.workers.PrepareEnvWorker().getTaskDefName());
        assertEquals("pt_run_load_test", new performancetesting.workers.RunLoadTestWorker().getTaskDefName());
        assertEquals("pt_analyze_results", new performancetesting.workers.AnalyzeResultsWorker().getTaskDefName());
        assertEquals("pt_generate_report", new performancetesting.workers.GenerateReportWorker().getTaskDefName());
    }
}
