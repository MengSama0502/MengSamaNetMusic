# License and Clean-Room Evidence

## Scope

This fixed source distribution records the license partition and the QQ Music
clean-room review requested for the Minecraft 1.20.1 Forge build.

## License partition

| Material | License / status | Included evidence |
| --- | --- | --- |
| NetMusic-derived and project Java code | BSD-3-Clause | `src/main/resources/LICENSE-CODE-BSD-3-CLAUSE.txt` |
| Project resources in `src/main/resources/assets` and `src/main/resources/data` | CC BY-NC-SA 4.0 | `src/main/resources/LICENSE-ASSETS-CC-BY-NC-SA-4.0.txt` |
| netMusicListForge-derived portions | MIT, Copyright (c) 2025 gly091020 | `src/main/resources/LICENSE-NETMUSICLISTFORGE-MIT.txt` |
| NetMusicCanNeedQQ | All Rights Reserved comparison reference only | `src/main/resources/THIRD-PARTY-NOTICES.txt` |

`README.md` and `src/main/resources/META-INF/mods.toml` identify the split
license model. The final JAR contains the three license texts and
`THIRD-PARTY-NOTICES.txt` at its root.

## NetMusicCanNeedQQ comparison and disposition

Reference reviewed: `NetMusicCanNeedQQ-main` under an All Rights Reserved
status. Its source is not included in this project.

| Current file | Closest reference file | Final normalized line-sequence ratio | Treatment |
| --- | --- | ---: | --- |
| `api/QqCredential.java` | `qq/QqCredential.java` | 0.3133 | Independently reworked: normalized storage, explicit expiry-at-time check, expiry-aware validity, UTC clock use. Remaining equal items are provider JSON field names and constructor signature. |
| `api/ParsedUrl.java` | `data/ParsedUrl.java` | 0.2000 | Reworked: renamed internal state, null/blank invariant, `Objects` validation and diagnostic `toString`. |
| `api/QqSearchResult.java` | `qq/QqSearchResult.java` | 0.1875 | Reworked: normalized adapter-row model, renamed state, strip normalization and separator-safe display behavior. |
| `gui/QqLoginScreen.java` | `client/QqSearchScreen.java` | 0.1277 | No material algorithmic match: both are Minecraft screens. Shared lines are framework lifecycle calls. |
| `api/QqMusicApi.java` | `qq/QqMusicApi.java` | 0.0900 | Reworked provider facade; residual async construct is generic Java API use. |
| `api/QqUrlParser.java` | `qq/QqUrlParser.java` | 0.0833 | URI/query based parser replaced regex/string-flow implementation; validates HTTP(S) QQ hosts and isolates redirect resolution. |
| `api/QqCredentialManager.java` | `qq/QqCredentialManager.java` | 0.0762 | Reworked with synchronized publication and atomic file replacement. |
| `api/QqLoginService.java` | `qq/QqLoginService.java` | 0.0434 | Reworked around `java.net.http.HttpClient`, composable futures, shared helpers and response decoding. QQ endpoint names, required request fields, callback grammar and DJB-style token math are protocol facts retained for interoperability. |
| `api/QqMusicUtils.java` | `qq/QqCredentialManager.java` | 0.0214 | No meaningful implementation overlap found. |

The comparison normalizes whitespace/comments/package/import declarations and
compares each current QQ/data file against every Java file in the reference.
Exact long-line checks after normalization found no matching implementation
lines in `ParsedUrl` and `QqMusicUtils`; protocol annotations/field names remain
where required by the external QQ response format.

Nonessential ARR-specific classes and symbols were searched for and are absent:
`QqDiscNbt`, `QqMusicUpdater`, `PhoneSongList`, `QqSearchState`,
`QqSearchTarget`, `MarkQqDiscMessage`, `ClearQqDiscMessage`, and the
`yincmewy.netmusiccanneedqq` package namespace.

## Easter egg removal

The two rainbow credit tooltips were removed from:

- `src/main/java/com/mengsama/mod/mengsamanetmusic/item/MusicPlayerItem.java`
- `src/main/java/com/mengsama/mod/mengsamanetmusic/block/MusicPlayerBlock.java`

Source checks confirm the removed strings and `rainbowColor` helper are absent.

## Verification

Commands executed:

```text
.\gradlew.bat clean test build --no-daemon
.\gradlew.bat test --rerun-tasks --no-daemon
.\gradlew.bat copyReleaseJar --no-daemon
```

Result: all three commands returned `BUILD SUCCESSFUL`. The second command forced compilation and test execution without relying on Gradle's test cache.

- 39 test suites
- 150 tests total
- 146 passed
- 4 skipped because provider live-chain tests require the opt-in live-test setting
- 0 failures and 0 errors

New coverage:

- `api/QqCleanRoomBehaviorTest.java`: public URL classification, redirect host handling, invalid URI rejection, credential expiry/cookies, result normalization and login response helpers.
- `mixin/PackagedLicenseValidationTest.java`: final JAR license/notice entries, expanded `mods.toml` license metadata and removed-easter-egg source assertions.

Final JAR:

```text
MengSamaNetMusic-1.20.1-forge-fixed.jar
SHA-256: BE803FBCBA27A96E41ABD8E89802499228438CE1A63FD89A42E0509C7249BA38
Size: 3,935,264 bytes
```

ZIP API verification confirmed these root entries: `LICENSE-CODE-BSD-3-CLAUSE.txt`, `LICENSE-ASSETS-CC-BY-NC-SA-4.0.txt`, `LICENSE-NETMUSICLISTFORGE-MIT.txt`, `THIRD-PARTY-NOTICES.txt`, and `META-INF/mods.toml`. The packaged metadata contains the BSD/CC license partition and does not contain `license = "MIT"`.
