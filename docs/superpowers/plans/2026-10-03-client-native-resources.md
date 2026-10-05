# Client native zoom resources implementation plan

Scope approved by user after `client-resource-zoom-comparison.md`: add missing original resources, restore native x1 fonts, read zoom-specific resources in both tabs, retain mod assets and the item protocol.

1. Record hashes and back up modified source; copy only missing PNG/resource files from the nested reference project's Assets/Resources. Preserve existing bytes and Unity GUIDs; generate unique GUIDs for new assets. Validate PNG decode and glyph table compatibility.
2. Add a shared resource candidate selector. Ordinary assets: current zoom, resource root, x4 fallback. Assets whose existing x4 pixels differ from the reference, or whose paths are unique to the mod: prefer existing x4 with resizing, then native/root fallback. Test selection, including missing native assets and x4 duplicate handling.
3. Integrate selector into Game1 and Game2 GameCanvas.loadImage. Resize only x4 sources used at another zoom. Keep legacy explicit x4 loaders; ensure resizing clamps dimensions and restores/releases temporary render targets.
4. Initialize native fonts first, then overlay valid original bitmap fonts at x1. Load bitmap atlases from x1 and validate image/table presence. Keep orange font native because its reference atlas is missing. Retain all font aliases and large-number fields.
5. Compile both clients, test real Unity resources at zooms 1–4, validate bitmap glyph bounds and font availability, missing resource fallback, mod image priority, and item quantities/options below 100 million. Capture menu/login rendering and document limits. No server deployment required.

Run in the current session; no Git repository or worktree is available. Backups and an isolated Unity verification project provide rollback and runtime verification. Edit external client files through approved elevated tools.
