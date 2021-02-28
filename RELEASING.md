1. Push a new git tag of a format 'v*', e.g. v1.0.0
2. Github Actions workflow will start, building release and pushing it to maven central. It will then create a github release.
3. Update github release with release notes.