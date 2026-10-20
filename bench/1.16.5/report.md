# Aetherium auto benchmark (1.16.5)

Window 854x480, render distance 8, seed 7294105550031, GPU: Unknown, Java 17.0.20.1, 4 CPUs.

| Scenario | Avg FPS | 1% low FPS | p50 ms | p99 ms | p99.9 ms | Max ms | Frames > 50 ms | Frames > 100 ms |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| Vanilla (Aetherium off) | 22.3 | 15.1 | 44.39 | 59.51 | 75.22 | 75.2 | 51 | 0 |
| Aetherium (defaults) | 24.7 | 17.0 | 39.66 | 56.65 | 61.25 | 61.3 | 18 | 0 |
| Aetherium, all 1.12.0 options off | 24.3 | 16.6 | 40.21 | 56.10 | 66.86 | 66.9 | 24 | 0 |
| Aetherium, directional culling + view-first building off | 25.2 | 20.3 | 39.44 | 47.41 | 53.65 | 53.6 | 1 | 0 |
| Aetherium, GL call trimming + cheap damage tint off | 25.2 | 20.0 | 38.93 | 48.12 | 52.06 | 52.1 | 3 | 0 |
| Aetherium, server lighting + server idle off | 25.2 | 19.2 | 39.26 | 48.90 | 53.51 | 53.5 | 4 | 0 |
| Aetherium, light table rate + AO cache off | 24.9 | 18.5 | 39.72 | 51.46 | 59.04 | 59.0 | 8 | 0 |
| Aetherium (defaults), measured again | 25.6 | 18.8 | 38.42 | 49.29 | 59.49 | 59.5 | 5 | 0 |
| Aetherium + render scale 70% | 28.3 | 19.7 | 34.76 | 46.41 | 54.03 | 54.0 | 3 | 0 |
| Flying into new terrain (+X), Aetherium off | 37.4 | 15.6 | 24.90 | 59.57 | 71.68 | 71.7 | 20 | 0 |
| Flying into new terrain (-X), Aetherium defaults | 27.6 | 14.6 | 34.78 | 63.27 | 72.41 | 72.4 | 36 | 0 |
| Flying into new terrain (+Z), all 1.12.0 options off | 31.8 | 19.3 | 29.85 | 47.79 | 62.21 | 62.2 | 3 | 0 |
| Flying into new terrain (-Z), Aetherium defaults | 36.7 | 17.6 | 25.53 | 54.95 | 63.37 | 63.4 | 14 | 0 |

## Notes

- vanilla: 2 GC collections, 17 ms in GC; renderer: Vanilla; regions: ; render scale: 100%
- aetherium: 3 GC collections, 25 ms in GC; renderer: Aetherium pipeline, compatible mode (97 sections in the last layer, 30% of faces skipped; last frame 303 section layers in 193 draws, vanilla: 303; batched: 163 buffers, 39 MB (33 MB used), last layer 30 buffers / 63 draws); regions: ; batched: 163 buffers, 39 MB (33 MB used), last layer 30 buffers / 63 draws; render scale: 100%
- new-off: 2 GC collections, 24 ms in GC; renderer: Aetherium pipeline, compatible mode (95 sections in the last layer, 30% of faces skipped; last frame 303 section layers in 192 draws, vanilla: 303; batched: 162 buffers, 39 MB (33 MB used), last layer 29 buffers / 62 draws); regions: ; batched: 162 buffers, 39 MB (33 MB used), last layer 29 buffers / 62 draws; render scale: 100%
- no-cull: 2 GC collections, 7 ms in GC; renderer: Aetherium pipeline, compatible mode (95 sections in the last layer, 30% of faces skipped; last frame 303 section layers in 192 draws, vanilla: 303; batched: 162 buffers, 39 MB (33 MB used), last layer 29 buffers / 62 draws); regions: ; batched: 162 buffers, 39 MB (33 MB used), last layer 29 buffers / 62 draws; render scale: 100%
- no-gl: 2 GC collections, 8 ms in GC; renderer: Aetherium pipeline, compatible mode (98 sections in the last layer, 30% of faces skipped; last frame 305 section layers in 192 draws, vanilla: 305; batched: 162 buffers, 39 MB (33 MB used), last layer 30 buffers / 63 draws); regions: ; batched: 162 buffers, 39 MB (33 MB used), last layer 30 buffers / 63 draws; render scale: 100%
- no-server: 2 GC collections, 7 ms in GC; renderer: Aetherium pipeline, compatible mode (96 sections in the last layer, 30% of faces skipped; last frame 303 section layers in 193 draws, vanilla: 303; batched: 163 buffers, 39 MB (33 MB used), last layer 30 buffers / 63 draws); regions: ; batched: 163 buffers, 39 MB (33 MB used), last layer 30 buffers / 63 draws; render scale: 100%
- no-light: 3 GC collections, 12 ms in GC; renderer: Aetherium pipeline, compatible mode (97 sections in the last layer, 30% of faces skipped; last frame 303 section layers in 192 draws, vanilla: 303; batched: 164 buffers, 40 MB (33 MB used), last layer 30 buffers / 63 draws); regions: ; batched: 164 buffers, 40 MB (33 MB used), last layer 30 buffers / 63 draws; render scale: 100%
- aetherium-again: 2 GC collections, 6 ms in GC; renderer: Aetherium pipeline, compatible mode (99 sections in the last layer, 30% of faces skipped; last frame 288 section layers in 183 draws, vanilla: 288; batched: 161 buffers, 39 MB (33 MB used), last layer 29 buffers / 62 draws); regions: ; batched: 161 buffers, 39 MB (33 MB used), last layer 29 buffers / 62 draws; render scale: 100%
- scale70: 2 GC collections, 6 ms in GC; renderer: Aetherium pipeline, compatible mode (97 sections in the last layer, 29% of faces skipped; last frame 303 section layers in 192 draws, vanilla: 303; batched: 163 buffers, 39 MB (33 MB used), last layer 30 buffers / 63 draws); regions: ; batched: 163 buffers, 39 MB (33 MB used), last layer 30 buffers / 63 draws; render scale: 70%
- fly-vanilla: 8 GC collections, 72 ms in GC; renderer: Vanilla; regions: ; render scale: 100%
- fly-aetherium: 13 GC collections, 96 ms in GC; renderer: Aetherium pipeline, compatible mode (75 sections in the last layer, 24% of faces skipped; last frame 225 section layers in 121 draws, vanilla: 225; batched: 157 buffers, 40 MB (26 MB used), last layer 12 buffers / 20 draws); regions: ; batched: 157 buffers, 40 MB (26 MB used), last layer 12 buffers / 20 draws; render scale: 100%
- fly-new-off: 10 GC collections, 73 ms in GC; renderer: Aetherium pipeline, compatible mode (31 sections in the last layer, 28% of faces skipped; last frame 54 section layers in 52 draws, vanilla: 54; batched: 110 buffers, 46 MB (37 MB used), last layer 13 buffers / 33 draws); regions: ; batched: 110 buffers, 46 MB (37 MB used), last layer 13 buffers / 33 draws; render scale: 100%
- fly-aetherium-z: 15 GC collections, 96 ms in GC; renderer: Aetherium pipeline, compatible mode (47 sections in the last layer, 38% of faces skipped; last frame 186 section layers in 185 draws, vanilla: 186; batched: 119 buffers, 44 MB (32 MB used), last layer 7 buffers / 8 draws); regions: ; batched: 119 buffers, 44 MB (32 MB used), last layer 7 buffers / 8 draws; render scale: 100%
