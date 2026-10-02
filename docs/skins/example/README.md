# Example skin: Neon

`skin.json` here is a complete skin (dark, cyan accent, large wavy controls with glow). To turn it into an
importable `.mskin`:

**macOS / Linux**

```bash
cd docs/skins/example
zip ../neon.mskin skin.json
```

**Windows (PowerShell)**

```powershell
cd docs\skins\example
Compress-Archive -Path skin.json -DestinationPath ..\neon.zip
Rename-Item ..\neon.zip neon.mskin
```

`skin.json` must sit at the **top level** of the archive (not inside a folder). If your skin uses a font or
image, add the files with the same relative paths the JSON refers to, e.g. `fonts/Orbitron.ttf`:

```bash
zip -r ../neon.mskin skin.json fonts images
```

Copy `neon.mskin` to the phone and import it from **Settings → Skins → Import skin**. See
[`../FORMAT.md`](../FORMAT.md) for every field.
