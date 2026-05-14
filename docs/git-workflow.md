# Git Workflow And Deployment Strategy

This document describes the recommended Git strategy for OnTime-back with one production EC2 server, one private production RDS instance, and one remote-PC development server.

## Goals

- Keep one clear production source of truth.
- Make every server deployment traceable to a Git branch and commit.
- Avoid using deployment branches as places where product code diverges.
- Keep feature branches short-lived and easy to review.
- Use `dev` as the integration branch and remote-PC development deploy source.

## Branch Model

Use a lightweight environment-branch workflow:

```text
feature/*, fix/*, chore/*  -> short-lived work branches
dev                        -> integration branch for CI and review
main                       -> production server source
```

Branch responsibilities:

| Branch | Purpose | Deployment |
| --- | --- | --- |
| `main` | Production-ready code and source of truth | Production server |
| `dev` | Integrated code for QA, frontend/mobile testing, and pre-release validation | Remote PC development server |
| `feature/*` | New feature work | No direct deployment |
| `fix/*` | Bug fixes | No direct deployment |
| `chore/*` | Maintenance, docs, config, CI changes | No direct deployment |

Avoid keeping a long-lived `deploy` branch as the production source. Production should be represented by `main`, while deployment behavior should be handled by GitHub Actions environments and secrets.

## Normal Development Workflow

1. Start from the latest `dev`.

```bash
git checkout dev
git pull origin dev
git checkout -b feat/some-feature
```

2. Commit focused changes on the feature branch.

```bash
git add .
git commit -m "feat: add some feature"
```

3. Open a pull request into `dev`.

```text
feature/* -> dev
```

4. CI runs tests and build checks.

5. After review, merge into `dev`.

6. Validate the integrated code on the remote PC development backend.

7. When the release candidate is ready, open a pull request from `dev` into `main`.

```text
dev -> main
```

8. After review and CI pass, merge into `main`.

9. GitHub Actions deploys `main` to the production server.

10. Tag the production release.

```bash
git tag prod-YYYY-MM-DD
git push origin prod-YYYY-MM-DD
```

## Server Mapping

Use branch-based CI and production deployment:

```text
pull_request to dev/main -> test workflow
push to dev  -> development environment -> remote PC development server
push to main -> production environment  -> production server
```

Recommended GitHub environments:

| Environment | Source Branch | Server | Approval |
| --- | --- | --- | --- |
| `development` | `dev` | Remote PC development server | Optional |
| `production` | `main` | Production server | Manual approval recommended |

## CI/CD Workflow

Recommended simple setup:

```text
.github/workflows/test.yml
.github/workflows/deploy-dev.yml
.github/workflows/deploy.yml
```

Expected triggers:

```text
pull_request to dev   -> run tests
pull_request to main  -> run tests
push to dev           -> deploy to remote PC development server
push to main          -> deploy to production server
workflow_dispatch     -> allow manual redeploy for each deploy workflow
```

Production deploy should use production secrets only:

```text
EC2_HOST
EC2_USER
EC2_SSH_KEY
SPRING_DATASOURCE_URL
SPRING_DATASOURCE_USERNAME
SPRING_DATASOURCE_PASSWORD
```

Development deploy should use development secrets only:

```text
DEV_REMOTE_HOST
DEV_REMOTE_USER
DEV_REMOTE_SSH_KEY
```

The development deploy uses the workflow `GITHUB_TOKEN` for GHCR image pulls. Optional `DEV_*` secrets can override the default dev deploy directory, HTTP port, MySQL credentials, and non-production OAuth/Firebase settings.

## Branch Protection

Protect `dev`:

- Require pull requests before merging.
- Require CI tests to pass.
- Allow squash merge.
- Allow faster review for low-risk changes.

Protect `main` more strictly:

- Require pull requests before merging.
- Require CI tests to pass.
- Require review approval.
- Require the PR branch to be up to date before merge.
- Block force pushes.
- Block direct pushes.
- Require production environment approval before deployment.

## Release And Rollback

Every production deployment should be traceable by commit SHA and tag.

Recommended release tags:

```text
prod-2026-05-07
prod-2026-05-07-1
v1.3.0
```

Rollback options:

1. Redeploy a previous image tag by commit SHA.
2. Revert the bad commit on `main` and deploy the revert.
3. If the issue only exists in configuration, update production secrets or environment config and rerun deployment.

Prefer image-SHA rollback for urgent incidents, then follow up with a Git revert or fix PR so `main` still reflects production truth.

## Migration From Current State

The current repository has `main` and `deploy` as separate long-lived branches. The target state should be:

```text
deploy branch -> retired
main branch   -> production
dev branch    -> integration and remote PC development deployment
```

Recommended migration sequence:

1. Compare `deploy` and `main`.
2. Merge or cherry-pick all intended production changes from `deploy` back into `main`.
3. Confirm `main` builds and tests successfully.
4. Create `dev` from the updated `main`.

```bash
git checkout main
git pull origin main
git checkout -b dev
git push origin dev
```

5. Change production deployment to trigger from `main`.
6. Ensure `dev` deploys only to the remote PC development environment.
7. Update GitHub production environment secrets.
8. Update GitHub development environment secrets for the remote PC.
9. Protect `main` and `dev`.
10. Stop using `deploy` for new work.
11. Delete or archive stale merged feature branches after confirming they are no longer needed.

## Practical Rules

- New work branches from `dev`.
- Normal PR target is `dev`.
- Release PR target is `main`.
- `dev` deploys only to the remote PC development server.
- Production deploys only from `main`.
- Do not commit directly to `main`.
- Do not commit directly to `dev` unless it is an emergency coordination fix.
- Delete feature branches after merge.
- Keep database migrations forward-only.
- Keep environment-specific values in GitHub secrets, not committed files.
