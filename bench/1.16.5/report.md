# Aetherium auto benchmark (1.16.5)

Window 854x480, render distance 8, seed 7294105550031, GPU: Unknown, Java 17.0.20.1, 4 CPUs.

| Scenario | Avg FPS | 1% low FPS | p50 ms | p99 ms | p99.9 ms | Max ms | Frames > 50 ms | Frames > 100 ms |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| Vanilla (Aetherium off) | 19.4 | 15.1 | 51.33 | 62.23 | 72.32 | 72.3 | 251 | 0 |
| Aetherium (defaults) | 21.5 | 15.5 | 46.21 | 60.94 | 65.64 | 65.6 | 100 | 0 |
| Aetherium, all 1.12.0 options off | 21.5 | 17.8 | 46.38 | 55.07 | 58.10 | 58.1 | 94 | 0 |
| Aetherium, directional culling + view-first building off | 21.7 | 16.0 | 45.93 | 58.26 | 65.48 | 65.5 | 89 | 0 |
| Aetherium, GL call trimming + cheap damage tint off | 21.3 | 15.6 | 46.27 | 61.86 | 67.68 | 67.7 | 112 | 0 |
| Aetherium, server lighting + server idle off | 21.9 | 17.3 | 45.35 | 55.85 | 59.61 | 59.6 | 77 | 0 |
| Aetherium, light table rate + AO cache off | 21.4 | 16.7 | 46.27 | 56.87 | 67.72 | 67.7 | 91 | 0 |
| Aetherium (defaults), measured again | 21.9 | 17.5 | 45.38 | 56.29 | 57.73 | 57.7 | 69 | 0 |
| Aetherium + render scale 70% | 24.5 | 19.3 | 40.64 | 50.30 | 53.98 | 54.0 | 6 | 0 |
| Flying into new terrain (+X), Aetherium off | 36.1 | 16.2 | 25.97 | 56.08 | 73.34 | 73.3 | 16 | 0 |
| Flying into new terrain (-X), Aetherium defaults | 25.1 | 14.9 | 37.65 | 63.84 | 70.63 | 70.6 | 77 | 0 |
| Flying into new terrain (+Z), all 1.12.0 options off | 28.6 | 16.3 | 33.78 | 57.10 | 68.52 | 68.5 | 24 | 0 |
| Flying into new terrain (-Z), Aetherium defaults | 32.0 | 15.7 | 29.43 | 60.17 | 68.40 | 68.4 | 25 | 0 |

## Notes

- vanilla: 3 GC collections, 16 ms in GC; renderer: Vanilla; regions: ; render scale: 100%
- aetherium: 4 GC collections, 20 ms in GC; renderer: Aetherium pipeline, compatible mode (107 sections in the last layer, 29% of faces skipped; last frame 327 section layers in 215 draws, vanilla: 327; batched: 168 buffers, 41 MB (33 MB used), last layer 35 buffers / 73 draws); regions: ; batched: 168 buffers, 41 MB (33 MB used), last layer 35 buffers / 73 draws; render scale: 100%
- new-off: 4 GC collections, 18 ms in GC; renderer: Aetherium pipeline, compatible mode (108 sections in the last layer, 29% of faces skipped; last frame 327 section layers in 212 draws, vanilla: 327; batched: 168 buffers, 41 MB (33 MB used), last layer 34 buffers / 73 draws); regions: ; batched: 168 buffers, 41 MB (33 MB used), last layer 34 buffers / 73 draws; render scale: 100%
- no-cull: 4 GC collections, 19 ms in GC; renderer: Aetherium pipeline, compatible mode (106 sections in the last layer, 29% of faces skipped; last frame 326 section layers in 213 draws, vanilla: 326; batched: 167 buffers, 41 MB (33 MB used), last layer 34 buffers / 72 draws); regions: ; batched: 167 buffers, 41 MB (33 MB used), last layer 34 buffers / 72 draws; render scale: 100%
- no-gl: 3 GC collections, 11 ms in GC; renderer: Aetherium pipeline, compatible mode (107 sections in the last layer, 29% of faces skipped; last frame 324 section layers in 210 draws, vanilla: 324; batched: 167 buffers, 41 MB (33 MB used), last layer 35 buffers / 73 draws); regions: ; batched: 167 buffers, 41 MB (33 MB used), last layer 35 buffers / 73 draws; render scale: 100%
- no-server: 3 GC collections, 10 ms in GC; renderer: Aetherium pipeline, compatible mode (107 sections in the last layer, 29% of faces skipped; last frame 327 section layers in 213 draws, vanilla: 327; batched: 167 buffers, 41 MB (33 MB used), last layer 35 buffers / 73 draws); regions: ; batched: 167 buffers, 41 MB (33 MB used), last layer 35 buffers / 73 draws; render scale: 100%
- no-light: 3 GC collections, 22 ms in GC; renderer: Aetherium pipeline, compatible mode (107 sections in the last layer, 29% of faces skipped; last frame 326 section layers in 212 draws, vanilla: 326; batched: 166 buffers, 41 MB (33 MB used), last layer 35 buffers / 73 draws); regions: ; batched: 166 buffers, 41 MB (33 MB used), last layer 35 buffers / 73 draws; render scale: 100%
- aetherium-again: 3 GC collections, 12 ms in GC; renderer: Aetherium pipeline, compatible mode (106 sections in the last layer, 29% of faces skipped; last frame 324 section layers in 210 draws, vanilla: 324; batched: 167 buffers, 41 MB (33 MB used), last layer 34 buffers / 72 draws); regions: ; batched: 167 buffers, 41 MB (33 MB used), last layer 34 buffers / 72 draws; render scale: 100%
- scale70: 3 GC collections, 15 ms in GC; renderer: Aetherium pipeline, compatible mode (108 sections in the last layer, 29% of faces skipped; last frame 326 section layers in 204 draws, vanilla: 326; batched: 168 buffers, 41 MB (33 MB used), last layer 34 buffers / 73 draws); regions: ; batched: 168 buffers, 41 MB (33 MB used), last layer 34 buffers / 73 draws; render scale: 70%
- fly-vanilla: 10 GC collections, 81 ms in GC; renderer: Vanilla; regions: ; render scale: 100%
- fly-aetherium: 14 GC collections, 93 ms in GC; renderer: Aetherium pipeline, compatible mode (73 sections in the last layer, 26% of faces skipped; last frame 219 section layers in 112 draws, vanilla: 219; batched: 152 buffers, 39 MB (26 MB used), last layer 11 buffers / 16 draws); regions: ; batched: 152 buffers, 39 MB (26 MB used), last layer 11 buffers / 16 draws; render scale: 100%
- fly-new-off: 15 GC collections, 88 ms in GC; renderer: Aetherium pipeline, compatible mode (40 sections in the last layer, 28% of faces skipped; last frame 64 section layers in 54 draws, vanilla: 64; batched: 109 buffers, 46 MB (36 MB used), last layer 15 buffers / 39 draws); regions: ; batched: 109 buffers, 46 MB (36 MB used), last layer 15 buffers / 39 draws; render scale: 100%
- fly-aetherium-z: 29 GC collections, 128 ms in GC; renderer: Aetherium pipeline, compatible mode (35 sections in the last layer, 35% of faces skipped; last frame 141 section layers in 143 draws, vanilla: 141; batched: 120 buffers, 44 MB (31 MB used), last layer 5 buffers / 6 draws); regions: ; batched: 120 buffers, 44 MB (31 MB used), last layer 5 buffers / 6 draws; render scale: 100%
