1. Create a Github release with a new tag in a format `vX.Y.Z`, e.g. `v0.1.2`
2. Github Actions workflow will start, building release and pushing it to maven central.
3. Staging repositories on Sonatype will be closed automatically, but need to be released manually.