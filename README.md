# NibbleMath

Android recipe book that shows the true cost of what you cook — per batch and per item.

- Recipe cards across multiple recipe books
- Cost a recipe from the ingredients you actually use (brand, pack size, price)
- Price lookup at Woolworths, Coles and ALDI, with manual override
- Pantry of go-to ingredients for fast recipe building
- OCR + camera recipe ingestion
- Metric-first units with translation; intelligent scaling

See [docs/spec.md](docs/spec.md) for the full spec.

## Development

Kotlin + Jetpack Compose. Development follows the Android dev playbook in the
Projects repo (`docs/android-dev-playbook.md`) — pure Linux toolchain, no IDE.

```bash
./gradlew installDebug   # build + install to connected device/emulator
```
