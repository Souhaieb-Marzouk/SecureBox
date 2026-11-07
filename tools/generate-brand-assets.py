"""Generates SecureBox brand assets from one source definition.

Run: python tools/generate-brand-assets.py
Requires Pillow + numpy (both are available in the project toolchain).
"""

from __future__ import annotations

import math
import os
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "app" / "src" / "main" / "res"
CORE_UI_RES = ROOT / "core-ui" / "src" / "main" / "res"
BRAND = ROOT / "docs" / "branding"

S = 2048  # supersampled working canvas
CENTER = (S / 2, S / 2)


def qbez(p0, p1, p2, steps=48):
    pts = []
    for i in range(steps):
        t = i / steps
        x = (1 - t) ** 2 * p0[0] + 2 * (1 - t) * t * p1[0] + t ** 2 * p2[0]
        y = (1 - t) ** 2 * p0[1] + 2 * (1 - t) * t * p1[1] + t ** 2 * p2[1]
        pts.append((x, y))
    return pts


def shield_polygon(size: int) -> list[tuple[float, float]]:
    """A shield silhouette with slightly curved sides, normalised to `size`."""
    u = size / 100.0
    pts = []
    pts += qbez((50, 5), (72, 4), (90, 18))
    pts += qbez((90, 18), (96, 34), (86, 55))
    pts += qbez((86, 55), (80, 85), (50, 98))
    pts += qbez((50, 98), (20, 85), (14, 55))
    pts += qbez((14, 55), (4, 34), (10, 18))
    pts += qbez((10, 18), (28, 4), (50, 5))
    return [(x * u, y * u) for x, y in pts]


def scaled(points, factor, center=CENTER):
    cx, cy = center
    return [(cx + (x - cx) * factor, cy + (y - cy) * factor) for x, y in points]


def gradient(size, stops, angle=90.0, alpha=255):
    """Linear gradient stops: [(t, (r,g,b)), ...], t in 0..1."""
    w, h = size
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    rad = math.radians(angle)
    t = (xx * math.cos(rad) + yy * math.sin(rad))
    t = (t - t.min()) / max(t.max() - t.min(), 1e-6)
    arr = np.zeros((h, w, 4), dtype=np.uint8)
    for i in range(len(stops) - 1):
        t0, c0 = stops[i]
        t1, c1 = stops[i + 1]
        band = (t >= t0) & (t <= t1)
        local = np.clip((t - t0) / max(t1 - t0, 1e-6), 0, 1)
        for channel in range(3):
            arr[..., channel][band] = (
                c0[channel] + (c1[channel] - c0[channel]) * local[band]
            ).astype(np.uint8)
    arr[..., 3] = alpha
    return Image.fromarray(arr, "RGBA")


def radial_glow(size, color, radius, alpha=180):
    w, h = size
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    cx, cy = w / 2, h / 2
    dist = np.sqrt((xx - cx) ** 2 + (yy - cy) ** 2) / max(radius, 1)
    a = np.clip(1 - dist, 0, 1) ** 2 * alpha
    arr = np.zeros((h, w, 4), dtype=np.uint8)
    arr[..., 0] = color[0]
    arr[..., 1] = color[1]
    arr[..., 2] = color[2]
    arr[..., 3] = a.astype(np.uint8)
    return Image.fromarray(arr, "RGBA")


def rounded_line(draw, points, width, fill, shadow=None):
    if shadow:
        shadow_layer = Image.new("RGBA", draw._image.size, (0, 0, 0, 0))
        sd = ImageDraw.Draw(shadow_layer)
        sd.line(points, fill=shadow, width=width + 20, joint="curve")
        for p in (points[0], points[-1]):
            sd.ellipse(
                [p[0] - width / 2, p[1] - width / 2, p[0] + width / 2, p[1] + width / 2],
                fill=shadow,
            )
        draw._image.alpha_composite(shadow_layer.filter(ImageFilter.GaussianBlur(18)))
    draw.line(points, fill=fill, width=width, joint="curve")
    r = width / 2
    for p in (points[0], points[-1]):
        draw.ellipse([p[0] - r, p[1] - r, p[0] + r, p[1] + r], fill=fill)


def make_mark() -> Image.Image:
    size = (S, S)
    shield = shield_polygon(S)
    inner = scaled(shield, 0.855)

    mask = Image.new("L", size, 0)
    ImageDraw.Draw(mask).polygon(shield, fill=255)

    canvas = Image.new("RGBA", size, (0, 0, 0, 0))

    # Ambient glow behind the mark.
    glow_mask = mask.filter(ImageFilter.GaussianBlur(70))
    glow = Image.new("RGBA", size, (54, 242, 208, 150))
    canvas.alpha_composite(Image.composite(glow, Image.new("RGBA", size), glow_mask.point(lambda v: int(v * 0.55))))

    # Soft floor shadow.
    shadow = Image.new("RGBA", size, (0, 0, 0, 0))
    shadow.paste((0, 0, 0, 170), (0, 70), mask)
    canvas.alpha_composite(shadow.filter(ImageFilter.GaussianBlur(55)))

    # Outer bevel: bright at top-left, violet at the base.
    rim = gradient(size, [
        (0.0, (154, 255, 240)),
        (0.35, (53, 242, 208)),
        (0.7, (35, 160, 220)),
        (1.0, (122, 104, 255)),
    ], angle=110)
    canvas.alpha_composite(Image.composite(rim, Image.new("RGBA", size), mask))

    # Inner face.
    face = gradient(size, [
        (0.0, (18, 72, 108)),
        (0.3, (9, 43, 74)),
        (0.72, (6, 26, 48)),
        (1.0, (10, 16, 38)),
    ], angle=105)
    inner_mask = Image.new("L", size, 0)
    ImageDraw.Draw(inner_mask).polygon(inner, fill=255)
    canvas.alpha_composite(Image.composite(face, Image.new("RGBA", size), inner_mask))

    # Inner edge light.
    edge = Image.new("RGBA", size, (0, 0, 0, 0))
    ed = ImageDraw.Draw(edge)
    ed.line(inner + [inner[0]], fill=(120, 255, 238, 180), width=6, joint="curve")
    canvas.alpha_composite(edge.filter(ImageFilter.GaussianBlur(2)))

    # Gloss: a diagonal pane over the upper half, clipped to the face.
    gloss = Image.new("RGBA", size, (0, 0, 0, 0))
    gd = ImageDraw.Draw(gloss)
    gd.polygon(
        [(S * 0.14, S * 0.02), (S * 0.92, S * 0.02), (S * 0.92, S * 0.40), (S * 0.14, S * 0.66)],
        fill=(255, 255, 255, 34),
    )
    gloss = gloss.filter(ImageFilter.GaussianBlur(22))
    gloss_masked = Image.composite(gloss, Image.new("RGBA", size), inner_mask)
    canvas.alpha_composite(gloss_masked)

    # Embossed check.
    draw = ImageDraw.Draw(canvas)
    check = [(S * 0.33, S * 0.51), (S * 0.455, S * 0.635), (S * 0.70, S * 0.36)]
    rounded_line(draw, check, width=86, fill=(255, 255, 255, 245), shadow=(0, 0, 0, 120))

    # Small sparkle accents.
    for cx, cy, r in ((S * 0.76, S * 0.18, 13), (S * 0.24, S * 0.69, 9), (S * 0.81, S * 0.58, 7)):
        draw.ellipse([cx - r, cy - r, cx + r, cy + r], fill=(190, 255, 248, 220))

    return canvas.resize((1024, 1024), Image.LANCZOS)


def make_foreground(mark: Image.Image, px: int) -> Image.Image:
    canvas = Image.new("RGBA", (px, px), (0, 0, 0, 0))
    inner = int(px * 0.76)
    art = mark.resize((inner, inner), Image.LANCZOS)
    canvas.alpha_composite(art, ((px - inner) // 2, (px - inner) // 2))
    return canvas


def load_font(size: int, bold: bool = True):
    candidates = [
        r"C:\Windows\Fonts\segoeuib.ttf" if bold else r"C:\Windows\Fonts\segoeui.ttf",
        r"C:\Windows\Fonts\arialbd.ttf" if bold else r"C:\Windows\Fonts\arial.ttf",
        "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
        if bold
        else "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
    ]
    for path in candidates:
        if os.path.exists(path):
            return ImageFont.truetype(path, size)
    return ImageFont.load_default()


def make_banner(mark: Image.Image, w: int, h: int) -> Image.Image:
    canvas = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    bg = gradient((w, h), [
        (0.0, (7, 12, 26)),
        (0.55, (13, 25, 49)),
        (1.0, (27, 18, 60)),
    ], angle=100)
    canvas.alpha_composite(bg)

    glow = radial_glow((w, h), (53, 242, 208), w * 0.55, alpha=110)
    canvas.alpha_composite(glow)

    logo_px = int(h * 0.70)
    logo = make_foreground(mark, logo_px)
    canvas.alpha_composite(logo, (int(h * 0.12), (h - logo_px) // 2))

    draw = ImageDraw.Draw(canvas)
    title_font = load_font(int(h * 0.155), bold=True)
    tag_font = load_font(int(h * 0.060), bold=False)
    base_x = int(h * 0.12) + logo_px + int(h * 0.10)
    draw.text((base_x, h * 0.44), "SecureBox", font=title_font, fill=(242, 247, 255, 255), anchor="lm")
    draw.text(
        (base_x, h * 0.64),
        "PRIVATE. LOCAL. YOURS.",
        font=tag_font,
        fill=(120, 230, 220, 235),
        anchor="lm",
    )
    return canvas


def main():
    BRAND.mkdir(parents=True, exist_ok=True)
    RES.mkdir(parents=True, exist_ok=True)

    mark = make_mark()
    mark.save(BRAND / "logo-1024.png")
    mark.resize((512, 512), Image.LANCZOS).save(BRAND / "icon-512.png")
    (CORE_UI_RES / "drawable-nodpi").mkdir(parents=True, exist_ok=True)
    mark.resize((512, 512), Image.LANCZOS).save(CORE_UI_RES / "drawable-nodpi" / "logo_mark.png")

    for density, px in {
        "mdpi": 108,
        "hdpi": 162,
        "xhdpi": 216,
        "xxhdpi": 324,
        "xxxhdpi": 432,
    }.items():
        folder = RES / f"mipmap-{density}"
        folder.mkdir(parents=True, exist_ok=True)
        make_foreground(mark, px).save(folder / "ic_launcher_foreground.png")

    for density, (w, h) in {
        "mdpi": (160, 90),
        "hdpi": (240, 135),
        "xhdpi": (320, 180),
        "xxhdpi": (480, 270),
    }.items():
        folder = RES / f"drawable-{density}"
        folder.mkdir(parents=True, exist_ok=True)
        banner = make_banner(mark, 960, 540).resize((w, h), Image.LANCZOS)
        banner.save(folder / "tv_banner.png")

    (BRAND / "tv-banner.png").parent.mkdir(parents=True, exist_ok=True)
    make_banner(mark, 960, 540).save(BRAND / "tv-banner.png")

    print("Generated logo, adaptive foregrounds, TV banners and store icon.")


if __name__ == "__main__":
    main()
