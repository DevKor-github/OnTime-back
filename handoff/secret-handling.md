# Secret Handling Handoff

## Completed locally

- Added root ignore rules for `.DS_Store` and `.idea/`.
- Hardened `ontime-back/.gitignore` for `.env`, local Spring config, Firebase Admin SDK JSON files, Apple private keys, keystores, and `GoogleService-Info.plist`.
- Added safe templates:
  - `ontime-back/.env.example`
  - `ontime-back/src/main/resources/application.example.properties`
- Updated Firebase initialization so production/local deployments can use `firebase.credentials.path` / `FIREBASE_CREDENTIALS_PATH` instead of packaging the Firebase Admin SDK JSON in application resources.
- Removed already-tracked local files from the Git index while leaving local copies on disk:
  - `.DS_Store`
  - `.idea/`
  - `ontime-back/GoogleService-Info.plist`
- Updated Docker Compose to mount `/etc/ontime` read-only and read Spring config from `/etc/ontime/application.properties`.
- Updated deploy workflow to stage secrets outside `src/main/resources`, move them to `/etc/ontime` on EC2, set restrictive permissions, and remove verbose action debugging.
- Updated PR test workflow to store Firebase Admin SDK and Apple `.p8` files under the runner temp directory instead of `src/main/resources`.
- Tightened local permissions for the current ignored secret files where present.

## Still requires owner/admin action

1. Rotate secrets that may have been committed or copied into build logs:
   - database passwords
   - JWT signing secret
   - Google OAuth client secret
   - Kakao OAuth client credentials
   - Apple Sign in private key (`.p8`) and related key id if needed
   - Firebase Admin SDK service account key
   - EC2 SSH deploy key if it has appeared in logs or old artifacts

2. Clean Git history if this repository has been pushed publicly or shared outside the trusted team.
   Removing files from the current index does not remove old commits. Use a coordinated history rewrite with `git filter-repo` or BFG, then force-push and ask all collaborators to reclone.

3. Confirm the next deployment can write to `/etc/ontime`.
   The workflow now runs `sudo mkdir -p /etc/ontime/key`; the EC2 deploy user must have passwordless sudo for these file operations.

4. Keep these file permissions on every host that stores real secrets:
   ```bash
   sudo chown -R ubuntu:ubuntu /etc/ontime
   chmod 700 /etc/ontime /etc/ontime/key
   chmod 600 /etc/ontime/application.properties
   chmod 600 /etc/ontime/firebase-adminsdk.json
   chmod 600 /etc/ontime/key/AuthKey_743M7R5W3W.p8
   ```

5. Add a secret scanner to CI, for example Gitleaks, and block pushes/PRs that include private keys, service account JSON, `.env`, or populated `application.properties`.

## Operational pattern going forward

- Commit only templates with fake values.
- Keep real local values in `ontime-back/.env` or an untracked local `application.properties`.
- Keep CI values in GitHub Actions Secrets.
- Keep production files in `/etc/ontime` or a managed secret store.
- Treat any secret that has entered Git history, CI logs, PR comments, issue text, chat, or artifacts as exposed and rotate it.
