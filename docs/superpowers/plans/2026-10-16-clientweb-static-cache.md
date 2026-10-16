# ClientWeb Static Cache Implementation Plan

> **For agentic workers:** Execute each checkbox in order and retain the previous WebGL build as a rollback copy. This plan is being executed inline with review checkpoints.

**Goal:** Update the browser game so reopening it reuses the large WebGL data payload while new builds are still detected and downloaded correctly.

**Architecture:** Use Unity WebGL's built-in persistent data cache and long immutable HTTP caching for versioned assets; retain the existing template adapter and its validator behavior. Verify the real game reload and the adapter separately. Build into staging, preserve gateway configuration, then replace the gateway-served build with a timestamped backup.

**Tech Stack:** Unity 2022.3.62f2 WebGL, JavaScript IndexedDB, Node.js gateway, Playwright/browser DevTools.

---

### Task 1: Record current build and validate gateway behavior

**Files:**
- Read: `ClientWeb/PRJ_2Tab_Web/Assets/WebGLTemplates/NroWeb/data-cache.js`
- Read: `ClientWeb/PRJ_2Tab_Web/Assets/WebGLTemplates/NroWeb/index.html`
- Read: `ClientWeb/PRJ_2Tab_Web/Gateway/server.cjs`
- Test: `ClientWeb/PRJ_2Tab_Web/Gateway/server.test.cjs`
- Read: `ClientWeb/PRJ_2Tab_Web/Builds/WebGL/config.js`

- [x] Record current WebGL file names, sizes, SHA-256 values, active `config.js`, and gateway's served directory.
- [x] Run the existing Gateway Node test suite and record its full result (68 passed after updating stale loader-path expectations).
- [x] Confirm the versioned `.data` route responds with immutable cache headers, ETag, Last-Modified, and conditional 304; shell remains `no-cache`.
- [x] In Chromium, the adapter probe downloaded the full `.data` on first load and reopened from the same profile with HEAD only and no second GET.
- [x] In the game shell, the first request was GET; after cache persistence, reload generated no `.data` request, so it did not transfer the body again. The Unity generated loader used its built-in cache path; the template adapter hook was not invoked by the generated loader.
- [x] Changed validators are covered by the existing validator test; a new versioned URL is used for this build.

### Task 2: Build and stage new WebGL output

**Files:**
- Read: `ClientWeb/PRJ_2Tab_Web/Assets/Editor/WebClientBuild.cs`
- Read: `ClientWeb/PRJ_2Tab_Web/Builds/WebGL/config.js`
- Create: `ClientWeb/PRJ_2Tab_Web/Builds/.staging/WebGL-<timestamp>/`

- [x] Set `NRO_WEB_BUILD_OUTPUT` to unique staging directory `Builds/.staging/WebGL-cache-20261016` and run the existing Unity WebGL build entry point.
- [x] Confirm `Result=Succeeded`, 0 errors/0 warnings, and required shell, loader, framework, `.data`, and `.wasm` artifacts.
- [x] Compare staged `config.js` with the previous active config; hashes match.
- [x] Verify the shell includes the cache adapter and points to the matching versioned assets; Unity's generated loader selected its built-in persistent cache at runtime.

### Task 3: Back up and install gateway-served build

**Files:**
- Backup: `ClientWeb/PRJ_2Tab_Web/Builds/WebGL` to `ClientWeb/PRJ_2Tab_Web/Builds/backups/WebGL-<timestamp>/`
- Install: staged output to `ClientWeb/PRJ_2Tab_Web/Builds/WebGL`

- [x] Confirm the gateway serves `Builds/WebGL` before replacement.
- [x] Move the complete active output to `Builds/backups/WebGL-20261016-185356` and verify its hashes against the pre-install output.
- [x] Install staged files while preserving active connection configuration; PHP site and game server/JAR were not touched.
- [x] Open the updated shell in Chromium; verify asset loading, cache headers, and no `.data` body transfer on a persisted-cache reload. Headless Chrome reported unsupported `screen.orientation.lock()`; this is unrelated to cache loading.
- [x] Confirm rollback backup is intact and APK artifact did not overwrite an earlier release.

### Task 4: Report verified result

- [x] Record APK and WebGL output paths, hashes, test/build results, browser cache evidence, and the headless orientation limitation.
- [x] Make completion claims only for checks that have fresh successful output.

**Result:** New active WebGL payload is `Builds/WebGL/Build/WebGL-cache-20261016.data` (66,897,312 bytes). Prior build is preserved in `Builds/backups/WebGL-20261016-185356`. Gateway test suite: 68 passed, 0 failed.

---
