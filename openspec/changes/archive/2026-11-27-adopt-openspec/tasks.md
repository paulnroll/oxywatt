# Tasks

## Adoption

- [x] Install npm OpenSpec outside the repo and initialize globally scoped Codex skills.
- [x] Add project context and capability/authority map using the packaged schema.
- [x] Migrate T08b1 with original R/T/V/F traceability and explicit F08 NOT RUN.
- [x] Route AGENTS.md, README, Canvas, decisions and project skills through the map, preserving prior work.
- [x] Verify OpenSpec, external paths, skill metadata, links/anchors, traceability and whitespace; record evidence.

## Evidence

Verified on 2026-11-27:

- OpenSpec 2.2.0 global npm installation and global-scope initialization PASS;
  seven generic skills in `~/.agents/skills/`, none added in the repository.
- `openspec validate --all --strict --no-interactive --json`: adoption change and
  saved-curve-field baseline both valid, no warnings. The info message confirms
  intentional `skip_specs` for the tooling-only change. Initial long-requirement
  warnings were fixed by separating behavior requirements without changing scope.
- `openspec templates --json`: all four template paths resolve from the external
  npm package. No project npm manifest, lockfile, node_modules or custom schema.
- `openspec update --dry-run --json`: no changes, notes or failures.
- Installed skill-creator `quick_validate.py`: oxywatt-spec, oxywatt-developer and
  oxywatt-pr all PASS.
- Documentation check: 17 Markdown files, 420 relative links/anchors PASS;
  F01–F08 and referenced R/V IDs resolve to the original acceptance documents.
- `git diff --check` PASS; new OpenSpec file whitespace also checked.
- Native archive completed as `2026-11-27-adopt-openspec` with no delta sync;
  archived task completion and final strict baseline validation PASS.

Prior T08b1 tests and installation remain linked historical evidence. No JVM or
Android tests, source synchronization, hardware/private-input checks, publication,
merge or release were performed. F08 remains NOT RUN. Pre-existing local changes
and original task/evidence anchors were retained.


## Owner follow-up: project npm tooling and publication review — 2026-11-27

The owner replaced the initial global-only CLI layout with project package.json
and package-lock.json, then authorized self-review and PR publication. Original
adoption evidence above describes the earlier local stage; its no-manifest and
F08 NOT RUN statements are historical, not the final PR disposition.

Final scope: exact OpenSpec 2.2.0 devDependency, private developer-tools package,
ignored node_modules and npm CLI/check/list scripts with telemetry disabled.
Generic skills remain global. The global install is no longer required to run
project commands. No runtime/build dependency is added to the Android app.

Self-review found the initial checkout/evidence behind main. PR preparation uses
an isolated branch from dd7edd9; newer main authorization and task history are
retained. F08 was corrected to PARTIAL from the existing physical editor/binding/
rounding/date/HOME-reopen evidence, with untested cases explicitly retained.
No new device/private-input check was performed. Unrelated mirrored documentation
and design files in the original checkout are excluded from this PR.

Final follow-up verification PASS:

- Clean `npm ci --ignore-scripts --no-fund --no-audit`: 66 packages installed from
  lockfile. npm scripts resolve the local OpenSpec 2.2.0 binary; the global CLI is
  not used. `npm run spec:check` and archived-task validation PASS.
- All four template paths resolve from the ignored local npm package; node_modules
  is ignored and no generic skills/schema sources are tracked.
- Final branch check: 13 Markdown files / 277 relative links and anchors PASS;
  F01–F08 and referenced R/V traceability PASS; all three changed project skills
  pass the installed skill-creator validator.
- Lockfile uses public npm registry URLs/integrity hashes with no local paths or
  credentials. New-file whitespace and `git diff --check` PASS.
- Review against dd7edd9 includes only specification/workflow/npm files, preserving
  later authorizations and original task/evidence anchors. No Kotlin, Android,
  Gradle, SDK, scientific-model or CI configuration change.

Publication is authorized. Merge/release and additional device/private-input work
remain outside this change. CI will verify existing project checks on the PR.
