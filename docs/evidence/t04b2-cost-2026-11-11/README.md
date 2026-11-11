# T04b2 baseline cost investigation — 2026-11-11

Software-only measurements on unchanged merged PR #24 production classes (`26e1875`). No private FIT, device, new numerical implementation or physiological evidence. [Proposed package](../../spdd/tasks/t04b2-long-variable-scan.md).

The existing PowerCurveAcceptanceTest suite was rerun with:

```sh
./gradlew :core:test --tests '*PowerCurveAcceptanceTest' --rerun-tasks --console=plain
```

Passed, 11 tests, zero failures. Existing four-hour counter: 243659 windows, 187673925 integrated overlapping fragments, 3600 retained intervals. This checks the engine directly, bypassing wrapper limits.

[OxyCost.java](OxyCost.java) is a standalone investigation driver of the unchanged public classes, not application implementation or an independent numerical oracle. It feeds identical intervals to direct T04a and BudgetedPowerCurve, separating actual engine work from conservative wrapper reservations. Reproduce after core compilation, using the installed Kotlin stdlib:

```sh
KOTLIN_LIB=$(find "$HOME/.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-stdlib/2.3.20" -name '*.jar' | head -1)
javac -d /tmp -cp "core/build/classes/kotlin/main:$KOTLIN_LIB" docs/evidence/t04b2-cost-2026-11-11/OxyCost.java
java -cp "/tmp:core/build/classes/kotlin/main:$KOTLIN_LIB" OxyCost
```

| Input | Direct windows | Direct fragments | Final retained | Wrapper admitted | Wrapper reserved | Limited |
|---|---:|---:|---:|---:|---:|---|
| Four hours alternating 100/200 W, 1 Hz | 243659 | 187673925 | 3600 | 186 | 19773567 | yes |
| Four hours irregular, repeating 250000001/749999999/1500000000/1500000000 ns | 268823 | 188198516 | 3600 | 186 | 19773567 | yes |
| 2000 alternating 1 ms intervals then 3600 s at 50 W | 18017 | 18027017 | 1 | 186 | 19773567 | yes |
| 14400 one-second intervals with explicit break + 10 s gap every 1800 inputs | 163649 | 37596050 | 1 | 186 | 19773567 | yes |

These are deterministic operation counts, not timings. The last fixture spans more than four elapsed hours because of gaps. Dense-then-long is a confirmed-interval core workload, not a claim that canonical silence confirms one hour of power. Pause/missing/expiry/loss all clear engine coverage; the driver models the numerical break only, not persisted lifecycle correctness. End-to-end coverage for those cases remains planned B2-04/B2-07.

Wrapper reserves 9×(n+3)×(n+1) before every append, including cases with few candidate ends. For an uncoalesced prefix n=0…185, the sum is 19773567; the next append would exceed 20M. Thus it stops independent of actual fragmented integration. Coalesced constant power stays at n=1 and avoids this cubic cumulative reservation growth. Direct integration remains potentially quadratic in retained fragments for one dense-then-long append: each expiration candidate revisits overlapping leaves. These observations motivate improving integration and charging actual bounded work together rather than raising the old cap.

No optimized-engine correctness, new budget compliance, complete persisted long-session replay or physical-device performance is established here. Those are acceptance work for T04b2 after named approval.
