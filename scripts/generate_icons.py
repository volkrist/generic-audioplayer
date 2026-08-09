"""Regenerate launcher / playstore / notification icons from the master PNG."""
from __future__ import annotations

from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / (
    "assets/c__Users_Volkr_AppData_Roaming_Cursor_User_workspaceStorage_"
    "d5f9dd62f69f311ed593fb000eef8067_images______________-"
    "e6eb7c6c-91a4-4596-894e-1ac08b591a52.png"
)
RES = ROOT / "app/src/main/res"

LAUNCHER = {
    "mdpi": 48,
    "hdpi": 72,
    "xhdpi": 96,
    "xxhdpi": 144,
    "xxxhdpi": 192,
}
FOREGROUND = {
    "mdpi": 108,
    "hdpi": 162,
    "xhdpi": 216,
    "xxhdpi": 324,
    "xxxhdpi": 432,
}
NOTIFICATION = {
    "mdpi": 24,
    "hdpi": 36,
    "xhdpi": 48,
    "xxhdpi": 72,
    "xxxhdpi": 96,
}


def resize(img: Image.Image, size: int) -> Image.Image:
    return img.resize((size, size), Image.Resampling.LANCZOS)


def save_png(img: Image.Image, path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    img.save(path, format="PNG", optimize=True)


def main() -> None:
    if not SOURCE.exists():
        raise SystemExit(f"Source icon missing: {SOURCE}")

    master = Image.open(SOURCE).convert("RGBA")
    if master.size != (1024, 1024):
        master = resize(master, 1024)

    branding = ROOT / "branding/app_icon.png"
    save_png(master, branding)

    playstore = resize(master, 512)
    save_png(playstore, ROOT / "app/src/main/ic_launcher-playstore.png")
    save_png(playstore, ROOT / "app/src/main/res/ic_launcher-playstore.png")

    splash = master.convert("RGB")
    splash_path = RES / "drawable-nodpi/app_icon_full.jpg"
    splash_path.parent.mkdir(parents=True, exist_ok=True)
    splash.save(splash_path, format="JPEG", quality=92, optimize=True)

    for density, size in LAUNCHER.items():
        folder = RES / f"mipmap-{density}"
        icon = resize(master, size)
        save_png(icon, folder / "ic_launcher.png")
        save_png(icon, folder / "ic_launcher_round.png")

    for density, size in FOREGROUND.items():
        folder = RES / f"mipmap-{density}"
        save_png(resize(master, size), folder / "ic_launcher_foreground.png")

    for density, size in NOTIFICATION.items():
        folder = RES / f"mipmap-{density}"
        save_png(resize(master, size), folder / "ic_notification.png")

    print("Generated icons from", SOURCE.name)
    print("  branding/app_icon.png")
    print("  app/src/main/ic_launcher-playstore.png")
    print("  app/src/main/res/ic_launcher-playstore.png")
    print("  app/src/main/res/drawable-nodpi/app_icon_full.jpg")
    print("  mipmap-*/ic_launcher*.png, ic_notification.png")


if __name__ == "__main__":
    main()
