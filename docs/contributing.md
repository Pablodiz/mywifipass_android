# Contributing

## How to Contribute

We welcome contributions to the MyWifiPass Android app. Please follow these guidelines.

## Development Process

1. **Fork** the repository on GitHub
2. **Create a branch** named after the issue: `XX-short-description`
3. **Make your changes** following the code style below
4. **Test** your changes on a real device or emulator
5. **Submit a pull request** with a clear description

## Commit Conventions

Use [Conventional Commits](https://www.conventionalcommits.org/):

```
feat: add long-press to delete WiFi passes
fix: handle empty FIDO2 challenge response
docs: update installation guide for API 35
```

Prefixes: `feat`, `fix`, `docs`, `refactor`, `test`, `chore`, `security`

## Code Style

- **Kotlin** standard conventions (4-space indent, camelCase)
- **Compose** best practices - stateless composables, state hoisting
- All user-facing strings in `res/values/strings.xml` (+ `values-es/`)
- Log with `android.util.Log` - tag = class name, appropriate level
- Coroutines: `Dispatchers.IO` for network/DB, `Dispatchers.Main` for UI

## Testing

There are no tests in the project yet (see CODE_REVIEW.md). Run `./gradlew lint` before committing.

## Pull Request Checklist

- [ ] Code compiles (`./gradlew assembleDebug`)
- [ ] Lint passes (`./gradlew lint`)
- [ ] No new warnings
- [ ] Strings added for both English and Spanish
- [ ] Works on API 29 (Android 10) as minimum
- [ ] Commit messages follow conventional format
- [ ] No IDE-generated files committed

## Reporting Issues

[Open an issue](https://github.com/Pablodiz/mywifipass_android/issues/new/choose) with:
- Android version and device model
- Steps to reproduce
- Expected vs actual behavior
- Relevant logcat output

## License

This project is licensed under the BSD 3-Clause License. All contributions are subject to this license.
