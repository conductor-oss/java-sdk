package servicemigration;

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
        assertEquals("sm_assess", new servicemigration.workers.AssessWorker().getTaskDefName());
        assertEquals("sm_replicate", new servicemigration.workers.ReplicateWorker().getTaskDefName());
        assertEquals("sm_cutover", new servicemigration.workers.CutoverWorker().getTaskDefName());
        assertEquals("sm_validate", new servicemigration.workers.ValidateWorker().getTaskDefName());
    }
}
