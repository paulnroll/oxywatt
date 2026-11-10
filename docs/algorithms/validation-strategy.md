# Validation strategy

Status: planned scenarios, none executed. Stable scenario IDs link requirements to future tests. Developer FIT replay does not imply product import or historical API access.

| ID | Scenario and observable oracle | Requirements |
|---|---|---|
| V01 | Equivalent decoded Karoo/FIT values with identical timing policy produce equal analytics; receipt/source provenance remains different | R01 |
| V02 | HR dropout makes HR-dependent analysis unavailable without destroying valid power records; zero W remains a real value | R02 |
| V03 | Manual/auto pause boundaries and initial Idle do not create false completed rides; resume does not bridge windows | R03 |
| V04 | Crash during batch write preserves atomic checkpoint; replay same local sequence does not duplicate rows; repeated equal power is retained | R03 |
| V05 | Restart while recording vs restart into new ride leaves ambiguous sessions separate; unknown interval marked missing | R03 |
| V06 | Constant/ramp/irregular interval signals match independent integral reference; gaps, spikes and sample-frequency changes handled explicitly | R04 |
| V07 | Ordinary submaximal-only activity cannot force FTP; CP synthetic fit is not presented as FTP; manual baseline distinct | R05 |
| V08 | Invalid mass/HRmax, missing HR, unstable or insufficient segments refuse VO₂max; parameter sensitivity recorded | R06 |
| V09 | Same canonical inputs, profile, code/model/preprocessing/config versions yield same result within declared numeric tolerance | R07 |
| V10 | Raw vs smoothed chart, unavailable gaps, stale labels, last calculation and source references render truthfully; no cohort mixing | R08 |
| V11 | Karoo 3 four-hour recording with hidden/no field, screen off and app closed; pause, reconnect, low-memory kill and user force-stop tested separately | R09 |
| V12 | Offline ride works; network permission absent unless justified; no GPS/private fixture leakage; package secrets not logged | R10 |
| V13 | JVM engine independent of Android; long-run memory/queue/battery budgets measured against repeatable baseline | R11 |
| V14 | Upgrade real prior schema, recompute/delete winning contributor, transactional result writes and storage-full recovery | R12 |

## Test layers

Pure JVM unit/property tests cover time integration, normalization, lifecycle, model eligibility and trend math. Reference fixtures are independently calculated, not snapshots of the implementation. Repository integration tests exercise transactions/restart/migrations; Android tests cover adapter cleanup and UI state. Real Karoo tests establish lifecycle and performance. Do not equate JVM coverage with hardware validation.

## Datasets and privacy

Use deterministic synthetic traces first. Owner FITs stay in ignored `fixtures/private/`; derive minimal anonymized fixtures only with redistribution permission, stripping route, serials and identifying timestamps. Decoder must retain timer events, missing sentinels, units, timestamp semantics and source provenance. Choose parser/license in T01. No such FIT files have been supplied or processed yet.

## Physiological validation

Separate numerical conformance, robustness, test–retest stability and criterion validity. A lab reference must match athlete, modality and time window; ordinary FIT alone lacks measured VO₂max ground truth. Use participant-level training/validation splits, held-out comparison, MAE/RMSE/bias/limits of agreement and sensitivity to HRmax/mass/efficiency. Specify numerical acceptance targets before the experiment based on intended use; no scientifically justified accuracy threshold is selected yet. Confidence intervals require calibration evidence; repeated segments from one ride are not independent subjects.

No compatible licensed paired field-ride/CPET dataset secured. Record this explicitly in any validation report. Comparison with a Garmin display is exploratory only. Scientific gates remain open until evidence and model limitations are reviewed.
