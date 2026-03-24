# Payroll Workflow

Payroll processing: collect hours, calculate gross, apply deductions, process, distribute stubs.

**Input:** `payPeriodId`, `departmentId` | **Timeout:** 60s

## Pipeline

```
prl_collect_hours
    │
prl_calculate_gross
    │
prl_apply_deductions
    │
prl_process_payroll
    │
prl_distribute_stubs
```

## Workers

**ApplyDeductionsWorker** (`prl_apply_deductions`)

```java
double federalTax = Math.round(gross * 0.22 * 100.0) / 100.0;
double stateTax = Math.round(gross * 0.05 * 100.0) / 100.0;
```

Reads `grossPayroll`. Outputs `netPayroll`, `totalDeductions`, `federalTax`, `stateTax`, `benefits`.

**CalculateGrossWorker** (`prl_calculate_gross`)

```java
gross += (regular * rate) + (ot * rate * 1.5);
```

Reads `employeeRecords`. Outputs `grossPayroll`, `employeeCount`.

**CollectHoursWorker** (`prl_collect_hours`)

Reads `payPeriodId`. Outputs `employeeRecords`, `totalEmployees`.

**DistributeStubsWorker** (`prl_distribute_stubs`)

Reads `employeeCount`, `processedBatchId`. Outputs `distributedCount`, `method`.

**ProcessPayrollWorker** (`prl_process_payroll`)

Reads `netPayroll`. Outputs `batchId`, `processedAt`, `bankReference`.

## Tests

**4 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
