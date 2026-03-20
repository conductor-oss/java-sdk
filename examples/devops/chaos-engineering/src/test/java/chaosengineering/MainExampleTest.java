package chaosengineering;

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
        assertEquals("ce_define_experiment", new chaosengineering.workers.DefineExperimentWorker().getTaskDefName());
        assertEquals("ce_inject_failure", new chaosengineering.workers.InjectFailureWorker().getTaskDefName());
        assertEquals("ce_observe", new chaosengineering.workers.ObserveWorker().getTaskDefName());
        assertEquals("ce_recover", new chaosengineering.workers.RecoverWorker().getTaskDefName());
    }
}
