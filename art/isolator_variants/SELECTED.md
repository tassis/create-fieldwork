# Selected texture draft

Owner selected the **gray-rim version** after comparing the wood-rim alternative
(2026-11-07). The selected pair is in [`rim/`](rim/README.md):

- Andesite: latest dark gray rim, sampled Create casing colors `#505351`,
  `#60635F`, `#404543`, sparse `#686C68`.
- Brass: existing bright brass rim, unchanged by the andesite experiments.
- Both: original placeholder inner patterns, four rivets and complete inactive /
  active top textures retained. Only side/bottom outer two-pixel rims differ.

[Comparison](rim/comparison.png) · [Andesite net](rim/andesite/preview.png) ·
[Brass net](rim/brass/preview.png) · [Selected ZIP](rim/isolator_variants.zip)

Unused instrument, casing and wood-rim drafts/previews/ZIPs were removed at the
owner's request on 2026-11-08. Only the selected rim pair is retained here.
Regenerate/check it with `--style rim` (also the exporter default):

```sh
python3 scripts/export-isolator-variants.py --style rim --check
```

The selected pair has been integrated into production resources. This document
records selection, not release or client rendering acceptance; those remain
separate gates. Version/publication were not changed by the artwork cleanup.
