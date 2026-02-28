# Luna Parity Performance Harness

This document defines the reproducible baseline and regression process for the `Neosback/luna-memory-speed-parity` branch.

## Goal

Measure allocation/GC/tick stability before and after the Luna-parity refactor under the same load profile.

## Fixed Scenario

1. Runtime duration: `10 minutes` steady-state capture.
2. Player load: `~100 active players` (or stress clients) in the same region.
3. Movement profile: identical script/pathing pattern across baseline and candidate runs.
4. Server tick: unchanged (`600ms`).

Do not compare runs with different map density, movement behavior, or duration.

## Capture Commands

1. Build the server jar:

```bash
./gradlew :game-server:jar
```

2. Start capture for 10 minutes:

```bash
./game-server/scripts/run_luna_parity_perf_capture.sh 600
```

Optional: override Netty leak detector level (default is `paranoid` in the script).

```bash
NETTY_LEAK_LEVEL=simple ./game-server/scripts/run_luna_parity_perf_capture.sh 600
```

3. Summarize GC log:

```bash
./game-server/scripts/summarize_gc_log.sh <capture_dir>/gc.log
```

## Required Metrics

Record all values for both `baseline` and `candidate`:

1. Allocation rate (MB/s) from JFR or VisualVM sampler.
2. Young GC count/minute.
3. Young GC total pause time/minute.
4. Old generation trend (stable / growing / shrinking).
5. Full GC count.
6. Tick timing:
   - p95 tick duration
   - max tick duration

## Acceptance Gates

1. Allocation rate reduced by `>= 35%`.
2. Young GC frequency reduced by `>= 30%`.
3. No full GC during steady-state.
4. p95 tick time improved with no sustained lag spikes.

## Report Template

| Metric | Baseline | Candidate | Delta | Pass |
|---|---:|---:|---:|---|
| Allocation MB/s |  |  |  |  |
| Young GC / min |  |  |  |  |
| Young pause ms / min |  |  |  |  |
| Full GC count |  |  |  |  |
| Tick p95 ms |  |  |  |  |
| Tick max ms |  |  |  |  |

## Artifacts Checklist

Each run directory should contain:

1. `gc.log`
2. `capture.jfr`
3. `server.log`
4. scenario notes (`players`, `region`, `movement script`)
