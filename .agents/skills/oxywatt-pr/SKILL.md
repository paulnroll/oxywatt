---
name: oxywatt-pr
description: Prepare, self-review, create or update an OxyWatt pull request, including Conventional Commit titles, repository GitHub identity and verification evidence.
---

# OxyWatt pull requests

Read root AGENTS.md and the named task. Use the existing session authorization for publication. Creating this skill alone authorizes no push, PR, merge or release.

## Prepare the change

Inspect the working tree, branch and full diff against the intended base, including untracked files. Preserve unrelated work. Fetch origin and incorporate the current base when requested or needed for a correct review. Choose merge, rebase or a fresh branch according to the history. Preserve unpublished work before switching branches.

Self-review the final diff for correctness, scope, privacy, requirement/task/test traceability and documentation consistency. Fix concrete issues and run checks appropriate to the approved slice. Record actual results and remaining limitations. Documentation changes need link/traceability and whitespace checks. SDK source builds, published-package builds, simulated tests and hardware checks have distinct evidence.

## Commit and PR title

Use Conventional Commits for both commit subjects and the PR title. The PR title is often the squash commit message, so check it independently.

Use `type(scope): imperative summary`, with scope optional. Choose the type from the final change, for example `docs: propose simulation-first testing architecture`, `test(core): verify FIT replay` or `feat(android): add Karoo diagnostic probe`. Use English and describe the final scope.

When correcting naming, inspect both commit subjects and the PR title. Fix the offending text. Preserve a correct commit when only the PR title needs correction. If a pushed feature commit must be amended, verify the remote head and use an explicit force-with-lease tied to that observed head. Preserve concurrent work and shared base history.

## Publish and verify

Run repository GitHub commands from the root with `GH_CONFIG_DIR=.git/gh-personal`. Check the configured identity before diagnosing access errors. Keep the configuration and credentials private.

Stage intended files explicitly. Inspect the staged diff and `git diff --cached --check`. Exclude private rides, logs, GPS, credentials, keystores and generated APKs. Commit and push the intended feature branch when publication is authorized.

Check for an existing PR for that head/base before creating another. Use an English Conventional Commit title. Write multiline descriptions to a temporary file and pass `--body-file`. Lead with the concrete problem and resulting behavior. Include validation and material gaps. When scope changes, update the title and description.

Read the remote PR back after creation or update and verify base, head commit, title and URL. Report pending CI accurately. Monitor or fix CI through completion when the user's request includes that work. Merge and release require their own authorization.

Return the PR link and concise verification/limitations. When the user requests a new-session handoff, include canonical document paths, the pending decision and the next authorized step.
