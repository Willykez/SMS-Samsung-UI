# OneMessages 0.5.0 — One UI 4 advanced design pass

## Source references inspected

- `OneMessages-samsung-reference-v4-ci-fixed2.zip`: the complete v4 SMS app source.
- `oneui-design-main.zip`: the supplied open-source One UI Design library and sample app.

The One UI library is primarily a legacy View/XML toolkit. OneMessages is Jetpack Compose, so the upgrade uses the supplied library as a design/interaction reference and implements equivalent Compose-native primitives rather than mixing two UI stacks.

## Catalog translated into OneMessages

| Supplied One UI catalog | Compose implementation |
| --- | --- |
| ToolbarLayout / toolbar search | Material 3 TopAppBar + custom rounded search field |
| RoundFrameLayout / RoundLinearLayout | One UI shape tokens + rounded Surface containers |
| RelatedCard / TipsCardPreference | `OneUiSectionCard` and grouped surfaces |
| SwitchBarLayout / SwitchBarPreference | `OneUiSettingRow` with Switch |
| MarginsTabLayout | `OneUiChipRow` / existing category row |
| GridMenuDialog | grouped/dropdown actions in conversation surfaces |
| StartEndTimePickerDialog | existing schedule flow; composer exposes schedule state |
| ColorPickerPreference | chat appearance color dialog |
| Separator | tonal surface/group spacing instead of hard grid lines |
| HapticSeekBar / SeekBarPreferencePro | existing settings slider pattern, ready for shared tokenization |
| NavigationBadgeIcon | Material `BadgedBox` in app navigation |
| Toast / transient notification | Compose SnackbarHost |
| ProgressDialog | Compose loading surfaces |
| DrawerLayout | navigation surfaces where needed |
| Index scroll utilities | lazy-list based conversation/message scrolling |

## Conversation screen changes

### Message bubbles
- Message grouping by sender and a short time window.
- Asymmetric One UI-style corners with tighter joins for consecutive messages.
- Separate timestamp/status row only at the end of a message group.
- Star state remains visible without disturbing text layout.
- Failed outgoing messages explicitly show `Not sent`.
- Date separators make long threads easier to scan.
- Bubble width increased modestly while retaining thumb-friendly margins.

### Composer
- Replaced the generic outlined Material text field with a rounded One UI composer surface.
- Uses `BasicTextField` so the container can adapt without Material text-field paddings.
- Send is an integrated circular action button.
- Schedule state is presented as an inline chip/row.
- Quick responses become a horizontal suggestion rail.
- SMS character/segment count appears contextually while typing.
- IME action sends the SMS.
- Enter key sends where the keyboard provides a key event.
- Composer grows to more lines while the keyboard is open.
- `imePadding()` keeps the composer above the IME/navigation area.
- Focus is retained after send so rapid SMS entry stays keyboard-first.
- Focused composer gets a subtle accent border.
- Secondary actions collapse when the keyboard is open to reclaim horizontal space.

### Header and menus
- Header keeps the contact identity compact and thumb reachable.
- Search uses a rounded inline field.
- Conversation actions remain available without turning the screen into a dense toolbar.
- Contact profile, mute, block and appearance actions remain intact.

## Reusable design layer

`ui/theme/OneUiComponents.kt` now contains Compose-native translations of the catalog:

- `OneUiTokens`
- `OneUiSectionCard`
- `OneUiSettingRow`
- `OneUiPill`
- `OneUiChipRow`

These are deliberately independent of the legacy View/XML library.

## Build verification

The source archive could be inspected and modified locally. A Gradle build could not be completed in this execution environment because the project wrapper attempted to download Gradle 8.9 and network access is unavailable. No claim of a successful APK build is made.

Recommended verification in Android Studio:

1. Sync Gradle.
2. Run `./gradlew :app:assembleDebug`.
3. Run on a device with the app selected as the default SMS application.
4. Exercise the thread with:
   - keyboard hidden/open,
   - one-line and multi-line drafts,
   - long SMS,
   - grouped incoming/outgoing messages,
   - failed/pending/sent/delivered states,
   - scheduled messages,
   - quick responses,
   - dark and AMOLED themes,
   - large font settings.
