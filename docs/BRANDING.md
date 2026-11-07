# SecureBox brand

## Mark

![SecureBox mark](branding/logo-1024.png)

The SecureBox mark is a 2.5D shield: a teal-to-violet bevel, a deep glass face and an embossed
check. It reads as protection without looking like a generic antivirus warning. The mark is
generated from `tools/generate-brand-assets.py`, so every launcher density and the TV banner stay
consistent.

## Palette

| Token | Hex | Use |
|---|---|---|
| Ink 950 | `#05070F` | Base background |
| Ink 900 | `#0A1020` | Cards / surfaces |
| Ink 800 | `#111A31` | Raised surfaces |
| Aurora Teal | `#35F2D0` | Primary action, safe state |
| Aurora Cyan | `#22C7E8` | Gradient partner |
| Aurora Violet | `#8B7CFF` | Secondary action, depth |
| Aurora Pink | `#FF6FD8` | Accent light |
| Aurora Amber | `#FFC46B` | Attention |
| Aurora Coral | `#FF6B81` | Danger |
| Text primary | `#F2F7FF` | Main text |
| Text secondary | `#A7B6D4` | Supporting text |

The light scheme uses `#F5F8FF` / `#FFFFFF` with `#00796B` and `#5B4BD6` accents.

## Typography

System sans-serif with a heavy display weight (Black/ExtraBold) for scores and headings, Semibold
for titles, and a 15–16 sp body on phones. The TV theme scales body text to 20 sp and headlines to
40 sp so everything is readable from a couch.

## Assets

- `logo-1024.png` — master transparent mark.
- `icon-512.png` — store icon.
- `tv-banner.png` — 960×540 source; Android TV density PNGs live in `app/src/main/res/drawable-*`.
- `app/src/main/res/mipmap-*/ic_launcher_foreground.png` — adaptive icon foregrounds.
- `app/src/main/res/drawable/ic_launcher_background.xml` — adaptive gradient background.

## Regenerate

```powershell
python tools/generate-brand-assets.py
```

Requires Pillow and numpy (both available in the project toolchain).

## Usage

Use the mark on the Aurora backdrop or a plain ink surface. Keep at least half the mark's width as
clear space. Do not recolour the check; the teal-to-violet rim is the identifying element.
