# TON Plugin for the IntelliJ IDEs Changelog

## [4.1.0]

v4.1.0 is a smaller follow-up to the Acton-focused v4.0.0 release. It improves navigation and editor polish around
Acton-backed Tolk projects, fixes completion in TypeScript app scaffolds, and keeps the plugin compatible with newer
JetBrains platform APIs.

### Tolk

- feat(tolk): add navigation from a Tolk contract header name to the matching `[contracts.<id>]` entry in Acton.toml in https://github.com/ton-blockchain/intellij-ton/pull/797
- fix(tolk): don't add a comma when completing the last field in a contract header in https://github.com/ton-blockchain/intellij-ton/pull/794

### Acton

- feat(acton): detect when the TOML plugin is disabled and show an Acton.toml notification instead of silently disabling Acton.toml support in https://github.com/ton-blockchain/intellij-ton/pull/795
- fix(acton): keep `.acton` helper symbols available in TypeScript app scaffold scripts and tests while still hiding them from contract-source completion in https://github.com/ton-blockchain/intellij-ton/pull/796

### Platform

- chore(all): migrate away from internal JetBrains APIs for newer platform compatibility in https://github.com/ton-blockchain/intellij-ton/pull/793
- fix(tolk): avoid synthetic presentation class loading failures during navigation and hover documentation

## [4.0.0]

v4.0.0 collects the work since v3.0.0 and makes Acton the main TON development workflow in the IDE.

The headline change is that Acton is no longer just an external command you run next to the editor. It is now integrated
through the whole project lifecycle: you can create TON projects from the current Acton templates, configure Acton.toml
with completion and navigation, run builds, tests, scripts, linting and formatting from the IDE, generate wrappers,
debug scripts and tests, inspect wallets and follow coverage, and deploy through Acton scripts without
leaving the JetBrains workflow.

This release also brings the Tolk support line forward from 1.2 through 1.4, adds smarter completion and hints around
contracts, enums, arrays, annotations and wrappers, and keeps the rest of the TON tooling in step: BoC disassembly is
Acton-powered, TL-B resolving is better, Blueprint support is removed, and the plugin targets the modern 2025.2+
JetBrains platform.

### Acton

#### Project setup and templates

- feat(acton): add the first Acton integration with Acton.toml actions and run configurations in https://github.com/ton-blockchain/intellij-ton/pull/598 and https://github.com/ton-blockchain/intellij-ton/pull/758
- feat(acton): add the Acton project wizard in https://github.com/ton-blockchain/intellij-ton/pull/608
- feat(acton): create `Run all tests` and `Build all contracts` run configurations for new projects in https://github.com/ton-blockchain/intellij-ton/pull/621
- feat(acton): support all project wizard options, including TypeScript app generation, in https://github.com/ton-blockchain/intellij-ton/pull/706 and https://github.com/ton-blockchain/intellij-ton/pull/697
- feat(acton): load project wizard templates from Acton itself in https://github.com/ton-blockchain/intellij-ton/pull/749; keep fallback templates aligned with current Acton templates, including `empty`, `counter`, `jetton`, `nft`, `w5-extension` and TypeScript app scaffolds
- feat(acton): add an action to initialize a TypeScript dApp from Acton.toml in https://github.com/ton-blockchain/intellij-ton/pull/782
- feat(acton): add an Acton auto-installer and startup version checks in https://github.com/ton-blockchain/intellij-ton/pull/772 and https://github.com/ton-blockchain/intellij-ton/pull/624
- feat(acton): allow skipping `acton up` version suggestions at application level in https://github.com/ton-blockchain/intellij-ton/pull/695
- chore(new): prevent project creation when `acton` is not available in https://github.com/ton-blockchain/intellij-ton/pull/759

#### Acton.toml, mappings and monorepos

- feat(acton): add Acton.toml name completion and resolving in https://github.com/ton-blockchain/intellij-ton/pull/614 and https://github.com/ton-blockchain/intellij-ton/pull/615
- feat(acton): support mappings for Tolk and correctly handle mapped imports on file moves in https://github.com/ton-blockchain/intellij-ton/pull/669 and https://github.com/ton-blockchain/intellij-ton/pull/694
- feat(acton): update Acton.toml schema and IDE support to the latest Acton versions in https://github.com/ton-blockchain/intellij-ton/pull/688, https://github.com/ton-blockchain/intellij-ton/pull/696, https://github.com/ton-blockchain/intellij-ton/pull/704, https://github.com/ton-blockchain/intellij-ton/pull/707, https://github.com/ton-blockchain/intellij-ton/pull/777
- feat(acton): add completion, resolving and language injection for Acton.toml script and config values in https://github.com/ton-blockchain/intellij-ton/pull/713, https://github.com/ton-blockchain/intellij-ton/pull/714, https://github.com/ton-blockchain/intellij-ton/pull/715
- feat(acton): improve Acton.toml and Tolk stdlib discovery in monorepositories in https://github.com/ton-blockchain/intellij-ton/pull/775 and https://github.com/ton-blockchain/intellij-ton/pull/776
- feat(acton): add actions to register a contract and create a new registered contract from the IDE in https://github.com/ton-blockchain/intellij-ton/pull/630 and https://github.com/ton-blockchain/intellij-ton/pull/631
- fix(acton): improve missing-Acton handling, project creation stability, EDT-safe template loading and directory test line markers in https://github.com/ton-blockchain/intellij-ton/pull/672, https://github.com/ton-blockchain/intellij-ton/pull/681, https://github.com/ton-blockchain/intellij-ton/pull/754, https://github.com/ton-blockchain/intellij-ton/pull/735

#### Build, wrappers, scripts and deployment

- feat(acton): integrate Acton test runner, coverage and script runner in https://github.com/ton-blockchain/intellij-ton/pull/599
- feat(acton): add build, test, fmt, lint and contract-header line markers, including UI test runs from Acton.toml, in https://github.com/ton-blockchain/intellij-ton/pull/610, https://github.com/ton-blockchain/intellij-ton/pull/716, https://github.com/ton-blockchain/intellij-ton/pull/717, https://github.com/ton-blockchain/intellij-ton/pull/725
- feat(acton): support `acton fmt`, range-based formatting and external `acton check` linters in https://github.com/ton-blockchain/intellij-ton/pull/693, https://github.com/ton-blockchain/intellij-ton/pull/781, https://github.com/ton-blockchain/intellij-ton/pull/670
- feat(acton): add linter suppression and issue tag support for `acton check` in https://github.com/ton-blockchain/intellij-ton/pull/708, https://github.com/ton-blockchain/intellij-ton/pull/722, https://github.com/ton-blockchain/intellij-ton/pull/738
- feat(acton): support script broadcasting with `--net` and default to tonscan for explorer links in https://github.com/ton-blockchain/intellij-ton/pull/710 and https://github.com/ton-blockchain/intellij-ton/pull/740
- feat(acton): run Acton commands in a terminal-like console with PTY and prompt support in https://github.com/ton-blockchain/intellij-ton/pull/720 and https://github.com/ton-blockchain/intellij-ton/pull/721
- feat(acton): add console filters for Acton test assertions and Tolk file paths in https://github.com/ton-blockchain/intellij-ton/pull/605 and https://github.com/ton-blockchain/intellij-ton/pull/771
- feat(acton): add gutter actions to regenerate Tolk or TypeScript wrappers and Tolk actions to generate wrappers from contract headers in https://github.com/ton-blockchain/intellij-ton/pull/779 and https://github.com/ton-blockchain/intellij-ton/pull/768

#### Debugging, tests, coverage and profiling

- feat(acton): support debugging scripts, tests and retrace commands, and wait for process termination after debug finishes, in https://github.com/ton-blockchain/intellij-ton/pull/712 and https://github.com/ton-blockchain/intellij-ton/pull/745
- feat(acton): show debug values for variables and fields on hover in https://github.com/ton-blockchain/intellij-ton/pull/756
- feat(acton): preserve failed-test underlines, show actual/expected values and suggest debug or backtrace reruns from test failure inspections in https://github.com/ton-blockchain/intellij-ton/pull/724, https://github.com/ton-blockchain/intellij-ton/pull/736, https://github.com/ton-blockchain/intellij-ton/pull/746, https://github.com/ton-blockchain/intellij-ton/pull/747
- feat(acton): add rerun failed tests and rerun selected tests actions in https://github.com/ton-blockchain/intellij-ton/pull/741 and https://github.com/ton-blockchain/intellij-ton/pull/770
- feat(acton): add initial profiling support and readable coverage-unavailable errors in https://github.com/ton-blockchain/intellij-ton/pull/705 and https://github.com/ton-blockchain/intellij-ton/pull/719
- feat(coverage): include branch hit tracking and coverage line data reports in https://github.com/ton-blockchain/intellij-ton/pull/703 and https://github.com/ton-blockchain/intellij-ton/pull/748

#### Wallets and project UX

- feat(acton): add a wallet tool window with wallet cards, balances, unconfigured-wallet hints, new-wallet actions, testnet airdrops and wallet-address fixes in https://github.com/ton-blockchain/intellij-ton/pull/633, https://github.com/ton-blockchain/intellij-ton/pull/634, https://github.com/ton-blockchain/intellij-ton/pull/635, https://github.com/ton-blockchain/intellij-ton/pull/636, https://github.com/ton-blockchain/intellij-ton/pull/637, https://github.com/ton-blockchain/intellij-ton/pull/680, https://github.com/ton-blockchain/intellij-ton/pull/690, https://github.com/ton-blockchain/intellij-ton/pull/691
- feat(acton): add a default explorer setting and use folder icons for `tests/`, `contracts/` and `src/` in https://github.com/ton-blockchain/intellij-ton/pull/692 and https://github.com/ton-blockchain/intellij-ton/pull/739
- feat(acton): support new Acton stdlib functions and hide Acton-only symbols from contract completions in https://github.com/ton-blockchain/intellij-ton/pull/755 and https://github.com/ton-blockchain/intellij-ton/pull/727

### Tolk

#### Language versions

- feat(tolk/1.2): support lambdas, new low-level functions and `any_address` in https://github.com/ton-blockchain/intellij-ton/pull/586, https://github.com/ton-blockchain/intellij-ton/pull/587, https://github.com/ton-blockchain/intellij-ton/pull/588
- feat(tolk/1.3): support strings, arrays, `??`, contract definitions, `@test`, `@abi`, contract stubs and validation in https://github.com/ton-blockchain/intellij-ton/pull/650, https://github.com/ton-blockchain/intellij-ton/pull/652, https://github.com/ton-blockchain/intellij-ton/pull/654, https://github.com/ton-blockchain/intellij-ton/pull/657, https://github.com/ton-blockchain/intellij-ton/pull/659, https://github.com/ton-blockchain/intellij-ton/pull/665
- feat(tolk/1.4): support void type parameters, lambda completion and highlighting for captured variables in https://github.com/ton-blockchain/intellij-ton/pull/709, https://github.com/ton-blockchain/intellij-ton/pull/731, https://github.com/ton-blockchain/intellij-ton/pull/732
- feat(tolk): support dotted annotations, field annotations, new test annotations and richer `@abi.X` / `@abi.clientType<T>` completion in https://github.com/ton-blockchain/intellij-ton/pull/743 and https://github.com/ton-blockchain/intellij-ton/pull/766

#### Completion, inference and editor support

- feat(tolk): add better generic completion, signature help, uppercase type-name completion and array indexing inference in https://github.com/ton-blockchain/intellij-ton/pull/622, https://github.com/ton-blockchain/intellij-ton/pull/644, https://github.com/ton-blockchain/intellij-ton/pull/647, https://github.com/ton-blockchain/intellij-ton/pull/661, https://github.com/ton-blockchain/intellij-ton/pull/674
- feat(tolk): improve default values for `Cell<T>`, maps, arrays and struct fields in completion and initialization in https://github.com/ton-blockchain/intellij-ton/pull/606, https://github.com/ton-blockchain/intellij-ton/pull/682, https://github.com/ton-blockchain/intellij-ton/pull/683, https://github.com/ton-blockchain/intellij-ton/pull/733
- feat(tolk): add pack-prefix generation, rename it for clarity, test get-method completion, postfix `println`, contract-header build markers and wrapper generation actions in https://github.com/ton-blockchain/intellij-ton/pull/603, https://github.com/ton-blockchain/intellij-ton/pull/613, https://github.com/ton-blockchain/intellij-ton/pull/623, https://github.com/ton-blockchain/intellij-ton/pull/717, https://github.com/ton-blockchain/intellij-ton/pull/768, https://github.com/ton-blockchain/intellij-ton/pull/773
- feat(tolk): improve hover documentation for default values, computed values, debugger values and contract header fields in https://github.com/ton-blockchain/intellij-ton/pull/663, https://github.com/ton-blockchain/intellij-ton/pull/769, https://github.com/ton-blockchain/intellij-ton/pull/783
- feat(tolk): add inlay hints for computed enum member values and reduce noisy builtin parameter hints in https://github.com/ton-blockchain/intellij-ton/pull/648, https://github.com/ton-blockchain/intellij-ton/pull/711, https://github.com/ton-blockchain/intellij-ton/pull/765, https://github.com/ton-blockchain/intellij-ton/pull/785
- feat(tolk): add the new disassemble view and Acton-powered disassembly actions in https://github.com/ton-blockchain/intellij-ton/pull/611 and https://github.com/ton-blockchain/intellij-ton/pull/767
- feat(tolk): add statement/top-level movers, better Windows path handling and shortest import path selection with mappings in https://github.com/ton-blockchain/intellij-ton/pull/737, https://github.com/ton-blockchain/intellij-ton/pull/780, https://github.com/ton-blockchain/intellij-ton/pull/786
- feat(tolk): simplify toolchain setup, auto-detect Acton's Tolk stdlib and highlight `array` as a builtin type in https://github.com/ton-blockchain/intellij-ton/pull/607 and https://github.com/ton-blockchain/intellij-ton/pull/642

#### Fixes

- fix(tolk): fix lambda parsing, struct initialization with `void`, tensor type parsing, nested stub access, test location parsing and alias field completion in https://github.com/ton-blockchain/intellij-ton/pull/590, https://github.com/ton-blockchain/intellij-ton/pull/662, https://github.com/ton-blockchain/intellij-ton/pull/757, https://github.com/ton-blockchain/intellij-ton/pull/778, https://github.com/ton-blockchain/intellij-ton/pull/784
- fix(tolk): don't inspect Acton stdlib, fix stdlib test paths, don't auto-import `common.tolk`, don't add test functions to completion and don't consider `_x` parameters unused in https://github.com/ton-blockchain/intellij-ton/pull/609, https://github.com/ton-blockchain/intellij-ton/pull/612, https://github.com/ton-blockchain/intellij-ton/pull/619, https://github.com/ton-blockchain/intellij-ton/pull/627, https://github.com/ton-blockchain/intellij-ton/pull/730
- fix(tolk): remove odd grammar keywords and avoid deprecated `createEmptyMap()` in generated defaults in https://github.com/ton-blockchain/intellij-ton/pull/723 and https://github.com/ton-blockchain/intellij-ton/pull/774

### TL-B, FunC, BoC and Platform

- feat(tlb): improve TL-B resolving in https://github.com/ton-blockchain/intellij-ton/pull/787
- fix(func): restore FunC test launch and move FunC tests into the FunC module in https://github.com/ton-blockchain/intellij-ton/pull/626 and https://github.com/ton-blockchain/intellij-ton/pull/628
- feat(boc): use Acton for BoC disassembly in https://github.com/ton-blockchain/intellij-ton/pull/618
- feat(all): highlight TON addresses as terminal links in https://github.com/ton-blockchain/intellij-ton/pull/632
- feat(all): remove Blueprint support and make Acton the supported project workflow in https://github.com/ton-blockchain/intellij-ton/pull/718
- feat(all): support JetBrains 2025.2, bump the minimum platform, use RustRover as the development base platform and refresh build tooling in https://github.com/ton-blockchain/intellij-ton/pull/579, https://github.com/ton-blockchain/intellij-ton/pull/582, https://github.com/ton-blockchain/intellij-ton/pull/592, https://github.com/ton-blockchain/intellij-ton/pull/593, https://github.com/ton-blockchain/intellij-ton/pull/594, https://github.com/ton-blockchain/intellij-ton/pull/689, https://github.com/ton-blockchain/intellij-ton/pull/751, https://github.com/ton-blockchain/intellij-ton/pull/752
- chore(all): switch to the TON icon, delete unused files, replace old organization links and remove unused links in https://github.com/ton-blockchain/intellij-ton/pull/742, https://github.com/ton-blockchain/intellij-ton/pull/604, https://github.com/ton-blockchain/intellij-ton/pull/595, https://github.com/ton-blockchain/intellij-ton/pull/702
- fix(all): fix template configuration, language-registration exceptions, 2025.3 editor API compatibility, `.acton` invalidation, broad Acton regressions and test stability in https://github.com/ton-blockchain/intellij-ton/pull/601, https://github.com/ton-blockchain/intellij-ton/pull/620, https://github.com/ton-blockchain/intellij-ton/pull/625, https://github.com/ton-blockchain/intellij-ton/pull/629, https://github.com/ton-blockchain/intellij-ton/pull/666, https://github.com/ton-blockchain/intellij-ton/pull/726, https://github.com/ton-blockchain/intellij-ton/pull/734, https://github.com/ton-blockchain/intellij-ton/pull/750
- chore(ci): stabilize CI, fix disk-space failures, add Zizmor checks and fix project-wide compiler, formatting and ktlint issues in https://github.com/ton-blockchain/intellij-ton/pull/600, https://github.com/ton-blockchain/intellij-ton/pull/701, https://github.com/ton-blockchain/intellij-ton/pull/760, https://github.com/ton-blockchain/intellij-ton/pull/753, https://github.com/ton-blockchain/intellij-ton/pull/728, https://github.com/ton-blockchain/intellij-ton/pull/729

## [3.2.0]

v3.2.0 focuses on Tolk 1.3 support, editor improvements and compatibility updates for newer JetBrains platforms.

### Tolk

- feat(tolk): add a default value for array fields in https://github.com/ton-blockchain/intellij-ton/pull/683
- feat(tolk): add an intention to generate a pack prefix for structs in https://github.com/ton-blockchain/intellij-ton/pull/603
- feat(tolk): don't auto-import `common.tolk` from the standard library in https://github.com/ton-blockchain/intellij-ton/pull/612
- feat(tolk): don't show the inlay hint for `address()` in https://github.com/ton-blockchain/intellij-ton/pull/648
- feat(tolk/1.3): add contract definition stubs and validity checks in https://github.com/ton-blockchain/intellij-ton/pull/665
- feat(tolk/1.3): add type inference for array indexing in https://github.com/ton-blockchain/intellij-ton/pull/674
- feat(tolk/1.3): support `??` operator in https://github.com/ton-blockchain/intellij-ton/pull/654
- feat(tolk/1.3): support `@test` and `@abi` annotations in https://github.com/ton-blockchain/intellij-ton/pull/652
- feat(tolk/1.3): support contract definitions from Tolk 1.3 in https://github.com/ton-blockchain/intellij-ton/pull/657
- feat(tolk/1.3): support string type from Tolk 1.3 in https://github.com/ton-blockchain/intellij-ton/pull/650
- feat(tolk/1.3): support the new array type from Tolk 1.3 in https://github.com/ton-blockchain/intellij-ton/pull/659
- feat(tolk/completion): add `<>` on generic struct and type alias completion in https://github.com/ton-blockchain/intellij-ton/pull/644
- feat(tolk/completion): add signature help for generic parameters in https://github.com/ton-blockchain/intellij-ton/pull/647
- feat(tolk/completion): add type completion for function names that start with an uppercase letter in https://github.com/ton-blockchain/intellij-ton/pull/661
- feat(tolk/completion): complete `<>` in generics and auto-delete `>` on `<` delete in https://github.com/ton-blockchain/intellij-ton/pull/622
- feat(tolk/completion): improve the default value for `Cell<T>` in completion in https://github.com/ton-blockchain/intellij-ton/pull/606
- feat(tolk/documentation): show default parameter values in hover docs in https://github.com/ton-blockchain/intellij-ton/pull/663
- feat(tolk/highlighting): highlight `array` as a builtin type in https://github.com/ton-blockchain/intellij-ton/pull/642
- fix(tolk): don't use nested stub index access in https://github.com/ton-blockchain/intellij-ton/pull/662
- fix(tolk): use `createEmptyMap()` as the default value for map fields in https://github.com/ton-blockchain/intellij-ton/pull/682

### Other

- build: update GrammarKit tooling in https://github.com/ton-blockchain/intellij-ton/pull/592
- build: update changelog tooling in https://github.com/ton-blockchain/intellij-ton/pull/594
- build: update platform settings tooling in https://github.com/ton-blockchain/intellij-ton/pull/593
- feat(all): add plugin support for 2025.2 in https://github.com/ton-blockchain/intellij-ton/pull/689
- feat(all): highlight TON addresses as links in the terminal in https://github.com/ton-blockchain/intellij-ton/pull/632
- fix(all): define templates in configs in https://github.com/ton-blockchain/intellij-ton/pull/666
- fix(all): fix the IDE exception caused by a missing language in https://github.com/ton-blockchain/intellij-ton/pull/601
- fix(all): raise the since-build version to 243 in https://github.com/ton-blockchain/intellij-ton/pull/629

## [3.1.1]

Fixed small issues in Tolk 1.2 support.

## [3.1.0]

- feat(tolk): support lambdas from Tolk 1.2 by @i582 in https://github.com/ton-blockchain/intellij-ton/pull/586
- feat(tolk/completion): gray out new low-level functions from Tolk 1.2 in completion by @i582 in https://github.com/ton-blockchain/intellij-ton/pull/587
- feat(tolk): support `any_address` type from Tolk 1.2 by @i582 in https://github.com/ton-blockchain/intellij-ton/pull/588

## [3.0.0]

v3.0.0 is the biggest update of the plugin ever.
We have improved every part of the plugin, every supported language.
The list of supported languages has expanded, and now the plugin also supports TASM, the future assembler standard in
the Tolk language, and Fift assembly has received its improved support as part of Fift support.
BoC files are no longer just opaque binary files, you can open them and see the disassembled code!
In this version, we also supported the latest release of Tolk 1.1.

### Tolk

#### Features

- feat(tolk/1.1): initial support for `readonly` and `private` modifiers in https://github.com/ton-blockchain/intellij-ton/pull/534
- feat(tolk/1.1): overall Tolk support performance improvement in https://github.com/ton-blockchain/intellij-ton/pull/415
- feat(tolk/1.1): support enum serialization `enum Foo: uint8 {}` in https://github.com/ton-blockchain/intellij-ton/pull/523
- feat(tolk/1.1): support enums in https://github.com/ton-blockchain/intellij-ton/pull/473
- feat(tolk/asm): add injection of assembly language for Tolk assembly functions in https://github.com/ton-blockchain/intellij-ton/pull/329
- feat(tolk/completion): add "Fill all fields..." and "Fill required fields..." completion in https://github.com/ton-blockchain/intellij-ton/pull/367
- feat(tolk/completion): add `lazy` keyword completion in https://github.com/ton-blockchain/intellij-ton/pull/241
- feat(tolk/completion): add `return <expr>;` completion item for functions without an explicit return type in https://github.com/ton-blockchain/intellij-ton/pull/519
- feat(tolk/completion): add a completion variant to fill all match arms in https://github.com/ton-blockchain/intellij-ton/pull/314
- feat(tolk/completion): add a default field value on field completion inside a struct instance in https://github.com/ton-blockchain/intellij-ton/pull/362
- feat(tolk/completion): add a string argument for @deprecated annotation completion in https://github.com/ton-blockchain/intellij-ton/pull/269
- feat(tolk/completion): add better completion for match arms for enum types in https://github.com/ton-blockchain/intellij-ton/pull/475
- feat(tolk/completion): add completion for `@on_bounced_policy` annotation in https://github.com/ton-blockchain/intellij-ton/pull/307
- feat(tolk/completion): add completion for `builtin` keyword in https://github.com/ton-blockchain/intellij-ton/pull/243
- feat(tolk/completion): add completion for entry point functions like `onInternalMessage` and special methods `unpackFromSlice` and `packToBuilder` in https://github.com/ton-blockchain/intellij-ton/pull/389
- feat(tolk/completion): add enum keyword completion in https://github.com/ton-blockchain/intellij-ton/pull/486
- feat(tolk/completion): add enum members completion without a qualifier in https://github.com/ton-blockchain/intellij-ton/pull/525
- feat(tolk/completion): add postfix completion  in https://github.com/ton-blockchain/intellij-ton/pull/271
- feat(tolk/completion): add snippet completion for block keywords in https://github.com/ton-blockchain/intellij-ton/pull/265
- feat(tolk/completion): add snippets for instance and static methods in https://github.com/ton-blockchain/intellij-ton/pull/524
- feat(tolk/completion): better completion for complex types like generics in https://github.com/ton-blockchain/intellij-ton/pull/483
- feat(tolk/completion): better context return keyword completion in https://github.com/ton-blockchain/intellij-ton/pull/267
- feat(tolk/completion): better match completion in https://github.com/ton-blockchain/intellij-ton/pull/316
- feat(tolk/completion): don't add already processed types in match arm completion in https://github.com/ton-blockchain/intellij-ton/pull/275
- feat(tolk/completion): show enum member value in completion in https://github.com/ton-blockchain/intellij-ton/pull/477
- feat(tolk/completion): show low-level methods at the bottom of the completion list, don't show all methods if there are no available methods in https://github.com/ton-blockchain/intellij-ton/pull/497
- feat(tolk/completion): suggest only applicable annotations in https://github.com/ton-blockchain/intellij-ton/pull/309
- feat(tolk/documentation): add documentation for annotations in https://github.com/ton-blockchain/intellij-ton/pull/412
- feat(tolk/documentation): add inline rendered view for Tolk documentation and initial documentation for functions in https://github.com/ton-blockchain/intellij-ton/pull/408
- feat(tolk/documentation): add overall documentation in https://github.com/ton-blockchain/intellij-ton/pull/410
- feat(tolk/documentation): resolve element links like `[param]` in rendered doc in https://github.com/ton-blockchain/intellij-ton/pull/471
- feat(tolk/documentation): support more doc references to other declarations in https://github.com/ton-blockchain/intellij-ton/pull/552
- feat(tolk/formatting): don't add space after `assert` in `assert()` without throw in https://github.com/ton-blockchain/intellij-ton/pull/520
- feat(tolk/formatting): support enum formatting in https://github.com/ton-blockchain/intellij-ton/pull/484
- feat(tolk/grammar): support `type int = builtin` in https://github.com/ton-blockchain/intellij-ton/pull/375
- feat(tolk/gutter): add gutter icon for recursive calls in https://github.com/ton-blockchain/intellij-ton/pull/245
- feat(tolk/highlighting): add special highlighting for mutable parameters and mutable self parameters in https://github.com/ton-blockchain/intellij-ton/pull/370
- feat(tolk/highlighting): add underline highlighting for mutable variables in https://github.com/ton-blockchain/intellij-ton/pull/263
- feat(tolk/highlighting): calculate read/write access for variables in https://github.com/ton-blockchain/intellij-ton/pull/337
- feat(tolk/highlighting): highlight Tolk paths in Blueprint test output in https://github.com/ton-blockchain/intellij-ton/pull/379
- feat(tolk/highlighting): highlight `map` type as keyword in https://github.com/ton-blockchain/intellij-ton/pull/490
- feat(tolk/ide): add search by project symbols in https://github.com/ton-blockchain/intellij-ton/pull/291
- feat(tolk/ide): improve breadcrumbs, fix spacing, add method receiver name and self if any in https://github.com/ton-blockchain/intellij-ton/pull/285
- feat(tolk/ide): show a type of struct field in the structure view in https://github.com/ton-blockchain/intellij-ton/pull/335
- feat(tolk/inlay-hints): add an inlay hint with exit code short description in https://github.com/ton-blockchain/intellij-ton/pull/312
- feat(tolk/inlay-hints): add an inlay hint with implicit method id in https://github.com/ton-blockchain/intellij-ton/pull/261
- feat(tolk/inlay-hints): don't show parameter hints for the same name field access and function/method call in https://github.com/ton-blockchain/intellij-ton/pull/255
- feat(tolk/inlay-hints): enable a constant type hint by default in https://github.com/ton-blockchain/intellij-ton/pull/279
- feat(tolk/inspections): add `CallArgumentsCountMismatch` inspection in https://github.com/ton-blockchain/intellij-ton/pull/403
- feat(tolk/inspections): add a quickfix for unresolved references in https://github.com/ton-blockchain/intellij-ton/pull/355
- feat(tolk/inspections): add a quickfix to create a function, constant, global or local variable for unresolved identifier in https://github.com/ton-blockchain/intellij-ton/pull/397
- feat(tolk/inspections): add check for reassigned immutable variable or constant in https://github.com/ton-blockchain/intellij-ton/pull/297
- feat(tolk/inspections): add initial type mismatch inspection in https://github.com/ton-blockchain/intellij-ton/pull/404
- feat(tolk/inspections): add inspection about FunC-style functions instead of proper constant in https://github.com/ton-blockchain/intellij-ton/pull/501
- feat(tolk/inspections): add inspection for uninitialized fields in struct instance expression in https://github.com/ton-blockchain/intellij-ton/pull/401
- feat(tolk/inspections): add unused import inspection and 'Optimize imports' action in https://github.com/ton-blockchain/intellij-ton/pull/470
- feat(tolk/inspections): remove type when rename unused tensor/tuple variable to `_` via quickfix in https://github.com/ton-blockchain/intellij-ton/pull/558
- feat(tolk/inspections): support variable declarations in type mismatch inspection in https://github.com/ton-blockchain/intellij-ton/pull/492
- feat(tolk/intentions): add intention to convert an integer literal to another base in https://github.com/ton-blockchain/intellij-ton/pull/505
- feat(tolk/refactorings): add extract variable refactoring in https://github.com/ton-blockchain/intellij-ton/pull/399
- feat(tolk/rename): automatically wrap to backticks on rename to non-identifier name in https://github.com/ton-blockchain/intellij-ton/pull/299
- feat(tolk/resolving): support the new resolving algorithm from Tolk 1.1 in https://github.com/ton-blockchain/intellij-ton/pull/481
- feat(tolk/search): add search by `Type.method`-like name and add receiver to method presentation in https://github.com/ton-blockchain/intellij-ton/pull/441
- feat(tolk/tests): add three real world projects to tests in https://github.com/ton-blockchain/intellij-ton/pull/438
- feat(tolk/tests): enable more inspections in https://github.com/ton-blockchain/intellij-ton/pull/543
- refactor(tolk): cleanup plugin.xml in https://github.com/ton-blockchain/intellij-ton/pull/488

#### Fixes

- fix(tolk/builtin): fix `T.estimatePackSize` signature in https://github.com/ton-blockchain/intellij-ton/pull/541
  fix(tolk/completion): don't add duplicate completion variant for primitive types in Tolk 1.1 in https://github.com/ton-blockchain/intellij-ton/pull/561
- fix(tolk/completion): add `match` postfix completion only for expressions in https://github.com/ton-blockchain/intellij-ton/pull/380
- fix(tolk/completion): add `storage` snippet in https://github.com/ton-blockchain/intellij-ton/pull/386
- fix(tolk/completion): add completion for `@stdlib` in an import path in https://github.com/ton-blockchain/intellij-ton/pull/384
- fix(tolk/completion): don't complete `private` and `readonly` keywords inside struct fields comments in https://github.com/ton-blockchain/intellij-ton/pull/553
- fix(tolk/completion): don't gray out method completion if the qualifier is unresolved in https://github.com/ton-blockchain/intellij-ton/pull/556
- fix(tolk/completion): don't show "Fill all fields" for a non-empty struct instance in https://github.com/ton-blockchain/intellij-ton/pull/522
- fix(tolk/completion): don't show top level completion inside doc comments in https://github.com/ton-blockchain/intellij-ton/pull/567
- fix(tolk/completion): fix auto-import insertion for files without imports in https://github.com/ton-blockchain/intellij-ton/pull/356
- fix(tolk/completion): fix backticked declarations completion in https://github.com/ton-blockchain/intellij-ton/pull/414
- fix(tolk/completion): make `return <expr>;` the first option for return completion in https://github.com/ton-blockchain/intellij-ton/pull/526
- fix(tolk/completion): show completion for special methods only for aliases in https://github.com/ton-blockchain/intellij-ton/pull/395
- fix(tolk/documentation): don't highlight `->` as a keyword in hover documentation in https://github.com/ton-blockchain/intellij-ton/pull/550
- fix(tolk/formatting): don't format doc comments in https://github.com/ton-blockchain/intellij-ton/pull/539
- fix(tolk/highlighting): add read/write detection and highlighting for struct fields in https://github.com/ton-blockchain/intellij-ton/pull/382
- fix(tolk/highlighting): fix and improve code example in Color Scheme > Tolk in https://github.com/ton-blockchain/intellij-ton/pull/247
- fix(tolk/ide): don't show the file name in the structure view in https://github.com/ton-blockchain/intellij-ton/pull/333
- fix(tolk/inlay-hints): don't show parameter hints for `ton()` in https://github.com/ton-blockchain/intellij-ton/pull/253
- fix(tolk/inlay-hints): don't show parameter hints for single letter parameters in https://github.com/ton-blockchain/intellij-ton/pull/252
- fix(tolk/inlay-hints): show `foo:` instead of `foo = ` in https://github.com/ton-blockchain/intellij-ton/pull/250
- fix(tolk/inspections): don't suggest creating a local variable outside function in https://github.com/ton-blockchain/intellij-ton/pull/507
- fix(tolk/inspections): remove `UnusedTypeParameter` inspection for now in https://github.com/ton-blockchain/intellij-ton/pull/331
- fix(tolk/intentions): don't try to import non-top-level declarations in https://github.com/ton-blockchain/intellij-ton/pull/495
- fix(tolk/rename): forbid `self` renaming in https://github.com/ton-blockchain/intellij-ton/pull/439
- fix(tolk/resolving): don't resolve static access to the struct field in https://github.com/ton-blockchain/intellij-ton/pull/502
- fix(tolk/resolving): fallback to resolve by any methods only if the receiver has an unknown type in https://github.com/ton-blockchain/intellij-ton/pull/499
- fix(tolk/resolving): resolve a match pattern first to type and if unresolved — to other symbols in https://github.com/ton-blockchain/intellij-ton/pull/535
- fix(tolk/type-inference): correctly infer `never` return type for function that always throws in https://github.com/ton-blockchain/intellij-ton/pull/478
- fix(tolk/type-inference): fix `lazy ...` type inference in https://github.com/ton-blockchain/intellij-ton/pull/545
- fix(tolk/type-inference): fix type inference for `a.1` for `tuple` type in https://github.com/ton-blockchain/intellij-ton/pull/547

### FunC

#### Features

- feat(func): add indent settings in https://github.com/ton-blockchain/intellij-ton/pull/455
- feat(func): add initial type inference in https://github.com/ton-blockchain/intellij-ton/pull/529
- feat(func): better method completion in https://github.com/ton-blockchain/intellij-ton/pull/530
- feat(func/actions): add FunC file template with stdlib and file template with contract stub in https://github.com/ton-blockchain/intellij-ton/pull/536
- feat(func/completion): add live templates with top-level declarations and conditionals and loops in https://github.com/ton-blockchain/intellij-ton/pull/533
- feat(func/completion): better context return completion in https://github.com/ton-blockchain/intellij-ton/pull/273
- feat(func/documentation): add hover documentation for assembly instructions in https://github.com/ton-blockchain/intellij-ton/pull/328
- feat(func/documentation): add hover documentation for function parameters in https://github.com/ton-blockchain/intellij-ton/pull/354
- feat(func/documentation): show assembly function body in https://github.com/ton-blockchain/intellij-ton/pull/339
- feat(func/folding): add folding for block comments in https://github.com/ton-blockchain/intellij-ton/pull/277
- feat(func/highlighting): add a code example to Color Scheme > FunC in https://github.com/ton-blockchain/intellij-ton/pull/249
- feat(func/highlighting): better highlighting for method calls in https://github.com/ton-blockchain/intellij-ton/pull/352
- feat(func/ide): add breadcrumbs in https://github.com/ton-blockchain/intellij-ton/pull/283
- feat(func/ide): add search by project symbols in https://github.com/ton-blockchain/intellij-ton/pull/289
- feat(func/ide): add structure view in https://github.com/ton-blockchain/intellij-ton/pull/281
- feat(func/inlay-hints): add an inlay hint for `method_id` in https://github.com/ton-blockchain/intellij-ton/pull/287
- feat(func/inlay-hints): don't show parameter hints for the same name function/method call and single letter parameters in https://github.com/ton-blockchain/intellij-ton/pull/259
- feat(func/inspections): add a full-fledged type mismatch inspection in https://github.com/ton-blockchain/intellij-ton/pull/537
- feat(func/inspections): add a quickfix to create an undefined function in https://github.com/ton-blockchain/intellij-ton/pull/531
- feat(func/inspections): add unused include inspection in https://github.com/ton-blockchain/intellij-ton/pull/538
- feat(func/inspections): suggest converting an assembly function for constant with a proper constant in https://github.com/ton-blockchain/intellij-ton/pull/504
- feat(func/inspections): suggest to convert `if (...) { throw ... }` to `throw_if` in https://github.com/ton-blockchain/intellij-ton/pull/503
- feat(func/inspections): support quickfix to rename unused variable to `_` for variables inside tensor like ``(int a, int b) = ...` in https://github.com/ton-blockchain/intellij-ton/pull/532
- feat(func/inspections): warn about function with `()` return type and without `impure` modifier in https://github.com/ton-blockchain/intellij-ton/pull/509
- feat(func/inspections): warn about suspicious operator precedence in complex expressions in https://github.com/ton-blockchain/intellij-ton/pull/508
- feat(func/resolving): improve resolving logic in https://github.com/ton-blockchain/intellij-ton/pull/295
- feat(func/structure-view): show includes in structure view in https://github.com/ton-blockchain/intellij-ton/pull/345

#### Fixes

- fix(func/resolving): fix variables and methods resolving in https://github.com/ton-blockchain/intellij-ton/pull/443
- fix(func/find-usages): fix kind names in find usages for parameters and local variables in https://github.com/ton-blockchain/intellij-ton/pull/445

### TL-B

#### Features

- feat(tlb): add support for dependson `include` in https://github.com/ton-blockchain/intellij-ton/pull/454
- feat(tlb): add support for global `block.tlb` file in https://github.com/ton-blockchain/intellij-ton/pull/453
- feat(tlb/actions): add a new TL-B file action in https://github.com/ton-blockchain/intellij-ton/pull/350
- feat(tlb/completion): add completion for builtin TL-B types in https://github.com/ton-blockchain/intellij-ton/pull/390
- feat(tlb/completion): add completion for types in https://github.com/ton-blockchain/intellij-ton/pull/394
- feat(tlb/documentation): add hover documentation for TL-B types in https://github.com/ton-blockchain/intellij-ton/pull/446
- feat(tlb/documentation): add hover documentation for builtin TL-B types in https://github.com/ton-blockchain/intellij-ton/pull/447
- feat(tlb/highlighting): highlight all builtin types in fields in https://github.com/ton-blockchain/intellij-ton/pull/450
- feat(tlb/highlighting): highlight implicit TL-B field types in https://github.com/ton-blockchain/intellij-ton/pull/449
- feat(tlb/highlighting): use the default field color for TL-B fields in https://github.com/ton-blockchain/intellij-ton/pull/358
- feat(tlb/ide): add a structure view in https://github.com/ton-blockchain/intellij-ton/pull/304
- feat(tlb/ide): add breadcrumbs in https://github.com/ton-blockchain/intellij-ton/pull/305
- feat(tlb/inlay-hints): add an inlay hint with an implicit constructor tag in https://github.com/ton-blockchain/intellij-ton/pull/360
- feat(tlb/resolving): add all rule definitions in the go-to-definition popup for types in https://github.com/ton-blockchain/intellij-ton/pull/301

#### Fixes

- fix(tlb): use Type icon for constructors in https://github.com/ton-blockchain/intellij-ton/pull/310
- fix(tlb/colors): fix TL-B color settings example in https://github.com/ton-blockchain/intellij-ton/pull/452
- fix(tlb/find-usages): fix element kind name in find usages in https://github.com/ton-blockchain/intellij-ton/pull/392
- fix(tlb/lexer): allow identifiers starting with a number in https://github.com/ton-blockchain/intellij-ton/pull/256

### Fift

#### Features

- feat(fift): add initial support for Fift assembly (highlighting + folding) in https://github.com/ton-blockchain/intellij-ton/pull/293
- feat(fift): support interpreter sessions `[ ... ]` in Fift in https://github.com/ton-blockchain/intellij-ton/pull/459
- feat(fift-assembly): add go to definition and find references for functions and globals in https://github.com/ton-blockchain/intellij-ton/pull/457
- feat(fift/completion): add instruction completion in https://github.com/ton-blockchain/intellij-ton/pull/322
- feat(fift/documentation): add hover documentation for instructions in https://github.com/ton-blockchain/intellij-ton/pull/320
- feat(fift/ide): add a structure view for assembly in https://github.com/ton-blockchain/intellij-ton/pull/324
- feat(fift/ide): add breadcrumbs for assembly in https://github.com/ton-blockchain/intellij-ton/pull/326
- feat(fift/inlay-hints): add instruction gas consumption as inlay hints in https://github.com/ton-blockchain/intellij-ton/pull/318
- fix(fift): fix several parser errors in standard Fift files in https://github.com/ton-blockchain/intellij-ton/pull/458

### TASM

- feat(tasm): initial support and highlighting for TASM in https://github.com/ton-blockchain/intellij-ton/pull/463

### BoC

- feat(boc): add disassemble, Cell tree, Hex and Base64 view for BoC file in https://github.com/ton-blockchain/intellij-ton/pull/460
- feat(boc): integrate TASM support to BoC viewer in https://github.com/ton-blockchain/intellij-ton/pull/465

### Other

- feat(ci): running tests on all platforms by @Danil42Russia in https://github.com/ton-blockchain/intellij-ton/pull/416
- fix: removed deprecation warning by @Danil42Russia in https://github.com/ton-blockchain/intellij-ton/pull/467
- chore: improve README in https://github.com/ton-blockchain/intellij-ton/pull/341
- chore: improve plugin description in https://github.com/ton-blockchain/intellij-ton/pull/343
- chore: cleanup in https://github.com/ton-blockchain/intellij-ton/pull/347
- chore: add TASM to descriptions in https://github.com/ton-blockchain/intellij-ton/pull/466

## [2.57.6]

- Added support for Tolk 1.0 semicolon syntax
- Added sorting completions by resolved import in Tolk
- Added default values and `mutate` rendering in parameter info in Tolk
- Added inspection for "Expected `fun` keyword" with quick-fix in Tolk
- Added variants completions for union types inside match patterns in Tolk
- Removed `unpackFromSlice` and `packToBuilder` from "unused function" inspection
- Removed `.tolk` from completions
- Fixed `match` keyword completion in Tolk
- Fixed formatting for struct fields, match, assert statements in Tolk
- Fixed int257 type resolving
- Fixed completions for top level in case of empty file or beginning of file in Tolk
- Fixed type inference for union typed fields in a struct expressions
- Fixed auto import without `.tolk`

## [2.5.7.5]

- Added support for Tolk 1.0
- Added deprecated annotation message support in Tolk
- Added smart completion for match patterns in Tolk
- Fixed file renaming in Tolk
- Fixed unnecessary receiver reference for primitive types in Tolk
- Fixed smart casts in some cases in Tolk
- Fixed generics type inference in some cases in Tolk
- Fixed sorting of fields in Tolk
- Fixed completions for elements with the same name in Tolk
- Fixed formatting for `:` in function return type in Tolk
- Fixed intent for union types in type aliases in Tolk

## [2.5.7.4]
- Added notification if Tolk stdlib is not found
- Added icon for annotations in Tolk
- Added smart completion weighers for Tolk
- Added breadcrumbs support for Tolk
- Added sticky lines support for Tolk
- Added navigation bar support for Tolk
- Added Tolk file links in terminal
- Fixed deprecation references highlighting in Tolk
- Fixed some function resolving issues in Tolk
- Fixed union type deduce in Tolk type inference
- Fixed direct method reference with type arguments generics deducing in Tolk
- Fixed inference for type params named as primitives in Tolk
- Fixed paren variable type inference in Tolk
- Fixed a lot of minor issues in Tolk

## [2.5.7]
- [Tolk 0.99](https://github.com/ton-blockchain/ton/pull/1707) support

## [2.5.6]
- [Tolk 0.13](https://github.com/ton-blockchain/ton/pull/1694) support

## [2.5.5]

- [Tolk 0.12](https://github.com/ton-blockchain/ton/pull/1645) support
- Removed Tact support, use [Tact plugin](https://github.com/tact-lang/intelli-tact)

## [2.5.4]

- [Tolk 0.11](https://github.com/ton-blockchain/ton/pull/1610) support

## [2.5.3]

- [Tolk 0.10](https://github.com/ton-blockchain/ton/pull/1559) support

### Fixed
- Tolk: Resolving references in constants
- Tolk: Resolving builtin function references: `_+_`, etc.
- Tolk: Double `()` in function call completion
- Tolk: `MAX..MIN` in type rendering
- Tolk: some minor smart-casts issues

## [2.5.2]

### Added
- Tolk 0.9 support
- Tolk block documentations
- Tolk parameter hints
- Intellij Platform v241 support

## [2.5.0]

### Added
- Tolk 0.7 support
- Tolk type inference & completion
- Tolk type hints for variables & functions
- TL-B schema inspections
- TL-B constructor tag generator

### Fixed
- A lot of minor bugs

## [2.4.0]

### Added
- Tolk support
- TVM Assembly support
### Fixed
- A lot of minor bugs

## [2.3.0]

### Added

- Support for the latest Tact
  1.4.0 ([#177](https://github.com/ton-blockchain/intellij-ton/issues/177), [#180](https://github.com/ton-blockchain/intellij-ton/issues/180))
- Inspections for out of range integer values in FunC and Tact
- Inspections for integer division by zero in FunC and Tact
- Inspections for integer overflow in FunC
- Constant expression evaluation in FunC and Tact (with inline
  hints!) ([#22](https://github.com/ton-blockchain/intellij-ton/issues/22))

### Fixed

- `message` and `bounced` highlight not working in Tact ([#174](https://github.com/ton-blockchain/intellij-ton/issues/174))
- `com.intellij.diagnostic.PluginException: Template not found: Fift File` ([#182](https://github.com/ton-blockchain/intellij-ton/issues/182))
- `Storage for FuncNamedElementIndex.storage is already registered` ([#181](https://github.com/ton-blockchain/intellij-ton/issues/181))

## [2.2.0]

### Added

- Full support latest Tact 1.2.0 version ([#166](https://github.com/ton-blockchain/intellij-ton/issues/166))

### Fixed

- A bug with comment/uncomment already commented
  lines ([#169](https://github.com/ton-blockchain/intellij-ton/issues/169))
- Function parameter name hints for var arguments of same
  name ([#167](https://github.com/ton-blockchain/intellij-ton/issues/167))

## [2.1.0]

### Added

- Language selection in project template ([#148](https://github.com/ton-blockchain/intellij-ton/issues/148))
- Empty and example project templates ([#147](https://github.com/ton-blockchain/intellij-ton/issues/147))
- `#include` assist ([#108](https://github.com/ton-blockchain/intellij-ton/issues/108))

### Fixed

- Negative method IDs are considered a syntax
  error ([#157](https://github.com/ton-blockchain/intellij-ton/issues/157))
- Non-ASCII characters in FunC identifiers ([#156](https://github.com/ton-blockchain/intellij-ton/issues/156))
- Invalid trailing comma in return tuple type ([#155](https://github.com/ton-blockchain/intellij-ton/issues/155))
- Unresolved reference to uninitialized
  variable ([#151](https://github.com/ton-blockchain/intellij-ton/issues/151))
- Invalid indent for multiline tuples in function signature return
  type ([#150](https://github.com/ton-blockchain/intellij-ton/issues/150))
- Auto-complete not work on `slice~` ([#149](https://github.com/ton-blockchain/intellij-ton/issues/149))
- `method_id` completion ([#126](https://github.com/ton-blockchain/intellij-ton/issues/126))
- Reference resolving with identifiers containing non-letter
  characters ([#107](https://github.com/ton-blockchain/intellij-ton/issues/107))
- Uppercase HEX in TL-B ([#100](https://github.com/ton-blockchain/intellij-ton/issues/100))
- Indent in chain calls ([#38](https://github.com/ton-blockchain/intellij-ton/issues/38))

## [2.0.5]

- fixed builtin functions resolving for `load_int`, `load_uint`

## [2.0.0]

### Complete plugin rework

- Documentation rendering
- Tact support
- Blueprint project support
- A lot of bug-fixes and small futures

## [1.0.3]

### Added

- Support try-catch statements

## [1.0.2]

### Added

- Support [FunC Update 2022.05](https://telegra.ph/FunC-Update-202205-05-17)
- Auto-formatting in FunC
- Support UTF-8 names in FunC functions
- Auto-completion for inbuilt FunC statements
- Auto-completion for all FunC functions in project scope
- Documentation in FunC stdlib.fc
- Language injection support (FunC, Fift, TL-B) for string literals in regular programming languages
- New template for FunC files creation

## [0.8.0]

### Added

- Impure functions smart-inspections ([#17](https://github.com/ton-blockchain/intellij-ton/issues/17))
- TON-specific words in spellchecker dictionary ([#16](https://github.com/ton-blockchain/intellij-ton/issues/16))

### Fixed

- Ternary expression statements ([#15](https://github.com/ton-blockchain/intellij-ton/issues/15))
- Code suggestions don't show after `.` (dot) ([#13](https://github.com/ton-blockchain/intellij-ton/issues/13))

## [0.7.0]

### Added

- Variable references
- Type checking for method calls

### Fixed

- Fixed method duplication on neighbour files
- Fixed IndexOutOfBoundsException crash
- A lot of bug fixes

## [0.6.0]

### Added

- String literals & Constants support ([https://github.com/newton-blockchain/ton/pull/75]())
- Block folding
- Parameter hints in function calls
- References in method calls (`foo.bar()`)
- Resolving references by neighbour files in directory
- New icon for TL-B Schema files

### Fixes

- Function names with `:`
- A lot of small bugs and issues

## [0.5.0]

### Added

- TL-B Schemas support
- Using new type extension `.func` for FunC files

## [0.4.0]

### Added

- New grammar model for FunC
- New syntax highlighting for FunC
- New syntax highlighting for Fift
- Auto-completions for Fift
- References in Fift

### Fixed

- A lot of small bugs and issues

## [0.3.1]

### Fixed

- [#4](https://github.com/ton-blockchain/intellij-ton/issues/4) `elseif` & `elseifnot` statements
- [#5](https://github.com/ton-blockchain/intellij-ton/issues/5) Complete revision of the Fift gramamr

## [0.3.0]

### Added

- New function highlighting in FunC
- Function name completion suggestions in FunC
- Function references by `Ctrl+Left Click` or `Ctrl+B`
- Fixed Fift path declaration (`#!/usr/bin/fift`)
- Fixed `include` declaration in Fift
- Fixed `asm` declaration in Fift
