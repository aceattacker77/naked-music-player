#!/usr/bin/env bash
# Rewrites the PNGs in docs/play-store/ as opaque 24-bit RGB, which is what the Play Console accepts (no alpha channel).
set -euo pipefail

cd "$(dirname "$0")/.."
jshell="${JAVA_HOME:?set JAVA_HOME to a JDK 17}/bin/jshell"
for f in docs/play-store/*.png; do
  "$jshell" -s - <<JSH
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
var src = ImageIO.read(new File("$f"));
var rgb = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);
var g = rgb.createGraphics();
g.drawImage(src, 0, 0, null);
g.dispose();
ImageIO.write(rgb, "png", new File("$f"));
/exit
JSH
  echo "flattened $f"
done
