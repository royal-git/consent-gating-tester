# ConsentGatingLab

Tiny Kotlin/Android playground I hacked together to sanity-check consent-gated AppsFlyer init/stop flows without touching the real app. It is not production-ready, audited, or polished—just a vibe-coded tool to poke at SDK behaviours.

## What’s Here
- Switchable consent + mock UMP signals to drive start/stop decisions.
- An `AnalyticsController` that swaps between a noop sink and the real AppsFlyer sink.
- A tabbed debug UI so you can flip consent, fire “app” events (gated), or hammer the raw AppsFlyer APIs and watch Logcat.

## Try It
1. Open the project in Android Studio (Chipmunk+).
2. Keep the `debug` build variant selected.
3. Run on an emulator/device and play with the dashboard toggles.

## Notes
- AppsFlyer dev key is placeholder; swap in your own if you actually need network calls.
- Consent policy comes from `assets/sdk_policy.json`—tweak it to experiment with different requirements.
- Expect rough edges, commented TODOs, and plenty of Timber logging.

## License

MIT License – see `LICENSE`.
