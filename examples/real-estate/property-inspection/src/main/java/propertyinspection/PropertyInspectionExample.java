package propertyinspection;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import propertyinspection.workers.*;
import java.util.List;
import java.util.Map;
public class PropertyInspectionExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 687: Property Inspection ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("pin_schedule","pin_inspect","pin_document","pin_report"));
        client.registerWorkflow("workflow.json");
        client.startWorkers(List.of(new ScheduleWorker(),new InspectWorker(),new DocumentWorker(),new ReportWorker()));
        if (args.length > 0 && "--workers".equals(args[0])) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("pin_property_inspection",1,Map.of("propertyId","PROP-400","inspectorId","INS-15","inspectionType","pre-purchase"));
        Workflow wf = client.waitForWorkflow(wfId,"COMPLETED",60000);
        String status = wf.getStatus().name();
        System.out.println("  Status: "+status+"\n  Report ID: "+wf.getOutput().get("reportId")+"\n  Condition: "+wf.getOutput().get("overallCondition"));
        client.stopWorkers();
        System.out.println("COMPLETED".equals(status)?"\nResult: PASSED":"\nResult: FAILED");
        System.exit("COMPLETED".equals(status)?0:1);
    }
}
