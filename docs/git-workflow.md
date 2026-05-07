# Git Workflow And Deployment Strategy

This document describes the recommended Git strategy for OnTime-back when running both a development server and a production server.

## Goals

- Keep one clear production source of truth.
- Support a separate development server for integration testing.
- Make every server deployment traceable to a Git branch and commit.
- Avoid using deployment branches as places where product code diverges.
- Keep feature branches short-lived and easy to review.

## Branch Model

Use a lightweight environment-branch workflow:

```text
feature/*, fix/*, chore/*  -> short-lived work branches
dev                        -> development server source
main                       -> production server source
```

Branch responsibilities:

| Branch | Purpose | Deployment |
| --- | --- | --- |
| `main` | Production-ready code and source of truth | Production server |
| `dev` | Integrated code for QA, frontend/mobile testing, and pre-release validation | Development server |
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

6. GitHub Actions deploys the updated `dev` branch to the development server.

7. Validate the change using the development server.

8. When the release candidate is ready, open a pull request from `dev` into `main`.

```text
dev -> main
```

9. After review and CI pass, merge into `main`.

10. GitHub Actions deploys `main` to the production server.

11. Tag the production release.

```bash
git tag prod-YYYY-MM-DD
git push origin prod-YYYY-MM-DD
```

## Server Mapping

Use branch-based deployments with separate GitHub Actions environments:

```text
push to dev  -> development environment -> dev server
push to main -> production environment  -> production server
```

Recommended GitHub environments:

| Environment | Source Branch | Server | Approval |
| --- | --- | --- | --- |
| `development` | `dev` | Dev server | Usually automatic |
| `production` | `main` | Production server | Manual approval recommended |

## CI/CD Workflow

Use either two workflows or one branch-aware workflow.

Recommended simple setup:

```text
.github/workflows/test.yml
.github/workflows/deploy-dev.yml
.github/workflows/deploy-prod.yml
```

Expected triggers:

```text
pull_request to dev   -> run tests
pull_request to main  -> run tests
push to dev           -> deploy to dev server
push to main          -> deploy to production server
workflow_dispatch     -> allow manual redeploy or rollback support
```

Development deploy should use development secrets only:

```text
DEV_EC2_HOST
DEV_EC2_USER
DEV_EC2_SSH_KEY
DEV_DATASOURCE_URL
DEV_DATASOURCE_USERNAME
DEV_DATASOURCE_PASSWORD
```

Production deploy should use production secrets only:

```text
PROD_EC2_HOST
PROD_EC2_USER
PROD_EC2_SSH_KEY
PROD_DATASOURCE_URL
PROD_DATASOURCE_USERNAME
PROD_DATASOURCE_PASSWORD
```

Do not share databases, Firebase credentials, OAuth redirect URIs, or private keys between development and production unless there is a deliberate reason.

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
dev branch    -> development server
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
6. Add development deployment to trigger from `dev`.
7. Update GitHub environment secrets for `development` and `production`.
8. Protect `main` and `dev`.
9. Stop using `deploy` for new work.
10. Delete or archive stale merged feature branches after confirming they are no longer needed.

## Practical Rules

- New work branches from `dev`.
- Normal PR target is `dev`.
- Release PR target is `main`.
- Production deploys only from `main`.
- Development deploys only from `dev`.
- Do not commit directly to `main`.
- Do not commit directly to `dev` unless it is an emergency coordination fix.
- Delete feature branches after merge.
- Keep database migrations forward-only.
- Keep environment-specific values in GitHub secrets, not committed files.
