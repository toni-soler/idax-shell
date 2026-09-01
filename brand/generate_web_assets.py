from pathlib import Path
import io
import sys

import cairosvg
from PIL import Image


def render(svg: Path, output: Path, size: int) -> Image.Image:
    data = cairosvg.svg2png(url=str(svg), output_width=size, output_height=size)
    image = Image.open(io.BytesIO(data)).convert("RGBA")
    output.parent.mkdir(parents=True, exist_ok=True)
    image.save(output, optimize=True)
    return image


def generate(product_dir: Path) -> None:
    mark = product_dir / "logo-mark.svg"
    generated = product_dir / "generated"
    for size in (16, 32, 48, 64, 128, 180, 192, 512, 1024):
        render(mark, generated / f"icon-{size}.png", size)
    base = Image.open(generated / "icon-512.png")
    base.save(generated / "favicon.ico", sizes=[(16, 16), (32, 32), (48, 48)])
    (generated / "favicon.svg").write_bytes(mark.read_bytes())
    (generated / "manifest.webmanifest").write_text(
        '{\n  "name": "' + product_dir.name + '",\n  "icons": [\n'
        '    {"src":"icon-192.png","sizes":"192x192","type":"image/png"},\n'
        '    {"src":"icon-512.png","sizes":"512x512","type":"image/png"}\n  ]\n}\n',
        encoding="utf-8",
    )


if __name__ == "__main__":
    for name in sys.argv[1:] or ["idax"]:
        generate(Path(__file__).parent / name)
