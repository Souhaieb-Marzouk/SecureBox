# v1.0.0 QA Guide

This is the working checklist for the last two P3 items:

- **SB-046 — Accessibility pass**
- **SB-048 — End-to-end QA and v1.0.0 release**

Build under test: **SecureBox 0.9.6 (versionCode 21)**.
Record the build/version for every result.

## 1. Connect and identify the phone

```powershell
$adb = "C:\Users\Mercenary\Documents\deepseek-harness\Random\_toolchain\android-sdk\platform-tools\adb.exe"

& $adb devices -l
& $adb shell getprop ro.product.model
& $adb shell getprop ro.build.version.release
& $adb shell getprop ro.build.version.sdk
& $adb shell dumpsys package io.securebox | Select-String "versionName|versionCode"
```

Expected: the phone appears as `device`, and `versionName=0.9.6`, `versionCode=21`.

## 2. Screenshots and UI dumps

Use these throughout; they are the evidence for any bug.

```powershell
& $adb shell screencap -p /sdcard/sb-screen.png
& $adb pull /sdcard/sb-screen.png .\sb-screen.png

& $adb shell uiautomator dump /sdcard/sb-ui.xml
& $adb pull /sdcard/sb-ui.xml .\sb-ui.xml
```

Do not use `adb exec-out screencap -p > file` in PowerShell; PowerShell may write UTF-16 and corrupt the PNG.

## 3. Accessibility — 200% font scale

Detailed route on ColorOS/OPPO:

1. **Settings → Display & brightness → Font**.
2. Open **Font size**.
3. Set the slider to the **largest** value.
4. Tap **Apply**.
5. Return to SecureBox.

If the route differs, use **Settings → Display → Font size**.

Check every screen below. For each, verify:

- no text is clipped or hidden behind the bottom bar;
- buttons remain fully visible and tappable;
- cards do not overlap;
- the score, counts and dialog buttons remain readable;
- horizontal French labels still fit (switch to Français and repeat the worst screens).

| Screen | Route | Pass? | Notes |
|---|---|---|---|
| Dashboard | Home |  |  |
| Network | Network |  |  |
| Apps list | Apps |  |  |
| App detail | Apps → any app |  |  |
| Threats | Threats |  |  |
| Hardening | Threats → Hardening |  |  |
| Settings | Settings |  |  |
| Privacy shield | Settings → Privacy shield |  |  |
| DNS screen | Privacy shield → Set up DNS |  |  |
| Statistics | Privacy shield → Protection statistics |  |  |
| Threat Intelligence | Settings → Threat Intelligence |  |  |
| Help | Settings → How to use SecureBox |  |  |
| Onboarding | App data cleared, then launch |  |  |

Restore the font size to your normal value afterward.

## 4. Accessibility — TalkBack

### Enable

1. **Settings → Accessibility → TalkBack**.
2. Turn **Use TalkBack** on.
3. Confirm the permission/usage dialog.
4. Use these gestures:
   - **Swipe right** — next item.
   - **Swipe left** — previous item.
   - **Double-tap** — activate.
   - **Two-finger swipe** — scroll.
   - **Three-finger tap** — TalkBack menu.

### Assertions

| Area | Expected announcement/behavior | Pass? |
|---|---|---|
| Bottom navigation | Home, Network, Apps, Threats, Settings are announced and selectable |  |
| Dashboard score | Score and grade are announced with context, not just “65” |  |
| Quick actions | Each card's label and purpose are announced |  |
| Network firewall switch | Name, on/off state and purpose are announced |  |
| Blocked app list | App name and blocked state are announced |  |
| DNS Shield switch | Name, on/off state and purpose are announced |  |
| Resolver choices | Provider names and selection state are announced |  |
| Statistics | Each counter is announced with its label |  |
| Apps list | App name, risk level and blocked state are announced |  |
| App detail sheet | Close action and scroll content are reachable |  |
| Threats/hardening | Findings are announced; Fix actions are reachable |  |
| Settings | Toggles/radios announce label and state |  |
| Dialogs | Reset/confirmation dialogs announce title and both buttons |  |
| TV/D-pad | Focus moves visibly and reaches top/bottom text |  |

### Disable

Turn TalkBack off afterward.

## 5. End-to-end phone checklist

| # | Test | Expected | Pass? |
|---|---|---|---|
| 1 | Install 0.9.6 over 0.9.5 | Same signing key, data preserved |  |
| 2 | Accept VPN prompt | “Network protection” notification |  |
| 3 | Enable Local firewall + DNS Shield together | Both stay on; internet works |  |
| 4 | Block one app | No internet for that app |  |
| 5 | Non-blocked app | Internet works |  |
| 6 | Feed domain test | `ads.example.com` and `tracker.example.net` fail |  |
| 7 | Protection statistics | DNS count increases after the test |  |
| 8 | HTTPS page + download + upload | Completes without stalls |  |
| 9 | Update check | Version 0.9.6 reported, no false update |  |
| 10 | Threat feed update | Signature verified; domains/rules counts shown |  |
| 11 | Bandwidth screen | Per-app rows populate |  |
| 12 | Open ports | List or honest “unavailable” state |  |
| 13 | Upload anomaly notification | Test threshold behavior if feasible |  |
| 14 | Reboot | VPN returns with saved switches |  |
| 15 | Turn both switches off | VPN stops; internet returns |  |
| 16 | Fresh install / `pm clear` | Onboarding completes; VPN consent works |  |

> `adb shell pm clear io.securebox` wipes settings. Do this last and reconfigure afterward.

## 6. Android TV checklist

Connect the TV box first:

```powershell
& $adb connect <TV-IP>:5555
& $adb devices -l
```

| # | Test | Expected | Pass? |
|---|---|---|---|
| 1 | Install 0.9.6 | App appears in Leanback launcher |  |
| 2 | D-pad through Network | Text above first card and below last card reachable |  |
| 3 | Apps → app detail | Refresh and bottom actions reachable |  |
| 4 | Enable both protections | Internet works |  |
| 5 | Block one app | That app has no internet |  |
| 6 | DNS test | Feed/test domain blocked |  |
| 7 | Statistics | DNS counter increases |  |
| 8 | Reboot | Both switches and VPN return |  |

## 7. Evidence to capture

- One screenshot per failed row.
- `sb-ui.xml` for the failed screen.
- Full logcat around the failure:

```powershell
& $adb logcat -c
# reproduce
& $adb logcat -d -s SecureBoxTunnel:I AndroidRuntime:E > .\securebox-qa.txt
```

Send the screenshots, UI XML and log; do not include personal data.

## 8. Closing SB-046 and SB-048

### SB-046

Close only when:

- every row in sections 3 and 4 passes on the phone;
- the TV section 6 D-pad rows pass;
- no clipped text at maximum font.

### SB-048 / v1.0.0

After every phone and TV row is green:

1. Bump `versionCode` to `22` and `versionName` to `1.0.0`.
2. Update `CHANGELOG.md`, `README.md`, `docs/backlog.html` and release notes.
3. Run:

```powershell
.\gradlew.bat verifyNoHardcodedStrings test lintDebug assembleRelease
```

4. Sign, stage under `SecureBox-dist\v1.0.0`, generate `SHA256SUMS`, sign with Ed25519, verify.
5. Publish the GitHub release with the full description and all four APKs.
6. Record the release hashes in `docs/VERIFICATION.md`.
