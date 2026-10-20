# Aetherium auto benchmark (1.16.5)

Window 854x480, render distance 8, seed 7294105550031, GPU: Unknown, Java 17.0.20.1, 4 CPUs.

| Scenario | Avg FPS | 1% low FPS | p50 ms | p99 ms | p99.9 ms | Max ms | Frames > 50 ms | Frames > 100 ms |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| Vanilla (Aetherium off) | 34.4 | 24.1 | 29.35 | 39.63 | 42.82 | 42.8 | 0 | 0 |
| Aetherium (defaults) | 37.8 | 23.4 | 26.12 | 37.81 | 48.14 | 48.1 | 0 | 0 |
| Aetherium, all 1.12.0 options off | 38.2 | 25.2 | 25.67 | 36.87 | 44.52 | 44.5 | 0 | 0 |
| Aetherium, directional culling + view-first building off | 38.7 | 25.8 | 25.45 | 36.39 | 41.92 | 41.9 | 0 | 0 |
| Aetherium, GL call trimming + cheap damage tint off | 38.7 | 23.6 | 25.45 | 36.17 | 48.05 | 48.1 | 0 | 0 |
| Aetherium, server lighting + server idle off | 38.7 | 24.8 | 25.23 | 35.69 | 47.93 | 47.9 | 0 | 0 |
| Aetherium, light table rate + AO cache off | 39.8 | 26.6 | 24.67 | 33.27 | 41.46 | 41.5 | 0 | 0 |
| Aetherium (defaults), measured again | 39.9 | 25.7 | 24.82 | 35.52 | 41.56 | 41.6 | 0 | 0 |
| Aetherium + render scale 70% | 45.4 | 22.2 | 21.64 | 36.40 | 63.01 | 63.0 | 2 | 0 |
| Flying into new terrain (+X), Aetherium off | 59.2 | 25.1 | 15.01 | 34.86 | 43.24 | 44.5 | 0 | 0 |
| Flying into new terrain (-X), Aetherium defaults | 49.0 | 23.8 | 19.32 | 35.67 | 48.92 | 48.9 | 0 | 0 |
| Flying into new terrain (+Z), all 1.12.0 options off | 52.5 | 30.1 | 18.00 | 30.91 | 34.77 | 38.3 | 0 | 0 |
| Flying into new terrain (-Z), Aetherium defaults | 60.8 | 28.2 | 15.41 | 31.30 | 39.18 | 43.5 | 0 | 0 |

## Notes

- vanilla: 3 GC collections, 17 ms in GC; renderer: Vanilla; regions: ; render scale: 100%
- aetherium: 3 GC collections, 20 ms in GC; renderer: Aetherium pipeline, compatible mode (90 sections in the last layer, 29% of faces skipped; last frame 272 section layers in 174 draws, vanilla: 272; batched: 167 buffers, 41 MB (33 MB used), last layer 31 buffers / 58 draws); regions: ; batched: 167 buffers, 41 MB (33 MB used), last layer 31 buffers / 58 draws; render scale: 100%
- new-off: 3 GC collections, 16 ms in GC; renderer: Aetherium pipeline, compatible mode (88 sections in the last layer, 29% of faces skipped; last frame 274 section layers in 171 draws, vanilla: 274; batched: 167 buffers, 41 MB (33 MB used), last layer 30 buffers / 56 draws); regions: ; batched: 167 buffers, 41 MB (33 MB used), last layer 30 buffers / 56 draws; render scale: 100%
- no-cull: 2 GC collections, 7 ms in GC; renderer: Aetherium pipeline, compatible mode (88 sections in the last layer, 29% of faces skipped; last frame 277 section layers in 173 draws, vanilla: 277; batched: 167 buffers, 41 MB (33 MB used), last layer 30 buffers / 56 draws); regions: ; batched: 167 buffers, 41 MB (33 MB used), last layer 30 buffers / 56 draws; render scale: 100%
- no-gl: 3 GC collections, 10 ms in GC; renderer: Aetherium pipeline, compatible mode (89 sections in the last layer, 29% of faces skipped; last frame 272 section layers in 173 draws, vanilla: 272; batched: 167 buffers, 41 MB (33 MB used), last layer 31 buffers / 57 draws); regions: ; batched: 167 buffers, 41 MB (33 MB used), last layer 31 buffers / 57 draws; render scale: 100%
- no-server: 2 GC collections, 5 ms in GC; renderer: Aetherium pipeline, compatible mode (88 sections in the last layer, 29% of faces skipped; last frame 277 section layers in 173 draws, vanilla: 277; batched: 166 buffers, 41 MB (33 MB used), last layer 30 buffers / 56 draws); regions: ; batched: 166 buffers, 41 MB (33 MB used), last layer 30 buffers / 56 draws; render scale: 100%
- no-light: 3 GC collections, 11 ms in GC; renderer: Aetherium pipeline, compatible mode (88 sections in the last layer, 29% of faces skipped; last frame 274 section layers in 172 draws, vanilla: 274; batched: 166 buffers, 41 MB (33 MB used), last layer 30 buffers / 56 draws); regions: ; batched: 166 buffers, 41 MB (33 MB used), last layer 30 buffers / 56 draws; render scale: 100%
- aetherium-again: 3 GC collections, 7 ms in GC; renderer: Aetherium pipeline, compatible mode (90 sections in the last layer, 29% of faces skipped; last frame 272 section layers in 174 draws, vanilla: 272; batched: 167 buffers, 41 MB (33 MB used), last layer 31 buffers / 58 draws); regions: ; batched: 167 buffers, 41 MB (33 MB used), last layer 31 buffers / 58 draws; render scale: 100%
- scale70: 2 GC collections, 6 ms in GC; renderer: Aetherium pipeline, compatible mode (90 sections in the last layer, 29% of faces skipped; last frame 272 section layers in 175 draws, vanilla: 272; batched: 166 buffers, 40 MB (33 MB used), last layer 31 buffers / 58 draws); regions: ; batched: 166 buffers, 40 MB (33 MB used), last layer 31 buffers / 58 draws; render scale: 70%
- fly-vanilla: 10 GC collections, 65 ms in GC; renderer: Vanilla; regions: ; render scale: 100%
- fly-aetherium: 13 GC collections, 67 ms in GC; renderer: Aetherium pipeline, compatible mode (79 sections in the last layer, 27% of faces skipped; last frame 236 section layers in 129 draws, vanilla: 236; batched: 159 buffers, 42 MB (27 MB used), last layer 13 buffers / 21 draws); regions: ; batched: 159 buffers, 42 MB (27 MB used), last layer 13 buffers / 21 draws; render scale: 100%
- fly-new-off: 12 GC collections, 59 ms in GC; renderer: Aetherium pipeline, compatible mode (52 sections in the last layer, 27% of faces skipped; last frame 91 section layers in 84 draws, vanilla: 91; batched: 124 buffers, 54 MB (44 MB used), last layer 16 buffers / 56 draws); regions: ; batched: 124 buffers, 54 MB (44 MB used), last layer 16 buffers / 56 draws; render scale: 100%
- fly-aetherium-z: 19 GC collections, 85 ms in GC; renderer: Aetherium pipeline, compatible mode (49 sections in the last layer, 40% of faces skipped; last frame 207 section layers in 204 draws, vanilla: 207; batched: 130 buffers, 49 MB (38 MB used), last layer 7 buffers / 9 draws); regions: ; batched: 130 buffers, 49 MB (38 MB used), last layer 7 buffers / 9 draws; render scale: 100%
