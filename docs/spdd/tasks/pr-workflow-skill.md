# Project PR workflow skill

Owner requested this skill on 2026-11-11 after the PR #9 naming correction.

Goal is repeatable OxyWatt PR preparation with self-review, Conventional Commit subjects and titles, correct GitHub identity and actual verification evidence. Scope is the project skill, AGENTS routing and Canvas/decision synchronization. Existing repository authorization and developer workflow are dependencies.

Expected files are `.agents/skills/oxywatt-pr/SKILL.md`, `AGENTS.md`, Canvas, decision log and this task. Verification uses the installed skill-creator validator, local Markdown links and `git diff --check`. Review that a correct commit is preserved when only the PR title needs correction, unrelated work stays intact, and publication follows the user's requested scope.

Risk is adding unnecessary approval or history-rewrite requirements. The skill applies existing session authorization and checks both names independently. This workflow adds no product requirement or V scenario.

## Evidence

Installed skill-creator quick_validate.py passed. All 80 local Markdown file targets resolve. git diff --check passed. Manual review confirmed that PR-title correction preserves a valid commit and publication uses existing session authorization.
