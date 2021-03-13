# kotlin-multiplatform-diff
![Build and test](https://github.com/petertrr/kotlin-multiplatform-diff/workflows/Build%20and%20test/badge.svg)
[![License](https://img.shields.io/github/license/petertrr/kotlin-multiplatform-diff)](https://github.com/petertrr/kotlin-multiplatform-diff/blob/main/LICENSE)
[![codecov](https://codecov.io/gh/petertrr/kotlin-multiplatform-diff/branch/main/graph/badge.svg)](https://codecov.io/gh/petertrr/kotlin-multiplatform-diff)
Todo: badge from maven central
Todo: badge for awesome kotlin

This is a port of [java-diff-utils](https://github.com/java-diff-utils/java-diff-utils) to kotlin
with multiplatform support. All credit for the implementation goes to original authors.

## Features
All features from version 4.9 of the original library are present, except for:
* Unified diff, which heavily uses file read/write and therefore needs a more complicated rewrite for kotlin-multiplatform
* diff-utils-jgit, which uses JVM-only jgit library

Please refer to the original guides for more information.

## Supported platforms
Currently, artifacts for the following platforms are supported:
* JVM
* JS (both browser and Node.js)
* LinuxX64
* MingwX64
* MacosX64