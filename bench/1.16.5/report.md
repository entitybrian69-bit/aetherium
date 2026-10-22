# Aetherium auto benchmark (1.16.5)

Window 854x480, render distance 8, seed 7294105550031, GPU: Unknown, Java 17.0.20.1, 4 CPUs.

| Scenario | Avg FPS | 1% low FPS | p50 ms | p99 ms | p99.9 ms | Max ms | Frames > 50 ms | Frames > 100 ms |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| Vanilla (Aetherium off) | 26.5 | 15.4 | 37.68 | 56.49 | 70.05 | 70.1 | 22 | 0 |
| Aetherium (defaults) | 28.9 | 19.6 | 33.82 | 48.83 | 52.25 | 52.2 | 3 | 0 |
| Aetherium, all 1.12.0 options off | 29.1 | 20.0 | 33.68 | 48.36 | 51.01 | 51.0 | 3 | 0 |
| Aetherium, directional culling + view-first building off | 29.3 | 19.7 | 33.48 | 47.89 | 55.97 | 56.0 | 2 | 0 |
| Aetherium, GL call trimming + cheap damage tint off | 29.3 | 21.0 | 33.45 | 46.65 | 48.74 | 48.7 | 0 | 0 |
| Aetherium, all 1.17.2 options off | 28.6 | 17.8 | 33.38 | 53.68 | 57.55 | 57.6 | 9 | 0 |
| Aetherium + GPU occlusion culling | 29.7 | 21.4 | 33.42 | 43.71 | 48.69 | 48.7 | 0 | 0 |
| Aetherium (defaults), measured again | 29.8 | 22.2 | 33.34 | 44.22 | 45.63 | 45.6 | 0 | 0 |
| Aetherium + render scale 70% | 33.5 | 22.9 | 29.59 | 41.89 | 46.44 | 46.4 | 0 | 0 |
| Flying into new terrain (+X), Aetherium off | 38.8 | 13.9 | 23.10 | 64.69 | 80.45 | 80.5 | 27 | 0 |
| Flying into new terrain (-X), Aetherium defaults | 29.6 | 14.5 | 31.34 | 65.07 | 74.24 | 74.2 | 39 | 0 |
| Flying into new terrain (+Z), all 1.17.2 options off | 33.0 | 15.3 | 27.82 | 55.33 | 88.10 | 88.1 | 17 | 0 |
| Flying into new terrain (-Z), Aetherium defaults | 36.5 | 15.7 | 25.33 | 54.65 | 89.49 | 89.5 | 19 | 0 |

## Notes

- vanilla: 5 GC collections, 13 ms in GC; renderer: Vanilla; regions: ; render scale: 100%; chests baked so far: 16
- aetherium: 4 GC collections, 8 ms in GC; renderer: Aetherium pipeline, compatible mode (68 sections in the last layer, 29% of faces skipped; last frame 209 section layers in 125 draws, vanilla: 209; batched: 123 buffers (123 packed), 19 MB (15 MB used), last layer 22 buffers / 39 draws); regions: ; batched: 123 buffers (123 packed), 19 MB (15 MB used), last layer 22 buffers / 39 draws; render scale: 100%; chests baked so far: 20
- new-off: 6 GC collections, 14 ms in GC; renderer: Aetherium pipeline, compatible mode (65 sections in the last layer, 29% of faces skipped; last frame 208 section layers in 124 draws, vanilla: 208; batched: 123 buffers (123 packed), 19 MB (15 MB used), last layer 22 buffers / 38 draws); regions: ; batched: 123 buffers (123 packed), 19 MB (15 MB used), last layer 22 buffers / 38 draws; render scale: 100%; chests baked so far: 24
- no-cull: 6 GC collections, 17 ms in GC; renderer: Aetherium pipeline, compatible mode (68 sections in the last layer, 29% of faces skipped; last frame 207 section layers in 123 draws, vanilla: 207; batched: 122 buffers (122 packed), 19 MB (15 MB used), last layer 22 buffers / 39 draws); regions: ; batched: 122 buffers (122 packed), 19 MB (15 MB used), last layer 22 buffers / 39 draws; render scale: 100%; chests baked so far: 28
- no-gl: 4 GC collections, 9 ms in GC; renderer: Aetherium pipeline, compatible mode (68 sections in the last layer, 29% of faces skipped; last frame 209 section layers in 125 draws, vanilla: 209; batched: 120 buffers (120 packed), 18 MB (15 MB used), last layer 22 buffers / 39 draws); regions: ; batched: 120 buffers (120 packed), 18 MB (15 MB used), last layer 22 buffers / 39 draws; render scale: 100%; chests baked so far: 32
- boost-off: 4 GC collections, 14 ms in GC; renderer: Aetherium pipeline, compatible mode (67 sections in the last layer, 29% of faces skipped; last frame 208 section layers in 125 draws, vanilla: 208; batched: 122 buffers, 30 MB (23 MB used), last layer 22 buffers / 39 draws); regions: ; batched: 122 buffers, 30 MB (23 MB used), last layer 22 buffers / 39 draws; render scale: 100%; chests baked so far: 32
- occlusion: 4 GC collections, 8 ms in GC; renderer: Aetherium pipeline, compatible mode (68 sections in the last layer, 27% of faces skipped; last frame 195 section layers in 114 draws, vanilla: 195; batched: 123 buffers (123 packed), 19 MB (15 MB used), last layer 22 buffers / 39 draws); regions: ; batched: 123 buffers (123 packed), 19 MB (15 MB used), last layer 22 buffers / 39 draws; render scale: 100%; chests baked so far: 36
- aetherium-again: 4 GC collections, 7 ms in GC; renderer: Aetherium pipeline, compatible mode (65 sections in the last layer, 29% of faces skipped; last frame 208 section layers in 123 draws, vanilla: 208; batched: 122 buffers (122 packed), 19 MB (15 MB used), last layer 22 buffers / 38 draws); regions: ; batched: 122 buffers (122 packed), 19 MB (15 MB used), last layer 22 buffers / 38 draws; render scale: 100%; chests baked so far: 40
- scale70: 5 GC collections, 16 ms in GC; renderer: Aetherium pipeline, compatible mode (65 sections in the last layer, 28% of faces skipped; last frame 208 section layers in 122 draws, vanilla: 208; batched: 122 buffers (122 packed), 19 MB (15 MB used), last layer 22 buffers / 38 draws); regions: ; batched: 122 buffers (122 packed), 19 MB (15 MB used), last layer 22 buffers / 38 draws; render scale: 70%; chests baked so far: 44
- fly-vanilla: 22 GC collections, 142 ms in GC; renderer: Vanilla; regions: ; render scale: 100%; chests baked so far: 44
- fly-aetherium: 16 GC collections, 89 ms in GC; renderer: Aetherium pipeline, compatible mode (70 sections in the last layer, 25% of faces skipped; last frame 217 section layers in 115 draws, vanilla: 217; batched: 153 buffers (153 packed), 24 MB (16 MB used), last layer 10 buffers / 17 draws); regions: ; batched: 153 buffers (153 packed), 24 MB (16 MB used), last layer 10 buffers / 17 draws; render scale: 100%; chests baked so far: 50
- fly-boost-off: 18 GC collections, 106 ms in GC; renderer: Aetherium pipeline, compatible mode (49 sections in the last layer, 28% of faces skipped; last frame 78 section layers in 71 draws, vanilla: 78; batched: 121 buffers, 51 MB (41 MB used), last layer 16 buffers / 50 draws); regions: ; batched: 121 buffers, 51 MB (41 MB used), last layer 16 buffers / 50 draws; render scale: 100%; chests baked so far: 50
- fly-aetherium-z: 15 GC collections, 111 ms in GC; renderer: Aetherium pipeline, compatible mode (47 sections in the last layer, 38% of faces skipped; last frame 193 section layers in 199 draws, vanilla: 193; batched: 123 buffers (123 packed), 27 MB (20 MB used), last layer 7 buffers / 8 draws); regions: ; batched: 123 buffers (123 packed), 27 MB (20 MB used), last layer 7 buffers / 8 draws; render scale: 100%; chests baked so far: 57
