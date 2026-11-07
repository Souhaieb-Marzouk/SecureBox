# Performance

Measured on the reference phone (OPPO CPH2173, Android 14, API 34) with a release build, screen
unlocked, three consecutive cold starts via `adb shell am start -W`:

| Run | TotalTime |
|---|---|
| 1 | 414 ms |
| 2 | 403 ms |
| 3 | 400 ms |

**Budget: cold start under 1,000 ms.** The current build is ~40% of budget.

## APK size

| Artifact | Size |
|---|---|
| Universal (all ABIs) | 2.32 MB |
| arm64-v8a | 2.23 MB |
| armeabi-v7a | 2.23 MB |
| x86_64 | 2.23 MB |

**Budget: universal under 40 MB.** The app is far below it; the native pump adds ~120 KB.

## What keeps startup fast

- No global blocking work in `Application.onCreate`; scheduling uses WorkManager only.
- The scanner rules are loaded lazily on the first scan, not at startup.
- The feed client is only touched from the Threat Intelligence screen or the daily worker.
- The dashboard computes its score from local settings, not from package enumeration.

## How to re-measure

```powershell
$adb = "$env:ANDROID_HOME\platform-tools\adb.exe"
& $adb shell am force-stop io.securebox
& $adb shell am start -W -n io.securebox/.MainActivity
```

Run it three times and compare `TotalTime` with the budget above. Report a regression if the median
exceeds 700 ms on a mid-range device.

## Baseline profiles

A generated baseline profile is not shipped yet. The cold-start budget is already met without one;
if it regresses, add the `androidx.baselineprofile` module and generate a profile on this device
before resorting to manual optimisation.
