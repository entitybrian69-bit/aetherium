# Aetherium auto benchmark (1.16.5)

Window 854x480, render distance 8, seed 7294105550031, GPU: Unknown, Java 17.0.20.1, 4 CPUs.

| Scenario | Avg FPS | 1% low FPS | p50 ms | p99 ms | p99.9 ms | Max ms | Frames > 50 ms | Frames > 100 ms |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| Vanilla (Aetherium off) | 28.8 | 19.6 | 34.25 | 45.99 | 56.98 | 57.0 | 2 | 0 |
| Aetherium (defaults) | 30.9 | 22.9 | 32.18 | 41.26 | 47.78 | 47.8 | 0 | 0 |
| Aetherium, all 1.12.0 options off | 30.9 | 21.4 | 31.95 | 41.06 | 55.70 | 55.7 | 1 | 0 |
| Aetherium, directional culling + view-first building off | 31.5 | 22.2 | 31.45 | 41.17 | 54.88 | 54.9 | 1 | 0 |
| Aetherium, GL call trimming + cheap damage tint off | 31.0 | 19.3 | 31.90 | 43.52 | 70.79 | 70.8 | 2 | 0 |
| Aetherium, server lighting + server idle off | 31.1 | 22.1 | 31.90 | 41.15 | 48.80 | 48.8 | 0 | 0 |
| Aetherium, light table rate + AO cache off | 31.4 | 23.4 | 31.69 | 39.52 | 48.34 | 48.3 | 0 | 0 |
| Aetherium (defaults), measured again | 31.1 | 23.2 | 32.08 | 40.76 | 47.99 | 48.0 | 0 | 0 |
| Aetherium + render scale 70% | 34.9 | 28.4 | 28.61 | 34.38 | 36.42 | 36.4 | 0 | 0 |
| Flying into new terrain (+X), Aetherium off | 49.6 | 19.0 | 18.00 | 46.10 | 67.86 | 67.9 | 4 | 0 |
| Flying into new terrain (-X), Aetherium defaults | 37.1 | 19.1 | 25.57 | 46.66 | 54.55 | 54.6 | 6 | 0 |
| Flying into new terrain (+Z), all 1.12.0 options off | 43.9 | 27.3 | 21.61 | 34.49 | 38.61 | 38.6 | 0 | 0 |
| Flying into new terrain (-Z), Aetherium defaults | 48.8 | 21.0 | 19.13 | 42.36 | 52.41 | 52.4 | 2 | 0 |

## Notes

- vanilla: 3 GC collections, 23 ms in GC; renderer: Vanilla; regions: ; render scale: 100%
- aetherium: 3 GC collections, 14 ms in GC; renderer: Aetherium pipeline, compatible mode (91 sections in the last layer, 28% of faces skipped; last frame 264 section layers in 168 draws, vanilla: 264; batched: 142 buffers, 38 MB (31 MB used), last layer 27 buffers / 61 draws); regions: ; batched: 142 buffers, 38 MB (31 MB used), last layer 27 buffers / 61 draws; render scale: 100%
- new-off: 3 GC collections, 10 ms in GC; renderer: Aetherium pipeline, compatible mode (89 sections in the last layer, 28% of faces skipped; last frame 265 section layers in 166 draws, vanilla: 265; batched: 142 buffers, 38 MB (31 MB used), last layer 26 buffers / 60 draws); regions: ; batched: 142 buffers, 38 MB (31 MB used), last layer 26 buffers / 60 draws; render scale: 100%
- no-cull: 3 GC collections, 11 ms in GC; renderer: Aetherium pipeline, compatible mode (91 sections in the last layer, 28% of faces skipped; last frame 264 section layers in 168 draws, vanilla: 264; batched: 142 buffers, 38 MB (31 MB used), last layer 27 buffers / 61 draws); regions: ; batched: 142 buffers, 38 MB (31 MB used), last layer 27 buffers / 61 draws; render scale: 100%
- no-gl: 3 GC collections, 8 ms in GC; renderer: Aetherium pipeline, compatible mode (89 sections in the last layer, 27% of faces skipped; last frame 265 section layers in 166 draws, vanilla: 265; batched: 142 buffers, 38 MB (31 MB used), last layer 26 buffers / 60 draws); regions: ; batched: 142 buffers, 38 MB (31 MB used), last layer 26 buffers / 60 draws; render scale: 100%
- no-server: 3 GC collections, 8 ms in GC; renderer: Aetherium pipeline, compatible mode (89 sections in the last layer, 27% of faces skipped; last frame 265 section layers in 166 draws, vanilla: 265; batched: 142 buffers, 38 MB (31 MB used), last layer 26 buffers / 60 draws); regions: ; batched: 142 buffers, 38 MB (31 MB used), last layer 26 buffers / 60 draws; render scale: 100%
- no-light: 3 GC collections, 14 ms in GC; renderer: Aetherium pipeline, compatible mode (91 sections in the last layer, 28% of faces skipped; last frame 264 section layers in 168 draws, vanilla: 264; batched: 142 buffers, 38 MB (31 MB used), last layer 27 buffers / 61 draws); regions: ; batched: 142 buffers, 38 MB (31 MB used), last layer 27 buffers / 61 draws; render scale: 100%
- aetherium-again: 2 GC collections, 6 ms in GC; renderer: Aetherium pipeline, compatible mode (91 sections in the last layer, 28% of faces skipped; last frame 265 section layers in 168 draws, vanilla: 265; batched: 142 buffers, 38 MB (31 MB used), last layer 27 buffers / 61 draws); regions: ; batched: 142 buffers, 38 MB (31 MB used), last layer 27 buffers / 61 draws; render scale: 100%
- scale70: 2 GC collections, 6 ms in GC; renderer: Aetherium pipeline, compatible mode (90 sections in the last layer, 27% of faces skipped; last frame 265 section layers in 166 draws, vanilla: 265; batched: 142 buffers, 38 MB (31 MB used), last layer 26 buffers / 60 draws); regions: ; batched: 142 buffers, 38 MB (31 MB used), last layer 26 buffers / 60 draws; render scale: 70%
- fly-vanilla: 8 GC collections, 62 ms in GC; renderer: Vanilla; regions: ; render scale: 100%
- fly-aetherium: 13 GC collections, 78 ms in GC; renderer: Aetherium pipeline, compatible mode (67 sections in the last layer, 27% of faces skipped; last frame 214 section layers in 122 draws, vanilla: 214; batched: 132 buffers, 33 MB (26 MB used), last layer 13 buffers / 20 draws); regions: ; batched: 132 buffers, 33 MB (26 MB used), last layer 13 buffers / 20 draws; render scale: 100%
- fly-new-off: 13 GC collections, 61 ms in GC; renderer: Aetherium pipeline, compatible mode (52 sections in the last layer, 28% of faces skipped; last frame 90 section layers in 81 draws, vanilla: 90; batched: 119 buffers, 51 MB (40 MB used), last layer 14 buffers / 55 draws); regions: ; batched: 119 buffers, 51 MB (40 MB used), last layer 14 buffers / 55 draws; render scale: 100%
- fly-aetherium-z: 18 GC collections, 90 ms in GC; renderer: Aetherium pipeline, compatible mode (44 sections in the last layer, 38% of faces skipped; last frame 170 section layers in 162 draws, vanilla: 170; batched: 122 buffers, 46 MB (33 MB used), last layer 7 buffers / 8 draws); regions: ; batched: 122 buffers, 46 MB (33 MB used), last layer 7 buffers / 8 draws; render scale: 100%
