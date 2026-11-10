# Observed power-duration curve

Status: proposed calculation specification; T04 depends on reviewed T02/T03 timing policy.

Durations initially: 5, 30, 60, 180, 300, 600, 1200, 1800, 3600 seconds. A missing duration is unavailable, not zero. Report observed best rolling mean, not a prediction of physiological maximum.

For a completely covered window [t−d,t], mean power is integral(P(u), u=t−d..t)/d, in watts. Use time-weighted piecewise intervals; taking N samples is only equivalent for verified uniform sampling. Valid zero power is included. Missing/invalid power, expiry, recording pause and process boundary break coverage. No qualifying window may bridge these breaks. HR gaps do not invalidate a pure power observation.

Use bounded rolling interval buffers with cumulative energy/time and correct fractional boundaries. A simple brute-force integral implementation in tests is the reference; optimize only after matching it. Persist duration/window/contributor, coverage/timing provenance and preprocessing version. Test maxima near both insertion and expiration boundaries, not merely at regular bin ticks; declare normalization resolution in the policy.

Acceptance examples: 300 seconds of fully covered 200 W produces 200 W at durations ≤300 and unavailable at longer durations. Fully covered 5-second sequence 0/100/200/300/400 W, one second each, produces 200 W for 5 seconds. A missing middle interval prevents a 5-second record across it. A 60-second pause is not removed to concatenate otherwise separate efforts.

Historical all-time curve takes per-duration maxima of valid per-session records. A recent-window view must be labeled with horizon. Incremental upsert compares new records; deleting a winning ride or correcting its source recomputes only affected duration/cohort records. Never silently compare preprocessing versions.
