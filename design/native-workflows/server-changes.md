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
  `leased`, `retryCount` (0 on the first offer, one more each time an attempt ends without
  applying), and once applied the record a replay reads: `endTime` (its order; set when it
  applies, the first continuation's included, so applied means `endTime > 0`), `done`,
  `recorded`, `resumedWith`; and `n` on the handler lane's marker only, its run number. Which
  continuation a task's marker is comes from the task's entry (`metadata.lane`,
  `metadata.continuation`); its state is derived (leased; applied; runnable once its task has
  ended and been evaluated; else waiting). Why the last completion did not apply is the
  workflow's `reasonForIncompletion` (one continuation runs at a time), cleared by the next
  apply. What the main lane returned is the run's output, and its exception the run's
  `reasonForIncompletion`. Empty values are not stored: a marker that has not run is `{}`.
- `common/.../run/NativeState.java`, and a `nativeState` field on `Workflow` and `WorkflowModel`:
  `bootstrap` and `handlers` (markers for the two lanes that have no task), and `replies`
  (every handled message). The bootstrap is always `main` #1 and the handler marker's lane always
  `handlers`, so neither is on a marker; the handler marker stores its run number (`{"n": 4}`).
  O(1) in the length of the execution. The code type and the
  continuation timeout stay in the definition's `metadata.native`.
- `TaskMapperContext.createTaskModel` gives the scheduled task a new marker when its entry carries
  `metadata.continuation`. `NativeContinuationService.initialize` builds `nativeState` at start
  and restart from the skeleton's `metadata.native`.
- The registered definition carries the graph the SDK discovered from the code
  (`Registration.skeleton` → `Replayer.discover`), with inline task definitions on its SIMPLE
  entries so it validates; `WorkflowDef` also allows `tasks: []` when `metadata.native` is
  present (the harness registers that). `NativeContinuationService.initialize` clears the tasks
  from the execution's own copy at start and restart, so an execution still starts empty and
  holds only what the code calls. `MetadataServiceImpl` refuses native definitions when
  `conductor.app.nativeWorkflow.enabled=false`.

### B2. Wake, lease, complete (medium, the core change)

- Wake: `DeciderService.decide` evaluates a terminal task as for any task (`executed`), which is
  all it takes for the continuation on it to be runnable. `WorkflowExecutorOps.decide` then runs
  `repairAndWake`, saves the touched tasks and the workflow row, and pushes the offered
  continuation's message, or the leased one's again (`pushIfNotExists`), to the continuation
  queue: the shared queue of the code type; task domains do not apply to workflow code. While a
  continuation is runnable or leased (`NativeContinuationService.needsPromptRevisit`), `decide` caps the
  workflow's decider-queue postpone at `workflowOffsetTimeout` (30 s) instead of the
  `maxPostponeDurationSeconds` (an hour) the in-progress tasks would allow, and always writes it,
  replacing a longer earlier postpone, so the next decide runs `repairAndWake` soon enough to
  re-route a wake-up queued for a domain whose workers are gone. Found by `chaos.sh` under the
  earlier per-worker routing: a worker killed right after its continuation was queued to its own
  domain queue, with the workflow postponed by an hour.
- Offer and lease: a decide, under the lock it already takes, offers the next continuation
  (`NativeContinuationService.offer` from `repairAndWake`): the handler lane while messages are
  queued, else the first continuation until it applied, else the one whose task ended first; the
  marker `leased` at its `retryCount`, which makes it the slot, then the push. A deferred
  continuation keeps its turn: it is offered again at once and holds the slot through its backoff. `ExecutionService.poll` hands a continuation queue's
  messages to `NativeContinuationService.lease`, which takes no lock and writes nothing: it checks
  the message is the one offered, leaves it unacknowledged for `continuationTimeoutSeconds`, and
  returns the finished task itself, or a synthetic task (`<workflowId>|main`, `<workflowId>|handlers`) built
  from `nativeState` with the claimed messages for a handler run and an entry whose `metadata`
  names the continuation (`lane`, `continuation`) as a task's entry does; the SDK reads lane and
  number from `task.workflowTask.metadata` for every continuation.
- Complete: `WorkflowExecutorOps.updateTask` routes a synthetic id, or a task with a marker whose
  result carries `protocol`, to `completeContinuation`, which runs inside the decide the
  completion triggers and calls `NativeContinuationService.complete` under that decide's lock: lease check (marker leased, `n` the continuation's number from its entry, `retryCount` the marker's; else 409),
  protocol check, variable conflict and pending-message deferral (a main lane that returned while
  the handler lane has work is deferred with `waiting for handlers: ` and not re-queued;
  `repairAndWake` offers the handler lane first and this one once that lane has settled; every
  deferral adds one to `retryCount` and puts its reason on the workflow), fragment validation and
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
applied with `done` (`NativeContinuationService.lanesSettled`). `done` stays on the marker for this:
the JOIN reads the definition and the tasks in two reads without the workflow lock, and `done` is
read with the task.

### B4. Recorded values (none)

`once`, `now` and `random` are reported in the completion's `recorded` map and stored on the
marker. No task type; the earlier plan's RECORD task was dropped with protocol 2.

### B5. UI: continuation on the task panel, engine state on the summary (small)

`ui-next/src/pages/execution/TaskSummary.tsx` shows a task's `continuation` (lane and number from
the entry's metadata, the derived state, applied time from `endTime`); `ExecutionSummary.tsx` shows `nativeState` (code
type, continuations applied, bootstrap state while pending, handler run). A run is held while its
`reasonForIncompletion` starts with `diverged: `; the held step sits after the task whose marker
is leased. There are
no hidden tasks to fold. `metadata.site = "Class.method:line"` on every entry gives the source
line. A SWITCH emitted by `switchOn` or drawn for a plain if or switch uses the evaluator
`native-code`, which reads the case the code took from `switchCaseValue`; `SwitchTaskMapper`
records `selectedCase` and `evaluationResult`;
`ui-next`'s switch mapper (`src/components/features/flow/nodes/mapper/switch.ts`) draws every
`decisionCases` key, empty lists and the default case included, and marks the selected one. The
preview entries the SDK puts in untaken cases (`metadata.preview: true`, `<base>_if_<key>`) are
ordinary definition entries to the server, validated like any other and never scheduled; neither
the server nor `ui-next` reads `metadata.preview`.

### B6. Per-execution time scale for WAIT (removed)

Dropped: timers run in real time in development too. `Workers.timeScale` is gone, and
`WaitTaskMapper` is unchanged from `main`.

## Standing items from earlier passes

### S1. Idempotency key on the public start (done)

`WorkflowExecutorOps.startWorkflow(StartWorkflowInput)` routes through `startWorkflowIdempotent`
when `idempotencyKey` is set; `RETURN_EXISTING` or `FAIL`.

### S2. Signals and early signals (done)

- Targeted completion of a specific WAIT: `POST /api/tasks/{workflowId}/{taskRefName}/{status}`,
  used for `external(..)` waits and human tasks.
- Signals, updates and queries: `POST /api/workflow/{workflowId}/messages` pushes to the durable
  per-workflow message queue (`JdbcWorkflowMessageQueueDAO`, SQLite, Postgres and MySQL) inside a
  decide, which wakes and offers the handler marker. The handler lane's poll claims the messages under
  `handlers#<n>`, fenced by `n`. `awaitCondition` waits are completed through the handler
  completion's `releases`.

### S3. JOIN any-of (removed)

Dropped: nothing used it.

## Not server changes

- Replay, divergence detection, the in-memory test environment and `DurableReplay` are SDK work
  on top of the markers and the task list the server exposes.
- No build pinning: workflow code is polled on the shared queue of its code type only, and the
  SDK sends no routing data in a completion.
- `@WorkerTask` registration attributes (`retryCount`, `retryDelaySeconds`,
  `responseTimeoutSeconds`) are SDK changes that produce ordinary task definitions.

## Deferred to their own rounds

- Latency: a push transport for continuations, replacing long-poll for the owning worker, and an
  in-memory fast path that skips the replay.
- Very large loops: incremental decide and bounded definition growth (`continueAsNew`).
