# Android Client APK Implementation Plan

> **For agentic workers:** Execute each checkbox in order and preserve existing release artifacts. This plan is being executed inline with review checkpoints.

**Goal:** Build a new Android APK from the approved native client project and leave existing builds untouched.

**Architecture:** Use the project's existing `ClientDistributionBuild.Android` release entry point and a unique timestamped output folder. Verify the artifact's package, architectures, signature mode, file size, and SHA-256, then write build metadata beside it.

**Tech Stack:** Unity 2022.3.62f2, Android IL2CPP, ARMv7 + ARM64, PowerShell, SHA-256.

---

### Task 1: Capture release inputs

**Files:**
- Read: `C:/Users/Admin/Downloads/PRJ_2Tab_550K/Assets/Editor/ClientDistributionBuild.cs`
- Read: `C:/Users/Admin/Downloads/PRJ_2Tab_550K/ProjectSettings/ProjectSettings.asset`
- Output: a new folder under `C:/Users/Admin/Downloads/PRJ_2Tab_550K/Builds/Distribution/`

- [x] Record scoped source status, active scenes, bundle/package identifier, version, architecture, scripting backend, and signing configuration.
- [x] Create a timestamped output folder that does not already exist; set `NRO_DISTRIBUTION_OUTPUT` to its absolute path.
- [x] Confirm `ClientDistributionBuild.Android` refuses overwrite and does not write into any existing release folder.

### Task 2: Build and verify APK

**Files:**
- Create: `<timestamped output>/Android/NRO.apk`
- Create: `<timestamped output>/Android/build-metadata.txt`

- [x] Invoke the existing Unity distribution build entry point with Android target and the reviewed output path.
- [x] Read the Unity build log and confirm successful exit and the expected APK path.
- [x] Verify APK package/version, ARMv7 + ARM64 native libraries, SDK levels, and signing mode. APK signature verified with `apksigner`; APK size is 84,263,251 bytes.
- [x] Write source revision/status, Unity version, package/version, build time, file size, and SHA-256 to metadata.
- [x] Compute APK SHA-256 again after metadata was written and confirm the recorded checksum matches.

### Task 3: Preserve and report artifacts

- [x] Confirm no previous APK was overwritten and the new APK is in the timestamped output folder.
- [x] Record exact APK path, byte size, SHA-256, and signing limitation (Unity development keystore) in the completion report.

**Result:** `Builds/Distribution/20261016-185454-ClientAPK/Android/NRO.apk`. SHA-256: `708CCC77C5FD33A0DB7F1CC853358A4490C0E5249DD13AA8C6C00D450705E612`. Signer matches the previous APK. Unity reported 84 warnings, 0 errors. Full repository status could not traverse Git LFS `Library/ArtifactDB`; the scoped Editor/ProjectSettings status was clean.

---
