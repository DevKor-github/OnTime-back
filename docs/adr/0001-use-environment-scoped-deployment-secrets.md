# Use Shared Secret Names With Environment Overrides

Production and development deployment workflows use the same secret names and rely on the selected GitHub Environment to override only values that differ. This avoids duplicating environment identity in names like `DEV_REMOTE_HOST` and `EC2_HOST`, keeps genuinely shared values in one repository secret, and prevents future deploy fixes from updating one environment naming scheme but missing the other.

Environment-specific values belong in `development` or `production` Environment secrets. Values shared by every environment should be workflow constants, repository variables, repository secrets, or the built-in `GITHUB_TOKEN` depending on whether they are sensitive. Deploy workflows include environment sentinel and datasource checks because GitHub falls back from missing Environment secrets to repository secrets.
