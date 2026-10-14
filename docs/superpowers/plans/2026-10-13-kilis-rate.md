# Killis 30% / 60% Implementation Plan

> Execute sequentially in this session with test-driven-development and verification-before-completion.

**Goal:** Apply user-approved 30% normal / 60% active buff per damaging hit in map165.

**Architecture:** Keep KilisAbsorptionService attribution and validity checks. Change the probability denominator to100 and thresholds to30/60. Award remains +1; the existing doubled-speed buff description becomes accurate.

**Tech Stack:** Java17, standalone Java regression tests, existing saved-maintenance/JAR deployment tooling.

- [x] Add KilisRateTest: enumerate rolls0–99 and expect exactly30 normal,60 active buff,30 expired buff; test owner attribution through pet.
- [x] Compile the test against installed SRC/20.jar and observe failure for legacy333 denominator.
- [x] Modify only KilisAbsorptionService probability; adjust existing absorption test's boosted/expired roll to45.
- [x] Build a candidate from current20.jar replacing only KilisAbsorptionService; run new rate test and previous five regression groups against candidate.
- [x] Review scoped diff and metadata; deploy using prior user authorization with data-saving maintenance, stopped-process checks, database snapshot, timestamped JAR backup and SHA-256 metadata.
- [x] Restart hidden server and rerun tests on installed JAR; verify runtime map165 and use live class loader to check30/60 constants.
- [x] Record deployment evidence and report actual rates.
