# Switchroot Android support

SwitchDeck's Nintendo Switch target is Switchroot Android. The stock Nintendo Switch system (Horizon OS) cannot install or run this Android APK.

## Target and graphics route

- Use a [Switchroot Android 14/15 build](https://wiki.switchroot.org/wiki/android/android-14-15) on a supported Nintendo Switch model. Follow Switchroot's own compatibility notes for the Switch revision and image.
- SwitchDeck enables the experimental Tegra route only when it recognizes a Switchroot device, Android is API 34 or newer, and Android reports an NVIDIA/Tegra Vulkan device with Vulkan 1.1 or newer.
- The route uses SwitchDeck's Android Vulkan bridge. It does not install a Linux Tegra driver or replace Android's NVIDIA driver. Gamescope, Proton, and individual games may still fail, even when the bridge starts.
- Switchroot Android is expected to expose the GPU as `/dev/nvhost-gpu` or a DRM render node. SwitchDeck exposes a guest render-node entry for the bridge path when one of those nodes is available.
- The graphics route, game compatibility, docked/handheld display behavior, suspend/resume, and thermals have not been validated on physical Switch hardware yet. Treat the route as experimental and include a session log with compatibility reports.

## Controllers and limits

Switchroot's [Android input configuration](https://wiki.switchroot.org/wiki/faq) supplies controller events to apps. SwitchDeck already handles standard Android gamepads and Joy-Con devices; configure or pair Joy-Cons in Switchroot first, then map them in SwitchDeck's controller settings if needed.

Nintendo Switch has limited memory and GPU headroom for a Steam/Linux desktop. Start with the Linux runtime, close other Android apps, and try a light game first. x86 games also run through ARM emulation and may be slow or unsupported.

## Build

Build the Android arm64 APK with the repository's standard [build instructions](../README.md#build). This fork does not yet publish its own signed APK or update catalog; do not install DroidDeck releases as SwitchDeck updates.
