# Durability bench for native workflows

A failure-testing bench in the style of Jepsen's analysis of Orkes Conductor 5.3.2
(jepsen.io/analyses/orkes-conductor-5.3.2), aimed at what native workflows add: code that replays,
continuations that move between workers, recorded values, signals and updates. It runs a workload
on several worker processes while a nemesis kills, pauses and partitions the workers and the
server. It records what every client and worker did, then checks the records against the
guarantees in the server repository's `proposals/native-workflows/DURABILITY.md`.

The code is in `src/main/java/durability/`.

## Running it

```bash
./gradlew :native-examples:installDist
BENCH_SERVER_PORT=8099 \
BENCH_SERVER_SCRIPT=/path/to/run-server.sh \
./native-examples/bench.sh run          # exit 0: valid, 1: a check failed
./native-examples/bench.sh check native-examples/build/durability/<run>   # re-check a run
```

`BENCH_SERVER_SCRIPT` runs the server in the foreground and should `exec java ...`, so that the
process the bench kills is the server itself. The bench starts the server with it after a kill.
The server must have `conductor.workflow-message-queue.enabled=true`. With
`conductor.app.nativeWorkflow.waitTimeScaleEnabled=true` the workers' time scale shortens timers.

| Variable | Default | Meaning |
|---|---|---|
| `BENCH_SERVER_PORT` | 8099 | server under test; 8080 is refused |
| `BENCH_PROXY_PORT` | 18097 | the partitioning proxy the workers connect through |
| `BENCH_WORKERS` | 3 | worker processes |
| `BENCH_WORKFLOWS` | 30 | executions started |
| `BENCH_MAIN_SECONDS` | 180 | length of the fault phase |
| `BENCH_RECOVERY_SECONDS` | 600 | how long executions get to finish after healing |
| `BENCH_FAULTS` | all five | `worker-kill,worker-pause,server-kill,server-pause,partition`, or `none` |
| `BENCH_FAULT_INTERVAL` | 12 | mean seconds between faults, exponentially distributed |
| `BENCH_SIGNAL_MS`, `BENCH_UPDATE_MS` | 250, 1500 | client operation rates |
| `BENCH_STEP_MAX_MS`, `BENCH_STEP_FAIL_RATE` | 800, 0.1 | task duration bound and injected transient failure rate |
| `BENCH_SEED` | time | seed for the fault schedule |
| `BENCH_OUT` | `build/durability/<timestamp>` | output directory |

## Workload

`LedgerWorkflow` exercises every part of the engine a failure can break:

1. Recorded values: `once` (a UUID), `random()` and `now()`. Every replay writes what it saw to the
   ledger.
2. A task, then two `async` lanes (two tasks; a timer then a task), joined. Then a child workflow
   running a task, then a condition wait released by a `close` signal, then a last task.
3. Handlers: `deposit` signals append an id to a list, `bump` updates increment a counter and
   return it, and `close` releases the wait. A `deposits` query returns the list, and also tries
   to append to it, which must never take effect.

Every task (`durability_step`) is a pure function of the execution id and step number. It sleeps a
random time and fails transiently at a configured rate, so retries happen all the time. Each
attempt writes start, end or fail to the worker's ledger, an append-only file written with one
`write` per line, so a SIGKILL cannot lose earlier lines. The workflow returns the sum of its six
steps, the deposits it saw in order, and the counter.

## Phases

1. **Main.** A starter starts executions with an idempotency key and correlation id per execution,
   retrying failed starts with the same key. A signaler sends `deposit` to a random running
   execution every 250 ms. Three updaters send `bump`. Two queriers run `deposits` every 400 ms.
   The nemesis injects faults. After recovery, every execution is queried once more.
2. **Recovery.** Heal everything, restart dead workers and the server. Wait for updates in flight,
   retry any start that never succeeded, send `close` to every execution, and wait until all are
   terminal.
3. **Final.** Read every execution by correlation id and by the id each start returned, then check.

Every client operation is recorded in `history.jsonl` as `invoke` followed by `ok`, `fail` or
`info`. `fail` means the operation definitely did not happen: connection refused, or a 4xx. `info`
means it may have happened: a timeout, a reset, or a 5xx. Faults are recorded in the same file.

## Faults

| Fault | How | What it tests |
|---|---|---|
| `worker-kill` | SIGKILL a worker. After 1 to 20 s, restart it: half the time with the same worker id, half with a new one | continuations held by a dead worker; replay on a new host; tasks killed mid-attempt |
| `worker-pause` | SIGSTOP a worker for 5 to 45 s, then SIGCONT | a zombie worker that wakes up and completes stale continuations |
| `server-kill` | SIGKILL the server, restart it after 1 to 10 s | crash between the writes of one operation; in-flight requests with unknown outcome |
| `server-pause` | SIGSTOP the server for 5 to 25 s | client and worker timeouts; long polls that never return |
| `partition` | the proxy resets every worker connection and holds new ones unanswered for 5 to 40 s | workers cut off from the server while clients still reach it |

Not covered: database faults (the bench runs on SQLite in the server process, so a server kill is
also a database crash), clock skew, disk faults, and partitions between clients and server.

## Checks

Errors violate a guarantee. Information is allowed by the guarantees, and is measured.

| Check | Level | Violation |
|---|---|---|
| `lost-execution` | error | a start was acknowledged, but the execution is gone |
| `duplicate-execution` | error | more than one execution for one idempotency key |
| `idempotency-mismatch` | error | two acknowledged starts of one key returned different ids |
| `null-workflow-id` | error | a start was acknowledged with no id |
| `never-acknowledged` | error | no start of a key succeeded, even after recovery |
| `incomplete` | error | an execution is not COMPLETED after recovery |
| `wrong-output` | error | the output's total or id is not what the steps return |
| `nondeterminism` | error | a replay diverged: a failed continuation or a NonDeterminismException in a worker log |
| `replay-divergence` | error | replays observed different `once`, `random` or `now` values, or values that differ from the output |
| `lost-signal` | error | an acknowledged deposit is missing from the output |
| `duplicate-signal` | error | a deposit was applied twice |
| `phantom-signal` | error | a deposit was applied that nobody sent |
| `signal-reordered` | error | acknowledged deposits from one client were applied out of order |
| `lost-update` | error | acknowledged updates returned the same counter value, or the final counter is below what was acknowledged |
| `phantom-update` | error | the counter is above the number of updates sent |
| `update-order` | error | an update acknowledged before another was sent returned the larger counter value: updates are not applied in one real-time order |
| `query-not-prefix` | error | a query answer is not a prefix of the execution's final deposits |
| `query-went-back` | error | one client's later answer is shorter than its earlier one |
| `query-final-mismatch` | error | a query after the execution ended differs from its output |
| `query-mutated` | error | the `deposits` query's attempt to write state reached the execution |
| `task-never-executed` | error | a completed execution has a step with no successful attempt in any worker ledger |
| `task-output-mismatch` | error | the server recorded a step output that no step returns |
| `indefinite-start-took-effect` | info | a start that failed or timed out created the execution anyway; the retry with the same key found it |
| `indefinite-signal-applied`, `indefinite-signal-dropped` | info | what happened to signals whose outcome the client did not know |
| `duplicate-task-execution` | info | a step succeeded more than once (at-least-once task execution) |
| `concurrent-task-execution` | info | two attempts of one step overlapped in time |
| `continuation-retried` | info | a continuation was offered more than once (`attempts > 1` on its marker), or its completion was deferred: a failover or a re-run. A redelivery to a second worker after the first died runs the same offer and does not raise `attempts` |
| `query-lag` | info | a query missed deposits acknowledged before it was sent but not yet applied |
| `continuation-stalled` | info | a continuation took more than 20 s from queued to applied (`appliedAt - queuedAt` on its marker); the stats give continuation latency percentiles |

`CheckerTest` plants each error in a synthetic run and asserts that exactly that check fires.

## Mutant runs

A mutant is a deliberate bug that the bench must catch: a mutant run must exit 1, failing a check
(`lost-signal`, `duplicate-signal`, `incomplete`, ...) or an independent check. The bugs are not in
the shipped server or SDK. Each is a patch in `mutants/`:

| Directory | Applies to | Mutants |
|---|---|---|
| `mutants/server/` | the server repository | `no-completion-deferral`, `no-variable-fencing`, `ignore-epoch`, `join-ignores-lanes`, `workflow-saved-last`, `no-claim-fence`, `destructive-pull`, `no-stale-write-guard`, `terminate-on-storage-errors` |
| `mutants/sdk/` | this repository | `no-message-dedupe`, `drop-some-signals`, `no-reply-prune` |

Apply a patch to a scratch copy, build it, and bench the copy. For a server mutant:

```bash
git -C /path/to/conductor worktree add /tmp/conductor-mutant
git -C /tmp/conductor-mutant apply "$PWD/native-examples/mutants/server/ignore-epoch.patch"
(cd /tmp/conductor-mutant && ./gradlew :conductor-server:bootJar -x test)
# BENCH_SERVER_SCRIPT runs /tmp/conductor-mutant/server/build/libs/conductor-server-*-boot.jar
BENCH_SERVER_SCRIPT=/path/to/run-mutant-server.sh ./native-examples/bench.sh run   # must exit 1
git -C /path/to/conductor worktree remove /tmp/conductor-mutant
```

For an SDK mutant, apply the patch to a worktree of this repository and run the bench from there
(`./gradlew :native-examples:installDist`, then `./native-examples/bench.sh run`), against an
unpatched server. A worktree starts from the last commit, so commit the code under test first.

## Independent checks

`independent/check.sh <run-dir>` checks a run with two tools that share no code with this bench,
so a bug in the bench's checker cannot hide a failure from them:

| Tool | What it is given | Property |
|---|---|---|
| Elle (Jepsen, `elle:elle:0.2.8`) | each deposit as `[:append <execution> N]`, each query answer as `[:r <execution> [...]]`; a client that saw an indefinite result is retired, as Jepsen does | **serializable**: one order of appends explains every read; no failed append is read, nothing duplicated, no contradictory orders. **strict serializable over the signals and the queries made after every execution ended**: by then everything acknowledged is applied, so a lost signal is a real-time violation |
| Porcupine 1.3.1 | each `bump` update as a fetch-and-increment returning the new value (an indefinite one may or may not have happened), each execution's final count as a read at the end | **linearizable**: one order consistent with real time in which every returned value is the counter's value |

Elle also reports strict serializability over all queries. It fails by design: a query reads
applied state, so it can miss a signal acknowledged just before it.

```bash
native-examples/independent/check.sh native-examples/build/durability/<run>
```

Elle runs from its own small Gradle project (`independent/elle`, pulling Elle from Clojars);
Porcupine from a Go module (`independent/porcupine`).

Both were shown to reject bad histories: Elle flags a query answer with a duplicated element
(`:duplicate-elements`, `:incompatible-order`) and, on the run that lost signals to a stale pull,
fails the final-reads check on the same lost deposit this bench's checker reported; Porcupine
rejects a history in which two updates return the same value.

## Update flood

`durability.UpdateFlood` sends many concurrent updates and queries to one execution and checks
that the returned counter values are exactly 1..N (each update applied once, in one order), that
the stored update replies stay within their 500-reply limit, and that the execution still
completes.

```bash
CONDUCTOR_SERVER_URL=http://127.0.0.1:8100/api \
java -cp "native-examples/build/install/native-examples/lib/*" durability.UpdateFlood 1000 20
```

Against the three-server cluster: 1000 updates from 20 threads in 40 s, values exactly 1..1000,
79 queries answered meanwhile, replies at the 500 cap.

## Output

The run directory holds `config.json`, `history.jsonl`, `ledger/*.jsonl`, `final.json`,
`report.json`, worker logs, and the server start logs. `final.json` summarizes every execution
with its `nativeState` (bootstrap and handler markers, replies, seen ids) and, per task,
its `continuation` marker next to the usual fields. The report prints a table of every check with
its count, statistics (operations by outcome, faults injected, execution statuses, duration
percentiles, step attempts, continuations), and the first 40 errors.

## What it found

Each row is a problem a run exposed, and the fix. All are fixed on the `design/native-workflows`
branches of the server and this SDK.

| Run | Finding | Fix |
|---|---|---|
| no faults | a JOIN completed while a lane's last continuation still had tasks to append; the execution failed | a completed continuation ends its lane only after it was applied and appended nothing |
| no faults | an update sent to an execution that then failed waited three minutes for a reply | the client stops waiting when the execution is terminal |
| all faults | 39 acknowledged signals lost: a lost poll response stalled the handler lane, the execution completed, and its queue was deleted | completion is deferred while messages are queued or a handler runs; discarded messages are logged and counted; continuation timeout 30 s instead of 120 s (an hour in dev mode) |
| heavy pauses and partitions | signals applied twice, and one update applied twice, after the client re-sent a message whose first send had succeeded | client-chosen message ids, de-duplicated by the handler lane |
| heavy pauses and partitions | an execution failed when a JOIN read a refused, about-to-be-retried continuation | live-target JOINs wait until the branch's continuation marker is applied and ended the lane |
| worker and server kills | signals lost: a continuation's stale copy of the variables overwrote a handler's write | completions apply only the variables they changed (`resumedWith`), and a conflicting completion runs again on the current values |
| server kills | executions failed after a deferral: the loop that drove the handler lane read the set-aside attempt | a deferred completion puts the marker back to runnable with the reason; nothing is set aside |
| server kills | an execution stuck forever: the server died mid-apply after saving the continuation as applied but before saving the definition with its fragment | the workflow row is saved before the task that carries the marker when a continuation is applied |
| chaos script | a start failed with a retryable server error right after a restart, and the client gave up | starts with an idempotency key retry server errors for up to two minutes |
| server pauses | an execution failed as diverged: a worker replayed a completion the server had saved but not yet applied, and expected tasks that did not exist yet | the SDK replays only `APPLIED` continuation markers |
| chaos script, protocol 2, with per-worker queues (since removed) | a worker was killed right after its continuation was queued to its own domain queue; the sweeper had postponed the workflow's decide by an hour on the strength of its in-progress tasks, so nothing re-routed the wake-up and the execution waited | `WorkflowExecutorOps.decide` caps the postpone at the normal workflow offset (30 s) while a continuation marker is `RUNNABLE`, so `repairAndWake` re-pushes the wake-up to a live queue on the next decide |

The bench had a bug of its own. After a server kill it could start the server twice: its health
wait saw no process on the port while the new server booted, and started another. The instance
that lost the race for the port never exited. It kept running sweepers and system-task workers
against the shared database with no HTTP endpoint. Runs before the fix therefore had several
headless servers on one database. That load also slowed restarts to two minutes. The bench now
tracks the server process it started and never starts a second while it boots.

The earliest finding came from the kitchensink, before the bench existed. Concurrent pulls and
pushes on SQLite failed with SQLITE_BUSY_SNAPSHOT, and the failed pull stopped the handler lane.

## Last runs

Both ran on the final builds, with one server process, and passed every check.

| Seed | Faults injected | Executions | Signals | Updates | Result |
|---|---|---|---|---|---|
| 2027 | 4 server kills, 4 server pauses, 3 worker pauses | 40 | 544 | 113 | valid |
| 2026 | 5 worker kills, 1 server kill, 2 server pauses, 3 worker pauses | 40 | 895 | 90 | valid |

In every run, some steps succeeded more than once and some continuations were retried.
At-least-once task execution allows both. Continuations that took over 20 s were all ones caught
by a pause, a partition or a server restart.

One failure seen once is not explained. An execution failed with "Error scheduling tasks": an
exception while creating or starting newly scheduled tasks terminates the execution. Its server
log was overwritten by a restart before it could be read, and the failure did not recur in later
runs with logs kept. A transient database error during task creation is the likely trigger. If
so, Conductor terminating an execution on a transient error while scheduling is worth changing.

## Cluster mode

With `BENCH_CLUSTER=8101,8102,8103` the bench runs against several Conductor servers sharing one
Postgres database (database, queues, index and the execution lock, `workflow-execution-lock.type=postgres`).
The workers connect through a load-balancing proxy across all servers, which the `partition`
fault cuts. Each server reaches the database through its own proxy, so `db-partition` isolates
one server from the database while the others keep running. Clients use their own balancer and
are never partitioned. Four more faults apply:

| Fault | How |
|---|---|
| `db-pause` | `docker pause` the database container for 5 to 25 s |
| `db-kill` | `docker kill` the database (SIGKILL), then start it again; Postgres recovers from its WAL |
| `db-partition` | reset and hold one server's database connections for 5 to 30 s |
| `cluster-kill` | SIGKILL every server, then start them one at a time |

`BENCH_NEMESES=2` or `3` runs that many fault injectors at once, so faults overlap; each fault
holds the process it acts on, so two never act on the same server or worker at once.

The server script receives the HTTP port and the database port. The one used here:

```bash
java -jar conductor-server-boot.jar --server.port="$1" \
  --conductor.db.type=postgres --conductor.queue.type=postgres \
  --conductor.indexing.enabled=true --conductor.indexing.type=postgres \
  --conductor.workflow-execution-lock.type=postgres \
  --spring.datasource.url="jdbc:postgresql://127.0.0.1:$2/conductor" \
  --conductor.workflow-message-queue.enabled=true
```

### Cluster findings

| Run | Finding | Fix |
|---|---|---|
| no faults | two of three workers crashed at startup: concurrent registration of the same definitions raced in Postgres and failed on the primary key | atomic `INSERT ... ON CONFLICT DO UPDATE` for task and workflow definitions; the SDK retries registration |
| overlapping faults | a handler lane stalled for an hour: the server running its message pull died, and the executor had reserved the pull's queue message for the hour-long default response timeout | messages are claimed, not popped, so a re-run returns the same ones: the handler lane's poll claims them under `handlers#<n>` with a 30 s lease; a JSON workflow's `PULL_WORKFLOW_MESSAGES` claims under its reference name and, like JOIN, declares itself safe to re-run for a one-minute reservation |
| overlapping faults | a database kill failed a handler loop: scheduling the next DO_WHILE iteration hit a closed connection, and the loop failed itself and the workflow | storage outages (lost connection, rolled-back transaction, database shutdown, HikariCP's stateless "Connection is closed") abort the decide for a retry instead of terminating; DO_WHILE rethrows |

| three overlapping nemeses, with queries | the close signal and three deposits lost: a completed pull ran again after a pause, claimed the newest messages under its old name and overwrote its record; the current pull deleted them as delivered | claims fenced by an ordinal (the handler run number `n`; a loop iteration for a JSON workflow's pull); the executor does not save over a task another run completed |

Also fixed after reading the code against Jepsen's Orkes findings, without the bench hitting them:
starts queue the workflow before writing it (abandoned workflows), starts never delete a workflow
they may have written (lost workflows), and the sweeper re-reads a task before re-queueing it
(concurrent execution of a task a worker had just polled).

### Cluster runs on the final builds

| Seed | Nemeses | Faults injected | Executions | Signals | Updates | Result |
|---|---|---|---|---|---|---|
| 4 | 2 | 3 cluster kills, 5 db kills, 3 db pauses, 1 db partition, 1 partition, 3 server pauses, 3 worker kills, 2 worker pauses | 50 | 877 | 85 | valid |
| 5 | 2 | 5 cluster kills, 1 db kill, 1 db pause, 2 db partitions, 4 partitions, 2 server pauses, 3 worker kills, 4 worker pauses | 50 | 945 | 54 | valid |
| 6 | 3 | 4 cluster kills, 1 db kill, 1 db pause, 3 partitions, 3 server kills, 2 server pauses, 1 worker kill | 50 | 1105 | 316 | valid |

With queries and the update-order check, on the final builds:

| Seed | Nemeses | Faults injected | Signals | Updates | Queries | Result |
|---|---|---|---|---|---|---|
| 8 | 3 | 2 cluster kills, 1 db kill, 3 db pauses, 3 db partitions, 4 partitions, 4 server kills, 5 server pauses, 3 worker kills, 3 worker pauses | 971 | 118 | 1440 | valid |
| 9 | 3 | 2 cluster kills, 2 db kills, 1 db pause, 4 db partitions, 1 partition, 1 server kill, 3 server pauses, 1 worker kill, 2 worker pauses | 1101 | 480 | 1499 | valid |

Between 20 and 45 percent of queries missed a signal acknowledged before the query was sent
(`query-lag`): queries read applied state, and under faults the handler lane runs behind.

Earlier cluster runs with one nemesis (database-heavy, all faults, 40 to 50 executions) also
passed. The kitchensink and the operator checks pass against the cluster through a balancer, with
three worker processes, and a `@Schedule` workflow runs exactly once per tick with three
schedulers.

## Known limits of the bench

- Single machine: every process shares one host, so there is no clock skew between nodes and no
  real network latency. Partitions are made by proxies, not by the network.
- Redis queues and MySQL are not exercised by the bench (MySQL only by its unit tests).
- Clients are never partitioned from the servers.
- Runs last minutes, not hours, so rare interleavings need many runs. Vary `BENCH_SEED` and run it
  in a loop to search longer.
