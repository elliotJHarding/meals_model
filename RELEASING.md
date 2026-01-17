# Release Process

## Automated Release via GitHub Actions

### Current Version
The current development version is always `X.Y.Z-SNAPSHOT` in `build.gradle.kts`.

### Steps to Release

1. **Go to GitHub Actions**
   - Navigate to https://github.com/elliotJHarding/meals_model/actions
   - Click "Release and Publish Packages" workflow
   - Click "Run workflow" button

2. **Select bump type**
   - **patch**: `1.0.0` → `1.0.1` (bug fixes)
   - **minor**: `1.0.0` → `1.1.0` (new features, default)
   - **major**: `1.0.0` → `2.0.0` (breaking changes)

3. **Click "Run workflow"**

   The workflow will automatically:
   - Strip `-SNAPSHOT` from current version (e.g., `1.0.0-SNAPSHOT` → `1.0.0`)
   - Build and publish packages with release version
   - Create git tag `v1.0.0`
   - Bump to next SNAPSHOT version (e.g., `1.1.0-SNAPSHOT`)
   - Commit and push changes back to main

4. **Monitor the workflow**
   - Wait for workflow to complete (~2-5 minutes)
   - Check for green checkmark

5. **Update consuming projects**

   **meals_server** (build.gradle):
   ```gradle
   implementation 'com.harding.meals:meals-contract:1.0.0'
   ```

   **meals_web_client** (package.json):
   ```json
   "@harding/meals-api": "1.0.0"
   ```

## What Happens Automatically

1. **Version `1.0.0-SNAPSHOT`** → Workflow triggered
2. **Commits**: "Release version 1.0.0"
3. **Publishes**: `com.harding.meals:meals-contract:1.0.0` and `@harding/meals-api@1.0.0`
4. **Tags**: `v1.0.0`
5. **Commits**: "Bump version to 1.1.0-SNAPSHOT"
6. **Result**: Main branch now at `1.1.0-SNAPSHOT`, ready for next development cycle

## Manual Version Override

If you need a specific version (e.g., for a hotfix):

1. Manually update `build.gradle.kts`:
   ```kotlin
   version = "1.0.1-SNAPSHOT"  // Skip to specific version
   ```

2. Commit and push:
   ```bash
   git commit -am "Prepare for hotfix 1.0.1"
   git push origin main
   ```

3. Run the release workflow (will release `1.0.1`)

## Rollback

GitHub Packages doesn't support unpublishing. To rollback:

1. **Delete the tag** (won't remove published packages):
   ```bash
   git tag -d v1.0.0
   git push origin :refs/tags/v1.0.0
   ```

2. **Release a new fixed version** (recommended):
   - Fix the issue
   - Run release workflow again with patch bump

## Local Development Setup

### For Java (meals_server)

Create `~/.gradle/gradle.properties`:
```properties
gpr.user=YOUR_GITHUB_USERNAME
gpr.key=YOUR_GITHUB_PERSONAL_ACCESS_TOKEN
```

### For TypeScript (meals_web_client)

Create `~/.npmrc`:
```
//npm.pkg.github.com/:_authToken=YOUR_GITHUB_PERSONAL_ACCESS_TOKEN
```

### Creating Personal Access Token

1. Go to GitHub Settings → Developer settings → Personal access tokens → Tokens (classic)
2. Click "Generate new token (classic)"
3. Give it a descriptive name (e.g., "meals packages")
4. Select scopes: `read:packages` and `write:packages`
5. Click "Generate token"
6. Copy the token and save it in your `~/.gradle/gradle.properties` and `~/.npmrc` files
