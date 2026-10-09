# Resource transfer repair

Approved scope: fix the diagnosed catalog packet overflow and both client rendering guards during saved maintenance. Server is stopped with data saved.

Design: keep the existing two catalog messages/cache keys, choose the split by serialized byte size at runtime, and validate both payloads against the 65535-byte frame limit before sending any catalog update. No network capability change or SQL/res replacement. Reject a catalog larger than two frames explicitly. Increment item version to force complete refresh.

1. Reproduce oversized tail with a captured real ItemData session; assert all template IDs arrive and each packet fits. Implement shared serialization and size-aware splitting; verify sparse IDs, short catalogs, oversized catalog rejection and original anime tests.
2. Both client namespaces: guard invalid icon IDs, allocate icon slots safely, handle unsolicited icon replies, avoid invalid atlas rows. Detect missing login template rather than consuming a partial body then continuing at the wrong stream position. Guard bag rendering and log template loading failures.
3. Test client guards with stub graphics/resources; compile both actual namespaces using project Unity assemblies. Preserve every client source before changing.
4. Build candidate by overlaying only changed Java classes. Run tests against candidate, verify unchanged resource hashes/SQL, back up active JAR with timestamp/SHA256 and metadata, install while stopped, restart hidden.
5. Check startup, port, live SQL mapping and catalog packet sizes; save deployment report. In-game reconnect/visual confirmation remains explicit if no live client session is available.
