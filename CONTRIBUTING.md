# Contributing to SaavnMp3‑Android 🎧

Thanks for helping make **SaavnMp3‑Android** better! We welcome all kinds of contributions — bug fixes, new features, documentation improvements, and more.

This project is an ad-free Android music streaming app built with **Kotlin**,
targeting Java 17, and using the unofficial JioSaavn API.

---

## Getting Started

### Prerequisites

Before contributing, ensure your development environment is ready:

- JDK 17
- Android Studio and the Android SDK for API 37
- Android 7.0 (API 24) or later emulator/device for manual testing
- Git

### Forking & Cloning

1. Fork this repository to your own GitHub account.  
2. Clone your fork:

   ```bash
   git clone https://github.com/<your-username>/SaavnMp3-Android.git
   cd SaavnMp3-Android
   ```

3. (Optional but recommended) Add upstream remote:

   ```bash
   git remote add upstream https://github.com/harshshah6/SaavnMp3-Android.git
   ```

### Setting Up Locally

* Open the cloned project in Android Studio and allow Gradle to sync.
* Build with `./gradlew assembleDebug` (on Windows, `gradlew.bat assembleDebug`).
* Run unit tests with `./gradlew testDebugUnitTest`.
* Run the app on an emulator or device for changes that affect runtime behavior.

---

## How to Contribute

### Reporting Bugs

When you spot a bug:

* Search existing open/closed issues — maybe it’s already reported.
* If not, create a new issue with:

  * A clear title
  * Steps to reproduce
  * Expected vs actual behavior
  * Logs, stack trace, screenshots
  * Device / OS version info

### Suggesting Enhancements

For new features or improvements:

* Create an issue labeled “enhancement” (or similar).
* Describe:

  * What you want to do
  * Why it’s needed / use case
  * Any sketches or mockups
  * Possible implementation approach

### Submitting Pull Requests

1. Create a descriptive branch from `master`. Example:

   ```bash
   git checkout -b feature/your-feature-name
   ```

2. Work on your change.

3. Commit with a meaningful message.

4. Push your branch to your fork:

   ```bash
   git push origin feature/your-feature-name
   ```

5. Open a Pull Request (PR) against the upstream `master` branch.

6. In the PR description, include:

   * What change or feature you have implemented
   * Why this change was needed
   * How to test it
   * Any screenshots (if UI changes)

7. Be ready to respond to review feedback and make updates.

---

## Coding Guidelines

### Style & Formatting

* Follow Kotlin and Android naming conventions (`camelCase` for properties and functions, `PascalCase` for classes).
* Keep methods short and single-purpose.
* Avoid magic numbers — use constants or resource files.
* Use resource files (`strings.xml`, `colors.xml`, `dimens.xml`) for user-facing UI text and shared design values.
* Add concise KDoc or comments when public APIs or non-obvious behavior need explanation.
* Keep UI responsive — avoid doing heavy tasks on the main thread.
* Use Android best practices (if applicable).

### Commit Messages

Use clear, conventional commit messages. A recommended structure:

```
<type>(<scope>): <short description>

Optional detailed explanation.
```

Where `<type>` might be:

* `feat`: new feature
* `fix`: a bug fix
* `docs`: documentation changes
* `style`: formatting, whitespace, etc
* `refactor`: code structure changes (no feature/bug)
* `test`: adding or updating tests
* `chore`: build / tooling changes

Example:

```
feat(player): add shuffle mode for playback

Allows user to toggle shuffle mode in the player UI.  
```

### Branching

* `master` is the stable branch.
* Branch new work from `master` (e.g. `feature/xyz`, `fix/issue-123`).
* Avoid working directly on `master` for unreviewed changes.

### Testing
* Before submitting a PR, run all tests locally and ensure nothing is broken.

---

## Review & Merge Process

* PRs will be reviewed by maintainers or experienced contributors.
* You may be asked to revise style, logic, or documentation.
* After approval and passing CI checks, your PR will be merged (via squash or merge).
* We may add you to the list of contributors if your contributions continue.

Don’t be discouraged by feedback — it helps improve code quality and consistency.

---

## Need Help?

* You can ask questions by opening an issue with the label “question.”
* You may also tag project maintainers or contributors in issues or PRs for guidance.
* For general discussions or planning, issues or project boards can be used.

---

Thank you — your contributions help make **SaavnMp3‑Android** stronger and better for everyone! 🙏
