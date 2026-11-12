# Server changes for native workflows

Companion to `README.md`. Status: implemented in the server repository (`conductor-oss/conductor`,
branch `design/native-workflows`), as protocol 2; its `proposals/native-workflows/` has the full
design (`01` to `03`), the test plan and the implementation status. Every item below names the
code it touches there. Paths are relative to that repository.

## Phase A: no server change

The SDK could have run the kitchensink on an unchanged server:

- Each continuation as a SIMPLE task named `<workflow>.v<version>.code` that the owning worker
  polls, and each fragment as an inline sub-workflow: `SubWorkflowParams.workflowDefinition`
  (`common/.../workflow/SubWorkflowParams.java`) is mapped by `SubWorkflowTaskMapper`.
- Owner affinity through `taskToDomain` with the ordered fallback that `setTaskDomains` and
  `getActiveDomain` implement (`core/.../execution/WorkflowExecutorOps.java`).
- External completion through `POST /api/tasks/{workflowId}/{taskRefName}/{status}`
  (`rest/.../controllers/TaskResource.java`).
- Variables through SET_VARIABLE (`core/.../tasks/SetVariable.java`).

Limits of Phase A: every fragment is a child execution, so the diagram nests, and fork branches
share one continuation, so a branch cannot advance until its siblings pause. Phase B removed both,
and its second round removed the pause tasks themselves.

## Phase B: the flat graph, with exactly the code's tasks

### B1. Engine state: continuation markers and `nativeState` (small)

The code only ever pauses on a task, so the state the engine needs to drive the code lives on
those tasks.

- `common/.../tasks/TaskContinuation.java`, and a `continuation` field on `Task` and `TaskModel`:
  `lane`, `n`, `state` (`WAITING` → `RUNNABLE` → `LEASED` → `APPLIED`), `epoch`, `worker`,
  `queue`, `queuedAt`, `leasedAt`, `appliedAt`, `attempts`, `deferred`, and once applied the
  record a replay reads: `step`, `recorded`, `resumedWith`, `done`, `result`, `error`.
- `common/.../run/NativeState.java`, and a `nativeState` field on `Workflow` and `WorkflowModel`:
  `protocol`, `codeType`, `continuationTimeoutSeconds`, `step`, `activeContinuation`, `activeUntil`, `bootstrap` and
  `handlers` (markers for the two lanes that have no task), `replies`, `seen`. O(1) in the length
  of the execution.
- `TaskMapperContext.createTaskModel` copies an entry's `metadata.continuation` onto the scheduled
  task as a `WAITING` marker. `NativeContinuationService.initialize` builds `nativeState` at start
  and restart from the skeleton's `metadata.native`.
- The registered definition carries the graph the SDK discovered from the code
  (`Registration.skeleton` → `Replayer.discover`), with inline task definitions on its SIMPLE
  entries so it validates; `WorkflowDef` also allows `tasks: []` when `metadata.native` is
  present (the harness registers that). `NativeContinuationService.initialize` clears the tasks
  from the execution's own copy at start and restart, so an execution still starts empty and
  holds only what the code calls. `MetadataServiceImpl` refuses native definitions when
  `conductor.app.nativeWorkflow.enabled=false`.

### B2. Wake, lease, complete (medium, the core change)

- Wake: `DeciderService.decide` calls `NativeContinuationService.onEvaluated` when it evaluates a
  terminal task; a `WAITING` marker becomes `RUNNABLE`. `WorkflowExecutorOps.decide` then runs
  `repairAndWake`, saves the touched tasks and the workflow row, and pushes one message per
  runnable or expired marker (`pushIfNotExists`) to the continuation queue: the shared queue of
  the code type, unless the workflow's `taskToDomain` names the code type or `*`, resolved by
  `ExecutorUtils.activeDomain` (the code `getActiveDomain` also uses): the first domain with a
  poller in the last 10 s, else the last entry, `NO_DOMAIN` meaning the shared queue. While a marker
  is `RUNNABLE` or `LEASED` (`NativeContinuationService.needsPromptRevisit`), `decide` caps the
  workflow's decider-queue postpone at `workflowOffsetTimeout` (30 s) instead of the
  `maxPostponeDurationSeconds` (an hour) the in-progress tasks would allow, and always writes it,
  replacing a longer earlier postpone, so the next decide runs `repairAndWake` soon enough to
  re-route a wake-up queued for a domain whose workers are gone. Found by `chaos.sh` under the
  earlier per-worker routing: a worker killed right after its continuation was queued to its own
  domain queue, with the workflow postponed by an hour.
- Offer and lease: a decide, under the lock it already takes, offers the next continuation
  (`NativeContinuationService.offer` from `repairAndWake`): the marker `LEASED` with `epoch + 1`,
  the slot, the queue, then the push. `ExecutionService.poll` hands a continuation queue's
  messages to `NativeContinuationService.lease`, which takes no lock and writes nothing: it checks
  the message is the one offered, leaves it unacknowledged for `continuationTimeoutSeconds`, and
  returns the finished task itself, or a synthetic task (`<workflowId>|main`, `<workflowId>|handlers`) built
  from `nativeState` with the workflow input or the claimed messages.
- Complete: `WorkflowExecutorOps.updateTask` routes a synthetic id, or a `LEASED` task whose
  result carries `protocol`, to `completeContinuation`, which runs inside the decide the
  completion triggers and calls `NativeContinuationService.complete` under that decide's lock: lease check (`n`, `epoch`; else 409),
  protocol check, variable conflict and pending-message deferral (a main lane that returned while
  the handler lane has work is deferred with `waiting for handlers: ` and not re-queued;
  `repairAndWake` wakes it once that lane has settled), fragment validation and
  `WorkflowDef.insertAfter` (after the awaited task, or after the entry the fragment's first
  element names in `metadata.after`: the SDK sets it on the first call after a `switchOn` case
  the lane resumed in, so that call lands after the switch and not inside the case;
  `NativeContinuationService.anchorOf`; an anchor that names no entry is a rejected fragment),
  variables, replies, seen, releases, the marker's record, the awaited task's
  `executed = false`; then the workflow row is saved before the task, the message is acknowledged,
  and the workflow is completed or failed when the main lane is done.
- `DeciderService.checkForWorkflowCompletion` returns false for native workflows; the start path
  schedules nothing for an empty native definition.

Checks that mattered:

- `isLazyEvaluateWorkflow` needs no exemption: a task in a fork whose JOIN has `joinOn: []` is
  listed by no join, so its completion decides at once.
- Definition size: each append rewrites the workflow row. Long plain-Java loops grow it without
  bound; `conductor.app.nativeWorkflow.maxDefinitionTasks` (5000) caps it. This is the "very
  large loops" round.

### B3. JOIN with live targets (small)

`Join.execute`, for a native JOIN whose `joinOn` is empty: targets are the last entry of each
branch of the preceding fork in the current definition; the JOIN waits while the fork is not in
the definition it read, and while any target is not terminal or carries a marker that is not
`APPLIED` with `done` (`NativeContinuationService.lanesSettled`). `inputParameters.joinStrategy:
ANY` completes on the first settled success.

### B4. Recorded values (none)

`once`, `now` and `random` are reported in the completion's `recorded` map and stored on the
marker. No task type; the earlier plan's RECORD task was dropped with protocol 2.

### B5. UI: continuation on the task panel, engine state on the summary (small)

`ui-next/src/pages/execution/TaskSummary.tsx` shows a task's `continuation` (lane, number,
state, deferred reason, worker, applied time); `ExecutionSummary.tsx` shows `nativeState` (code
type, continuations applied, bootstrap state while pending, handler run). There are
no hidden tasks to fold. `metadata.site = "Class.method:line"` on every entry gives the source
line. A SWITCH emitted by `switchOn` needs nothing new on either side: `SwitchTaskMapper`
evaluates `value-param` on `switchCaseValue` and records `selectedCase` and `evaluationResult`;
`ui-next`'s switch mapper (`src/components/features/flow/nodes/mapper/switch.ts`) draws every
`decisionCases` key, empty lists and the default case included, and marks the selected one. The
preview entries the SDK puts in untaken cases (`metadata.preview: true`, `<base>_if_<key>`) are
ordinary definition entries to the server, validated like any other and never scheduled; neither
the server nor `ui-next` reads `metadata.preview`.

### B6. Per-execution time scale for WAIT (small)

`WaitTaskMapper` divides the duration by `variables.__timeScale` when
`conductor.app.nativeWorkflow.waitTimeScaleEnabled` is true. Dev mode sets the variable in the
first continuation's completion; production never does.

## Standing items from earlier passes

### S1. Idempotency key on the public start (done)

`WorkflowExecutorOps.startWorkflow(StartWorkflowInput)` routes through `startWorkflowIdempotent`
when `idempotencyKey` is set; `RETURN_EXISTING` or `FAIL`.

### S2. Signals and early signals (done)

- Targeted completion of a specific WAIT: `POST /api/tasks/{workflowId}/{taskRefName}/{status}`,
  used for `awaitExternal` and human tasks.
- Signals, updates and queries: `POST /api/workflow/{workflowId}/messages` pushes to the durable
  per-workflow message queue (`JdbcWorkflowMessageQueueDAO`, SQLite, Postgres and MySQL) inside a
  decide, which wakes and offers the handler marker. The handler lane's poll claims the messages under
  `handlers#<n>`, fenced by `n`. `awaitCondition` waits are completed through the handler
  completion's `releases`.

### S3. JOIN any-of (done)

`inputParameters.joinStrategy: ANY` on the JOIN; the other branches are not cancelled.

## Not server changes

- Replay, divergence detection, the in-memory test environment and `DurableReplay` are SDK work
  on top of the markers and the task list the server exposes.
- Build pinning is opt-in: `buildId` on the host adds a polled domain, and a domain list in the
  start's `taskToDomain` uses it. The SDK sends no routing data in a completion.
- `@WorkerTask` registration attributes (`retryCount`, `retryDelaySeconds`,
  `responseTimeoutSeconds`) are SDK changes that produce ordinary task definitions.

## Deferred to their own rounds

- Latency: a push transport for continuations, replacing long-poll for the owning worker, and an
  in-memory fast path that skips the replay.
- Very large loops: incremental decide and bounded definition growth (`continueAsNew`).
