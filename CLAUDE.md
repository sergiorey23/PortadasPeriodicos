# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

Android app (package `sergirex.portadasperiodicos`) that shows daily newspaper front pages from kiosko.net. Kotlin, single `:app` module, XML layouts + ViewBinding (no Compose), Hilt, DataStore, Coil, Material 3. minSdk 23, targetSdk 36, compileSdk 37.

## Commands

Gradle needs a JDK 17 and `JAVA_HOME` is not set in the shell here:

```bash
export JAVA_HOME=/home/sergiorey/.jdks/temurin-17 PATH=$JAVA_HOME/bin:$PATH
./gradlew assembleDebug
./gradlew testDebugUnitTest
./gradlew testDebugUnitTest --tests "sergirex.portadasperiodicos.domain.EditionDateTest"   # one class (add .method for one test)
./gradlew lintDebug
./gradlew assembleRelease      # minified (R8) + signed; also bundleRelease for the Play .aab
```

- Run `testDebugUnitTest`, `lintDebug` and `assembleRelease` as separate invocations: chaining them intermittently fails `lintAnalyzeDebugUnitTest`, and it passes when rerun alone.
- Unit tests are plain JVM (no Robolectric): `android.util.Log` is not mocked, so code under test must not call it on the executed path (the repository takes an `onProblem` hook for this).
- Release signing reads `keystore.properties` (gitignored; template in `keystore.properties.example`). Never print or commit the keystore, `*.jks` or `keystorePortadas.txt`.
- `gradle.properties` must keep `android.builtInKotlin=false` and `android.newDsl=false`: the build applies the Kotlin plugin explicitly and fails without them (both are deprecated, removing them means migrating to AGP's built-in Kotlin).
- Versioning: bump `versionCode`/`versionName` in `app/build.gradle`; releases are tagged `vX.Y.Z`.

## Architecture

Package-by-layer inside one module: `domain/` (pure Kotlin models, repository interfaces, one-method use cases), `data/` (implementations), `presentation/` (ViewModels, UI state), `di/AppModule.kt` (Hilt bindings), `notifications/`, `ui/` (custom views). Activities/Fragments sit at the package root. Storage is DataStore everywhere (old SharedPreferences are migrated once with `SharedPreferencesMigration`/`DataMigration`; keep those migrations, users upgrade from old versions). `data/local/DataStoreExt.kt` has `safeData`/`editSafely` — use them so a bad preferences file falls back to defaults instead of crashing.

**Newspaper catalog (the data source for everything).** `PeriodicosRepositoryImpl` picks the highest `catalogVersion` among the downloaded copy (cached in `filesDir/catalog`) and `assets/periodicos.json`. `MyApplication` refreshes it on every foreground (ProcessLifecycle), rate-limited to once per 6 h, ETag-based. A downloaded file is adopted only if `CatalogParser` validates it (bad entries skipped, file rejected if invalid / newer schema / older version / <10 entries). It is published by GitHub Pages from `docs/catalog/periodicos.json`. To change the list: edit the asset, **bump `catalogVersion`**, copy the file byte-for-byte to `docs/catalog/` (`CatalogAssetTest` fails if they differ) — see `docs/catalog/README.md`. Favorites take their names from the catalog and hide papers that left it (unless the catalog is unreadable).

**Which edition to show.** `PortadaCoverRepositoryImpl` probes kiosko.net with HEAD requests walking back up to 20 days from the target date (concurrently, 6 papers at a time) and memoizes the answer; a fallback to an older day is re-probed after 30 min, a new target day never reuses an old answer. `EditionDate` owns the date rules: strings are `yyyy/MM/dd` (also URL path segments), "today" switches at 06:00, and date-picker values are UTC midnights (use `fromUtcMillis`/`toUtcMillis`). `CoverUrls` is the only place that knows the image URL layout (`.640.jpg` thumbnails, `.jpg` full size). Covers that aren't today's show a red date ribbon (grid: `CornerRibbonView`; detail: a flat label).

**Image loading.** One shared `OkHttpClient` (Hilt) serves both Coil (`MyApplication` is the `SingletonImageLoader.Factory`) and the HEAD probes. `CoverCacheControlInterceptor` rewrites Cache-Control on kiosko.net responses (past editions immutable, today's 30 min) because the CDN only sends ETag/Last-Modified. Grid thumbnails are loaded with `size(Size.ORIGINAL)` — leaving Coil to measure the wrap_content item `ImageView` left recreated pages blank. Bitmaps for save/share/widget go through `Context.loadBitmap()` (don't combine `ImageRequest.target()` with `execute()`: it hangs).

**Home screen state.** `PortadasViewModel` is scoped to the `Portadas` Activity, not to each tab Fragment, and holds one loader per `HomeTab` (favorites + 5 categories). Fragments are cheap and recreated freely by the ViewPager2 (default offscreen limit); a tab fetches only when it's RESUMED, re-loads when the day, the selected date or its newspaper list changed, and keeps old covers on screen if a reload yields nothing. The tab list is a `StateFlow` (favorites tab exists only while there are favorites); `Portadas.TabsAdapter` diffs it. The Activity is never destroyed when the user leaves (back = `moveTaskToBack`), which is why staleness across days is handled in the ViewModel.

**Detail screen** (`PortadaDetalle` + `PortadaDetalleFragment`) receives a `Parcelable` list (`Portada`) via `PortadaDetalle.createIntent`; fragments report the edition they actually displayed to `PortadaDetalleViewModel.resolvedDates` so save/share fetch that image.

**Ads / purchase.** Meta Audience Network banners via `AdManager` (home + detail); `BillingManager` (Play Billing, one-time `remove_ads_id`) makes Google Play the authority and writes `AdsEntitlementRepository`; screens observe it through `AdsViewModel` (null = not loaded yet, don't show a banner). Debug builds enable Meta test mode.

**Daily reminder.** `NotificationScheduler` is the single place that schedules the 09:00 alarm (setting on **and** notification permission); called at app start, on toggle, on permission result and from `BootReceiver` (alarms don't survive reboot/update). Plain `BroadcastReceiver`s get Hilt through an `EntryPoint` (`Context.notificationScheduler()`), not `@AndroidEntryPoint`, and use `launchAsync` (goAsync + 9 s timeout).

## Working in this repo

- The user writes in Spanish; code, comments and commits are in English.
- The emulator (`medium_phone`) may be shared with the user's other app (`sergirex.portadasrevistas`); don't start a second instance (two instances of the same AVD kill each other). The user has said there is no need to launch the emulator just to test a change; compiling is enough unless they ask.
- Layouts adapt to the banner: after an ad loads, floating buttons move up, so UI-automation taps must use bounds from a fresh `uiautomator dump`, not fixed coordinates.
