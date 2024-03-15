# TON Plugin for the IntelliJ IDEs Changelog

## [2.1.0]

- Added language selection in project template ([#148](https://github.com/ton-blockchain/intellij-ton/issues/148))
- Added empty and example project templates ([#147](https://github.com/ton-blockchain/intellij-ton/issues/147))

- Fixed negative method IDs are considered a syntax
  error ([#157](https://github.com/ton-blockchain/intellij-ton/issues/157))
- Fixed non-ASCII characters in FunC identifiers ([#156](https://github.com/ton-blockchain/intellij-ton/issues/156))
- Fixed invalid trailing comma in return tuple type ([#155](https://github.com/ton-blockchain/intellij-ton/issues/155))
- Fixed unresolved reference to uninitialized
  variable ([#151](https://github.com/ton-blockchain/intellij-ton/issues/151))
- Fixed invalid indent for multiline tuples in function signature return
  type ([#150](https://github.com/ton-blockchain/intellij-ton/issues/150))
- Fixed auto-complete not work on `slice~` ([#149](https://github.com/ton-blockchain/intellij-ton/issues/149))
- Fixed `method_id` completion ([#126](https://github.com/ton-blockchain/intellij-ton/issues/126))
- Fixed reference resolving with identifiers containing non-letter
  characters ([#107](https://github.com/ton-blockchain/intellij-ton/issues/107))
- Fixed uppercase HEX in TL-B ([#100](https://github.com/ton-blockchain/intellij-ton/issues/100))
- Fixed indent in chain calls ([#38](https://github.com/ton-blockchain/intellij-ton/issues/38))

## [2.0.5]

- fixed builtin functions resolving for `load_int`, `load_uint`

## [2.0]

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

- Impure functions smart-inspections ([#17](https://github.com/andreypfau/intellij-ton/issues/17))
- TON-specific words in spellchecker dictionary ([#16](https://github.com/andreypfau/intellij-ton/issues/16))

### Fixed

- Ternary expression statements ([#15](https://github.com/andreypfau/intellij-ton/issues/15))
- Code suggestions don't show after `.` (dot) ([#13](https://github.com/andreypfau/intellij-ton/issues/13))

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

- [#4](https://github.com/andreypfau/intellij-ton/issues/4) `elseif` & `elseifnot` statements
- [#5](https://github.com/andreypfau/intellij-ton/issues/5) Complete revision of the Fift gramamr

## [0.3.0]

### Added

- New function highlighting in FunC
- Function name completion suggestions in FunC
- Function references by `Ctrl+Left Click` or `Ctrl+B`
- Fixed Fift path declaration (`#!/usr/bin/fift`)
- Fixed `include` declaration in Fift
- Fixed `asm` declaration in Fift
