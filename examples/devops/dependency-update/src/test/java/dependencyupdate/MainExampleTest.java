package dependencyupdate;

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
        assertEquals("du_scan_outdated", new dependencyupdate.workers.ScanOutdatedWorker().getTaskDefName());
        assertEquals("du_update_deps", new dependencyupdate.workers.UpdateDepsWorker().getTaskDefName());
        assertEquals("du_run_tests", new dependencyupdate.workers.RunTestsWorker().getTaskDefName());
        assertEquals("du_create_pr", new dependencyupdate.workers.CreatePrWorker().getTaskDefName());
    }
}
