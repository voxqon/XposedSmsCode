# magisk-ui-kit

Shared Android Compose UI primitives for the Magisk317 app family.

Current extracted surface:

- `io.github.magisk317.uikit.theme`
  - `MagiskUiKitTheme`
  - `MagiskThemeMode`
  - `UpdateSystemBars`
  - `SystemBarsScrim`
  - `Spacing`
- `io.github.magisk317.uikit.foundation`
  - `BasePresenter`
  - `BaseView`
  - `TextWatcherAdapter`
  - `BaseItemCallback`
  - `ItemCallback`
  - `ItemChildCallback`
  - `ImmutableListWrapper`
  - `LoadingIndicatorTokens`
  - `rememberMinDurationLoading`
  - `AppLinearLoadingIndicator`
  - `PolygonMorphLoadingIndicator`
  - `LocalSnackbarHostState`
  - `SessionLoadingRegistry`
- `io.github.magisk317.uikit.shell`
  - `BaseActivity`
  - `PlayUpdateDelegate`
  - `AppInfoHelper`
  - `RecordItem`
- `io.github.magisk317.uikit.common`
  - `DismissibleSnackbarHost`
- `io.github.magisk317.uikit.preference`
  - `SectionCard`
  - `Item`
  - `StateSwitchItem`
  - `ActionSwitchItem`
  - `UrlSourceSettingsScreen`
- `io.github.magisk317.uikit.surface`
  - `ExpressiveHeroCard`
  - `ExpressiveSectionCard`
  - `FeatureEntryCard`
  - `MetricCard`
  - `MetricGrid`
  - `MetricSpec`
  - `InfoPill`
  - `DetailSectionCard`
  - `DetailRow`
  - `DetailDivider`
  - `OverlayHeaderScaffold`
  - `OverlayHeaderPanel`
  - `ChromeBlurBackground`
  - `chromeSurfaceColor`
  - `chromeTopAppBarColors`
  - `AnimatedCompactBottomNavigationChrome`
  - `AnimatedSystemBarsScrim`
  - `MainTabScaffold`
  - `MainTabSpec`
  - `resolveMainChromeVisible`
  - `tabTransitionDirection`
  - `tabEnterTransition`
  - `tabExitTransition`
  - `tabPopEnterTransition`
  - `tabPopExitTransition`
  - `tabPredictivePopEnterTransition`
  - `tabPredictivePopExitTransition`
- `io.github.magisk317.uikit.pager`
  - `MainPagerState`
  - `rememberMainPagerState`
  - `PagerNavigationSpringSpec`
- `io.github.magisk317.uikit.onboarding`
  - `OnboardingInfoCard`
  - `OnboardingProgressRow`
  - `OnboardingLogCard`
  - `OnboardingActionRowCard`
  - `OnboardingSummaryHeader`
  - `OnboardingStatusRow`
  - `OnboardingItemState`
- `io.github.magisk317.uikit.surface`
  - `PagerTabScaffold` for top-level pages backed by `HorizontalPager`
  - `PagerTabScaffold` keeps deep-route ownership in the parent app and supports configurable
    adjacent-page precomposition via `beyondViewportPageCount`
  - `SearchWorkspaceScaffold`
  - `WorkspaceTopBarSearchOverlay`
  - `WorkspaceSearchField`
  - `WorkspaceListItem`
  - `WorkspaceEmptyState`
  - `WorkspaceTrailingIcon`
  - `LabelValueBlock`
  - `ActionStrip`
  - `SectionColumn`
  - `StatusHeroCard` plus `rememberStatusCardClickHandler` / `StatusCardTapPolicy`

`StatusHeroCard` owns the shared status-card click contract: pass `isEntitled` together with
`onActivateClick` (single tap while entitlement is missing) and `onDiagnosticsToggle` (5 taps
within 1.8s while entitled). Parent apps only inject their own activation entry and diagnostics
toggle; the tap-window rules live in `StatusCardTapPolicy` and must not be re-implemented per app.
Omitting both callbacks keeps the legacy plain `onClick` behavior.

`MainTabScaffold` reports its effective compact-navigation inset through
`onContentBottomPaddingChanged` after a successful composition. The callback receives the initial
value and later changes, including `0.dp` when compact chrome is hidden; unrelated recompositions
do not repeat the same value. Replacing the callback does not replay the current inset; the next
changed inset is delivered to the latest callback. Parent apps can use this contract to keep
transient UI such as snackbars above the animated bottom chrome. Set `showSystemBarsScrim` to
`false` when a parent owns system-bar scrims; the default remains `true` for backward compatibility.

The `io.github.magisk317.uikit.onboarding` package holds stateless setup-wizard card
primitives shared by parent onboarding flows. The components own no business logic: callers
pass every string, state, and navigation callback, and resolve intents on return. They render
exclusively through the dual-track `AppCard` / `AppText` / `AppCircularProgressIndicator`
contracts so Miuix and Material parents stay visually consistent.

`OnboardingItemState` is the shared tint contract for the end-of-flow report:
`OnboardingSummaryHeader` lays out the headline (title plus the caller's own granted/missing
count lines), `OnboardingStatusRow` renders a read-only requirement line, and
`OnboardingActionRowCard` accepts the same state through its optional trailing `state`
parameter. `Granted` resolves to the primary role, `Outstanding` to the error role and
`Pending` to the on-surface-variant role, so a row whose state could not be read is shown in
the same warning tint as an unmet one and the two stay distinguished only by the wording the
parent supplies. The state is presentation-only: it never selects an icon, an intent or an
enabled condition, and `OnboardingActionRowCard` keeps rendering uncoloured when `state` is
omitted, so pre-existing callers are unaffected.

## Navigation ownership

The kit owns reusable, stateless navigation presentation:

- compact bottom navigation and expanded rail layout
- chrome visibility resolution and measured content inset reporting
- tab direction and regular/predictive enter/exit transition helpers
- tab metadata that is generic over the parent application's route type

Parent applications own route declarations, tab order and count, business screens, back-stack
policy, refresh signals, and feature state. Do not add product route names, ViewModels, repositories,
or app-specific navigation decisions to this repository. A shared behavior should move here only
when at least two consumers use the same semantics; visual similarity alone is not sufficient.

## Include from another repo

Recommended: add it as a git submodule in the parent repository root.

```bash
git submodule add https://github.com/magisk3171/magisk-ui-kit.git magisk-ui-kit
```

Then in `settings.gradle.kts`:

```kotlin
include(":magisk-ui-kit")
```

Then in the target module:

```kotlin
dependencies {
    implementation(project(":magisk-ui-kit"))
}
```
