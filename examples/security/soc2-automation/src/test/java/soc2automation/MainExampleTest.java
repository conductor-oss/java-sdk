package soc2automation;

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
        assertEquals("soc2_collect_controls", new soc2automation.workers.CollectControlsWorker().getTaskDefName());
        assertEquals("soc2_test_effectiveness", new soc2automation.workers.TestEffectivenessWorker().getTaskDefName());
        assertEquals("soc2_identify_exceptions", new soc2automation.workers.IdentifyExceptionsWorker().getTaskDefName());
        assertEquals("soc2_generate_evidence", new soc2automation.workers.GenerateEvidenceWorker().getTaskDefName());
    }
}
