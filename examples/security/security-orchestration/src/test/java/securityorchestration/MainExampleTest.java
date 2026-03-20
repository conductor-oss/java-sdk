package securityorchestration;

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
        assertEquals("soar_ingest_alert", new securityorchestration.workers.IngestAlertWorker().getTaskDefName());
        assertEquals("soar_enrich", new securityorchestration.workers.EnrichWorker().getTaskDefName());
        assertEquals("soar_decide_action", new securityorchestration.workers.DecideActionWorker().getTaskDefName());
        assertEquals("soar_execute_playbook", new securityorchestration.workers.ExecutePlaybookWorker().getTaskDefName());
    }
}
