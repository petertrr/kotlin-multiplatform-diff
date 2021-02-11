# kotlin-diff-utils
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