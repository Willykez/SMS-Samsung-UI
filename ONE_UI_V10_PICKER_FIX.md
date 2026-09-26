# OneMessages 1.0.0 — Picker compile fix

The previous picker implementation used `androidx.compose.material3.TimePickerDialog`, which is not available in the Compose Material3 version pinned by this project (Compose BOM 2024.06.00). It also triggered experimental Material3 diagnostics.

The picker now uses only APIs available in the pinned Material3 release:
- `AlertDialog`
- `DatePicker`
- `TimePicker`
- `rememberDatePickerState`
- `rememberTimePickerState`

The entire picker is annotated with `@OptIn(ExperimentalMaterial3Api::class)` and uses a two-stage One UI-inspired flow (date → time), rounded dialog shape, large hierarchy, explicit Back/Next/Done/Cancel actions, device 12/24-hour preference, and future-time validation.

No SESL dependencies or forked AndroidX artifacts are introduced.
