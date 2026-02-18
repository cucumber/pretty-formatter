# Changelog

All notable changes to this project will be documented in this file.

The formatter is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/)
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [3.1.0] - 2026-02-18
### Added
- [JavaScript] Support custom code formatting ([#92](https://github.com/cucumber/pretty-formatter/pull/92))

### Fixed
- Avoid stack trace with pending and skipped ([#91](https://github.com/cucumber/pretty-formatter/pull/91))

## [3.0.0] - 2026-01-31
### Added
- [JavaScript] Add `SummaryPrinter` ([#40](https://github.com/cucumber/pretty-formatter/pull/40))
- [JavaScript] Add `ProgressPrinter` ([#40](https://github.com/cucumber/pretty-formatter/pull/40))
- Print execution duration in summary ([#62](https://github.com/cucumber/pretty-formatter/pull/62))
- Include responsible step in summary ([#63](https://github.com/cucumber/pretty-formatter/pull/63))
- Show matching step definitions for ambiguous steps ([#67](https://github.com/cucumber/pretty-formatter/pull/67))
- Add progress bar printer ([#79](https://github.com/cucumber/pretty-formatter/pull/79))

### Changed
- [JavaScript] BREAKING CHANGE: Expose `PrettyPrinter` rather than a high-level formatter ([#40](https://github.com/cucumber/pretty-formatter/pull/40))
- [JavaScript] Publish package in CommonJS format ([#40](https://github.com/cucumber/pretty-formatter/pull/40))
- Overhaul status colours ([#71](https://github.com/cucumber/pretty-formatter/pull/71), [#72](https://github.com/cucumber/pretty-formatter/pull/72))

### Fixed
- [Java] Correct attempt count for retries ([#40](https://github.com/cucumber/pretty-formatter/pull/40))
- [Java] Unwrap test run exception from `Optional` when printing ([#40](https://github.com/cucumber/pretty-formatter/pull/40))

## [2.4.1] - 2025-11-02
### Fixed
- [Java] Unwrap optional exceptions in `TestRunFinished`

## [2.4.0] - 2025-10-27
### Changed
- Update dependency io.cucumber:messages up to v30

## [2.3.0] - 2025-09-19
### Added
- [Java] Message based summary printer ([#35](https://github.com/cucumber/pretty-formatter/pull/35))
- [Java] Message based progress printer ([#37](https://github.com/cucumber/pretty-formatter/pull/37))

## [2.2.0] - 2025-09-11
### Changed
- Update dependency @cucumber/query to v14
- Update dependency @cucumber/messages to v29
- Update dependency io.cucumber:query to v14
- Update dependency io.cucumber:messages to v29

### Fixed
- [Java] Fix step match arguments highlighting for ambiguous steps ([#28](https://github.com/cucumber/pretty-formatter/pull/28))
- [JavaScript] Fix repeated step arguments ([#21](https://github.com/cucumber/pretty-formatter/pull/21))

## [2.1.0] - 2025-08-17
### Added
- Add attachments option ([#19](https://github.com/cucumber/pretty-formatter/pull/19))

### Fixed
- [JavaScript] Print stacktrace with message and type ([#14](https://github.com/cucumber/pretty-formatter/pull/14))

## [2.0.1] - 2025-07-19
### Fixed
- Remove test only default style ([#7](https://github.com/cucumber/pretty-formatter/pull/7))

## [2.0.0] - 2025-07-18
### Changed
- Added JavaScript implementation, replacing [cucumber-js-pretty-formatter](https://github.com/cucumber/cucumber-js-pretty-formatter) ([#4](https://github.com/cucumber/pretty-formatter/pull/4))

## [0.3.0] - 2025-07-10
### Added
- Add status icons and plain text theme ([#3](https://github.com/cucumber/pretty-formatter/pull/3))

### Changed
- Update dependency io.cucumber:query to v13.5.0

## [0.2.0] - 2025-07-07
### Changed
- Update dependency io.cucumber:messages to v28

## [0.1.0] - 2025-07-07
### Added
- Java implementation ([#1](https://github.com/cucumber/pretty-formatter/pull/1) M.P. Korstanje)

[Unreleased]: https://github.com/cucumber/pretty-formatter/compare/v3.1.0...HEAD
[3.1.0]: https://github.com/cucumber/pretty-formatter/compare/v3.0.0...v3.1.0
[3.0.0]: https://github.com/cucumber/pretty-formatter/compare/v2.4.1...v3.0.0
[2.4.1]: https://github.com/cucumber/pretty-formatter/compare/v2.4.0...v2.4.1
[2.4.0]: https://github.com/cucumber/pretty-formatter/compare/v2.3.0...v2.4.0
[2.3.0]: https://github.com/cucumber/pretty-formatter/compare/v2.2.0...v2.3.0
[2.2.0]: https://github.com/cucumber/pretty-formatter/compare/v2.1.0...v2.2.0
[2.1.0]: https://github.com/cucumber/pretty-formatter/compare/v2.0.1...v2.1.0
[2.0.1]: https://github.com/cucumber/pretty-formatter/compare/v2.0.0...v2.0.1
[2.0.0]: https://github.com/cucumber/pretty-formatter/compare/v0.3.0...v2.0.0
[0.3.0]: https://github.com/cucumber/pretty-formatter/compare/v0.2.0...v0.3.0
[0.2.0]: https://github.com/cucumber/pretty-formatter/compare/v0.1.0...v0.2.0
[0.1.0]: https://github.com/cucumber/pretty-formatter/compare/f17778f0f8b098be22522327f081a698ed561aa0...v0.1.0
