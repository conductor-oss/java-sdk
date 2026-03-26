package securityposture;

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
        assertEquals("sp_assess_infrastructure", new securityposture.workers.AssessInfrastructureWorker().getTaskDefName());
        assertEquals("sp_assess_application", new securityposture.workers.AssessApplicationWorker().getTaskDefName());
        assertEquals("sp_assess_compliance", new securityposture.workers.AssessComplianceWorker().getTaskDefName());
        assertEquals("sp_calculate_score", new securityposture.workers.CalculateScoreWorker().getTaskDefName());
    }
}
