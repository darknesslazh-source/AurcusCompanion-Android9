# Build APK with GitHub Actions

1. Create or open your GitHub repository.
2. Upload the *contents* of this folder (`AurcusCompanion-APK-Ready`) to the repository root, including the hidden `.github/workflows/build-apk.yml` file. Do not upload the parent folder as an extra nested directory.
3. Commit to `main` or `master`.
4. Open the repository's **Actions** tab and select **Build Aurcus Companion APK**.
5. If the workflow has not started automatically, choose **Run workflow**.
6. When the run succeeds, open it and download the `AurcusCompanion-debug-apk` artifact.

The workflow compiles a debug APK; it does not guarantee compilation until GitHub Actions actually runs. Check the build logs if Kotlin compilation fails.
