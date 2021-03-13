Library:
* [ ] More kotlin-way
    * [ ] Refactor DiffRowGenerator to use kotlin-dsl-style builder
* [x] Explicit API mode
* [ ] Binary compatibility validator for JVM
* [ ] Benchmarks on JVM to compare with the original library

Infra:
* [ ] Static analysis (detekt, diktat)
* [ ] Git hooks
* [ ] Main CI worker depends on other two, executes task closeAndReleaseSonatypeStagingRepository if everything is successful