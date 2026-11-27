# Saved power curve ride field

## Purpose

Specify the bounded, read-only graphical Karoo field over an explicitly selected
saved local session. This migrated T08b1 contract does not approve live analysis,
general history aggregation, FTP or VO2max. R03/R04/R07/R08/R09/R10/R11/R12 apply.

Evidence: [T08b1](../../../docs/spdd/tasks/t08b1-saved-curve-field.md).
Recorded F01–F07 software PASS and 0.9.0 installation/limited UI PASS do not close
F08 is PARTIAL: later explicit binding, matching rounded value/date, physical native
editor large/smaller Dark and smaller Light, and app HOME/reopen passed. Recording-time
ride-page behavior, process-death survival, all tiny/error states and Light full-page
remain unverified. The baseline was reconciled against main dd7edd9 and the latest
T08b1 evidence during publication review; no fresh hardware check was performed.

## Requirements

### Requirement: T08b1 explicit durable saved-session binding
The field SHALL use one explicitly bound local session UUID, one supported duration
and Light or Dark appearance. Defaults SHALL be 5 min and Light. The small versioned
preference SHALL store identity/settings rather than curve values or list rank.
Diagnostic selection alone SHALL NOT retarget the field. Bind, Clear and restore
SHALL NOT request analysis, retry or collection. Mapping: F02 → R07/R08/R12 → V09/V16/V23 software.

#### Scenario: Restore an older bound session
- **GIVEN** a supported saved real-provenance session explicitly bound at 5 s/Dark
- **WHEN** diagnostic selection changes or the process is recreated
- **THEN** the same UUID/duration/appearance is restored and revalidated before numbers appear
- **AND** no newer session is substituted, even outside the 20-row list

#### Scenario: Clear, invalid settings or refused candidate
- **WHEN** the user clears the binding, settings are corrupt/unknown, or a candidate cannot be bound
- **THEN** Clear returns to setup, invalid settings show a setup error, and a refused candidate retains the prior binding's identity/date/result with separate configuration feedback
- **AND** no analysis generation is created

### Requirement: T08b1 exact-session newest-result authority
The field SHALL validate the exact UUID and current authoritative real result.
Loading, missing/deleted session, pending/failed/unavailable newest result,
unsupported/mismatched provenance and read errors SHALL have explicit states.
They SHALL NOT expose an older generation, stale success or another session's
numbers. Mapping: F03 → R03/R07/R12 → V04/V09/V14/V16.

#### Scenario: A newer result supersedes success
- **GIVEN** a bound session formerly had an available curve
- **WHEN** its newest result becomes pending, failed or unavailable
- **THEN** the field displays the reason without older-result numeric fallback

#### Scenario: Initial load or repository validation fails
- **WHEN** validation is incomplete, the bound session is deleted, a slot/session/codec/acquisition mismatch is detected, or reading fails
- **THEN** the field displays loading, removed, invalid/unsupported or read-error status as applicable without a cached numeric flash or automatic retarget

### Requirement: T08b1 honest saved-value and chart semantics
The field SHALL use stored results for exactly 5/30/60/180/300/600/1200/1800/3600
seconds. The chart SHALL use log duration, linear watts, a zero baseline, no
smoothing/extrapolation and a selected available duration marker. Only adjacent
available points SHALL connect; missing is absent and valid zero stays zero.
Mapping: F04 → R04/R08 → V06 presentation/V10 software.

#### Scenario: Missing and zero points differ
- **GIVEN** saved values include zero watts, a missing duration and available durations on both sides
- **WHEN** the field renders
- **THEN** zero appears at the baseline and the missing point breaks the line
- **AND** the main value comes from the selected stored result, never the drawn line

### Requirement: T08b1 display rounding and limited scans
The field SHALL round stored watts only for display to whole watts.
Resource-limited curves SHALL visibly retain Limited scan and the unavailable
reason. Mapping: F04 → R04/R08 → V06 presentation/V10 software.

#### Scenario: Selected duration is unavailable
- **WHEN** the selected duration has no covered window or a resource-limit refusal
- **THEN** it displays an em dash and the corresponding reason
- **AND** other valid points can remain without claiming a whole-session maximum for a limited scan

### Requirement: T08b1 historical identity
The registered type SHALL be OxyWatt · Saved power curve. The readout SHALL label
Saved power with duration, watts/unit and the session's UTC first-local-receipt date;
the app SHALL explain that date without claiming native ride creation/save time.
Mapping: F01/F05 → R08/R09/R11 → V10/V11 software/V13/V22/V23 software.

#### Scenario: Present saved-session provenance
- **WHEN** a saved curve is available
- **THEN** the header labels Saved power with its chosen duration and the footer labels Session with the UTC receipt date
- **AND** the app explains that this is receipt time, not native ride save time

### Requirement: T08b1 accessible layouts
Light/Dark SHALL be explicit settings. Compact layouts SHALL omit the chart before
duration/value/date; tiny layouts SHALL request Enlarge field if these cannot fit.
English resource-backed text and marker shape SHALL carry meaning without color.
Mapping: F01/F05 → R08/R09/R11 → V10/V11 software/V13/V22/V23 software.

#### Scenario: Resize or change appearance
- **WHEN** a field becomes compact or too small, or Light/Dark is selected
- **THEN** it retains historical labels and value/date where they fit, otherwise shows Enlarge field
- **AND** accessible error/status descriptions do not rely on color or an inferred host-theme API

### Requirement: T08b1 existing-worker ownership and bounded projection
The field SHALL use the existing service-owned worker/repositories after canonical
work, with one coalesced exact-session/current-result read on binding/start,
bound-result completion or a subscribed stale check no more often than every five
seconds. It SHALL retain one immutable nine-duration snapshot, suppress unchanged
emissions and perform no raw replay/history scan.
Mapping: F06 → R09/R11 → V13/V17 software/V22.

#### Scenario: Subscribe while acquisition continues
- **WHEN** a field subscribes and canonical work is pending
- **THEN** canonical work retains priority, subscribed stale checks respect five seconds, and unchanged snapshots cause no render emission
- **AND** without a subscriber there is no periodic field stale query

### Requirement: T08b1 read-only field lifecycle
Attaching, hiding or detaching SHALL NOT start a source, Collector, database or
analysis, control acquisition or extend residency. An unavailable worker SHALL
show Open OxyWatt / History unavailable instead of opening independent storage.
Cancellation SHALL release scheduled work, subscriptions and bitmap references.
Mapping: F01/F06/F07 → R09/R10/R11 → V11 software/V12/V13/V17 software/V22.

#### Scenario: Detach, service destruction or binder failure
- **WHEN** a view detaches, the service is destroyed or its binder fails
- **THEN** that view's scheduled work/subscription stop without starting acquisition or changing source lifecycle

### Requirement: T08b1 rendering bounds
Each view SHALL bound its bitmap to 512 × 512 ARGB pixels (about 1 MiB) and coalesce
updates to at most once per second. Mapping: F06 → R09/R11 → V13/V17 software/V22.

#### Scenario: Oversized view or unavailable worker
- **WHEN** host dimensions exceed the bitmap bound or the worker is unavailable
- **THEN** the field scales a bounded bitmap or shows explicit worker guidance, respectively
- **AND** no alternate repository or background survival guarantee is introduced

### Requirement: T08b1 evidence and scope remain bounded
The field SHALL preserve existing engine, schema, hashes, storage and acquisition
policies and introduce no network/telemetry, permission, SDK or analytical dependency
change. Software/fake-host evidence SHALL stay distinct from hardware evidence.
Mapping: F07/F08 → R01/R04/R08/R09/R10/R11 → V01/V06/V10/V11/V12/V13/V22/V23 with original software/device qualifications.

#### Scenario: Report limited software and hardware acceptance
- **GIVEN** recorded F01–F07 software, installation and the limited F08 physical editor/binding/readout observations passed
- **WHEN** reporting acceptance
- **THEN** F08 remains PARTIAL, with recording-time, process-death, remaining layout/error states and Light full-page unverified
- **AND** acquisition/residency, resource endurance and physiological gates remain open
