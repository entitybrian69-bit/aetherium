# Aetherium auto benchmark (1.16.5)

Window 854x480, render distance 8, seed 7294105550031, GPU: Unknown, Java 17.0.20.1, 4 CPUs.

| Scenario | Avg FPS | 1% low FPS | p50 ms | p99 ms | p99.9 ms | Max ms | Frames > 50 ms | Frames > 100 ms |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| Vanilla (Aetherium off) | 35.9 | 22.1 | 27.07 | 43.58 | 51.62 | 51.6 | 1 | 0 |
| Aetherium (defaults) | 42.1 | 27.0 | 23.55 | 31.04 | 49.24 | 49.2 | 0 | 0 |
| Aetherium, all 1.12.0 options off | 42.3 | 30.1 | 23.58 | 29.60 | 39.59 | 39.6 | 0 | 0 |
| Aetherium, directional culling + view-first building off | 42.5 | 28.4 | 23.35 | 32.06 | 39.60 | 39.6 | 0 | 0 |
| Aetherium, GL call trimming + cheap damage tint off | 42.8 | 26.3 | 23.14 | 31.35 | 51.28 | 51.3 | 1 | 0 |
| Aetherium, all 1.17.2 options off | 43.6 | 28.0 | 22.89 | 30.34 | 46.37 | 46.4 | 0 | 0 |
| Aetherium + GPU occlusion culling | 43.9 | 33.6 | 22.67 | 27.91 | 33.01 | 33.0 | 0 | 0 |
| Aetherium (defaults), measured again | 42.9 | 29.4 | 23.11 | 30.37 | 42.46 | 42.5 | 0 | 0 |
| Aetherium + render scale 70% | 47.0 | 35.8 | 21.36 | 25.17 | 38.38 | 38.4 | 0 | 0 |
| Flying into new terrain (+X), Aetherium off | 63.0 | 24.8 | 14.14 | 35.08 | 42.99 | 48.9 | 0 | 0 |
| Flying into new terrain (-X), Aetherium defaults | 54.8 | 28.3 | 17.28 | 31.60 | 38.47 | 42.5 | 0 | 0 |
| Flying into new terrain (+Z), all 1.17.2 options off | 63.4 | 30.9 | 14.79 | 28.79 | 37.48 | 40.8 | 0 | 0 |
| Flying into new terrain (-Z), Aetherium defaults | 67.1 | 29.6 | 13.98 | 30.12 | 39.15 | 44.3 | 0 | 0 |

## Notes

- vanilla: 1 GC collections, 6 ms in GC; renderer: Vanilla; regions: ; render scale: 100%; chests baked so far: 12
- aetherium: 1 GC collections, 6 ms in GC; renderer: Aetherium pipeline, compatible mode (99 sections in the last layer, 27% of faces skipped; last frame 285 section layers in 184 draws, vanilla: 285; batched: 143 buffers (143 packed), 24 MB (20 MB used), last layer 28 buffers / 74 draws); regions: ; batched: 143 buffers (143 packed), 24 MB (20 MB used), last layer 28 buffers / 74 draws; render scale: 100%; chests baked so far: 16
- new-off: 1 GC collections, 6 ms in GC; renderer: Aetherium pipeline, compatible mode (99 sections in the last layer, 27% of faces skipped; last frame 285 section layers in 184 draws, vanilla: 285; batched: 143 buffers (143 packed), 24 MB (20 MB used), last layer 28 buffers / 74 draws); regions: ; batched: 143 buffers (143 packed), 24 MB (20 MB used), last layer 28 buffers / 74 draws; render scale: 100%; chests baked so far: 20
- no-cull: 1 GC collections, 6 ms in GC; renderer: Aetherium pipeline, compatible mode (98 sections in the last layer, 27% of faces skipped; last frame 281 section layers in 181 draws, vanilla: 281; batched: 142 buffers (142 packed), 24 MB (20 MB used), last layer 28 buffers / 72 draws); regions: ; batched: 142 buffers (142 packed), 24 MB (20 MB used), last layer 28 buffers / 72 draws; render scale: 100%; chests baked so far: 24
- no-gl: 1 GC collections, 7 ms in GC; renderer: Aetherium pipeline, compatible mode (99 sections in the last layer, 27% of faces skipped; last frame 285 section layers in 185 draws, vanilla: 285; batched: 143 buffers (143 packed), 24 MB (20 MB used), last layer 28 buffers / 74 draws); regions: ; batched: 143 buffers (143 packed), 24 MB (20 MB used), last layer 28 buffers / 74 draws; render scale: 100%; chests baked so far: 28
- boost-off: 1 GC collections, 7 ms in GC; renderer: Aetherium pipeline, compatible mode (97 sections in the last layer, 27% of faces skipped; last frame 280 section layers in 181 draws, vanilla: 280; batched: 142 buffers, 38 MB (32 MB used), last layer 28 buffers / 72 draws); regions: ; batched: 142 buffers, 38 MB (32 MB used), last layer 28 buffers / 72 draws; render scale: 100%; chests baked so far: 28
- occlusion: 1 GC collections, 7 ms in GC; renderer: Aetherium pipeline, compatible mode (89 sections in the last layer, 25% of faces skipped; last frame 247 section layers in 164 draws, vanilla: 247; batched: 142 buffers (142 packed), 24 MB (20 MB used), last layer 28 buffers / 69 draws); regions: ; batched: 142 buffers (142 packed), 24 MB (20 MB used), last layer 28 buffers / 69 draws; render scale: 100%; chests baked so far: 32
- aetherium-again: 1 GC collections, 5 ms in GC; renderer: Aetherium pipeline, compatible mode (98 sections in the last layer, 27% of faces skipped; last frame 283 section layers in 184 draws, vanilla: 283; batched: 143 buffers (143 packed), 24 MB (20 MB used), last layer 28 buffers / 72 draws); regions: ; batched: 143 buffers (143 packed), 24 MB (20 MB used), last layer 28 buffers / 72 draws; render scale: 100%; chests baked so far: 36
- scale70: 1 GC collections, 4 ms in GC; renderer: Aetherium pipeline, compatible mode (99 sections in the last layer, 27% of faces skipped; last frame 287 section layers in 186 draws, vanilla: 287; batched: 143 buffers (143 packed), 24 MB (20 MB used), last layer 28 buffers / 74 draws); regions: ; batched: 143 buffers (143 packed), 24 MB (20 MB used), last layer 28 buffers / 74 draws; render scale: 70%; chests baked so far: 40
- fly-vanilla: 4 GC collections, 33 ms in GC; renderer: Vanilla; regions: ; render scale: 100%; chests baked so far: 40
- fly-aetherium: 4 GC collections, 39 ms in GC; renderer: Aetherium pipeline, compatible mode (78 sections in the last layer, 26% of faces skipped; last frame 242 section layers in 129 draws, vanilla: 242; batched: 134 buffers (134 packed), 21 MB (17 MB used), last layer 15 buffers / 20 draws); regions: ; batched: 134 buffers (134 packed), 21 MB (17 MB used), last layer 15 buffers / 20 draws; render scale: 100%; chests baked so far: 47
- fly-boost-off: 4 GC collections, 35 ms in GC; renderer: Aetherium pipeline, compatible mode (54 sections in the last layer, 27% of faces skipped; last frame 89 section layers in 69 draws, vanilla: 89; batched: 121 buffers, 53 MB (43 MB used), last layer 11 buffers / 52 draws); regions: ; batched: 121 buffers, 53 MB (43 MB used), last layer 11 buffers / 52 draws; render scale: 100%; chests baked so far: 47
- fly-aetherium-z: 4 GC collections, 32 ms in GC; renderer: Aetherium pipeline, compatible mode (41 sections in the last layer, 38% of faces skipped; last frame 153 section layers in 143 draws, vanilla: 153; batched: 126 buffers (126 packed), 30 MB (22 MB used), last layer 7 buffers / 9 draws); regions: ; batched: 126 buffers (126 packed), 30 MB (22 MB used), last layer 7 buffers / 9 draws; render scale: 100%; chests baked so far: 54
