# Aetherium auto benchmark (1.16.5)

Window 854x480, render distance 8, seed 7294105550031, GPU: Unknown, Java 17.0.20.1, 4 CPUs.

| Scenario | Avg FPS | 1% low FPS | p50 ms | p99 ms | p99.9 ms | Max ms | Frames > 50 ms | Frames > 100 ms |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| Vanilla (Aetherium off) | 31.5 | 19.6 | 31.03 | 49.04 | 56.27 | 56.3 | 3 | 0 |
| Aetherium (defaults) | 35.8 | 21.7 | 27.58 | 42.83 | 49.15 | 49.1 | 0 | 0 |
| Aetherium, all 1.12.0 options off | 35.5 | 22.4 | 27.82 | 36.32 | 60.20 | 60.2 | 2 | 0 |
| Aetherium, directional culling + view-first building off | 35.8 | 23.3 | 27.63 | 35.94 | 51.73 | 51.7 | 2 | 0 |
| Aetherium, GL call trimming + cheap damage tint off | 34.1 | 20.9 | 28.64 | 44.69 | 53.69 | 53.7 | 1 | 0 |
| Aetherium, all 1.17.2 options off | 33.8 | 23.4 | 29.12 | 41.74 | 44.64 | 44.6 | 0 | 0 |
| Aetherium + GPU occlusion culling | 36.9 | 24.9 | 26.83 | 35.62 | 46.35 | 46.3 | 0 | 0 |
| Aetherium (defaults), measured again | 37.1 | 25.5 | 26.83 | 34.58 | 45.69 | 45.7 | 0 | 0 |
| Aetherium + render scale 70% | 40.5 | 22.1 | 24.19 | 34.77 | 66.41 | 66.4 | 2 | 0 |
| Flying into new terrain (+X), Aetherium off | 53.3 | 18.7 | 16.40 | 45.50 | 57.46 | 68.1 | 7 | 0 |
| Flying into new terrain (-X), Aetherium defaults | 43.2 | 23.4 | 21.59 | 39.22 | 50.91 | 50.9 | 1 | 0 |
| Flying into new terrain (+Z), all 1.17.2 options off | 51.4 | 26.9 | 17.98 | 33.54 | 39.17 | 42.8 | 0 | 0 |
| Flying into new terrain (-Z), Aetherium defaults | 53.1 | 21.6 | 17.44 | 41.43 | 47.22 | 53.1 | 1 | 0 |

## Notes

- vanilla: 1 GC collections, 7 ms in GC; renderer: Vanilla; regions: ; render scale: 100%; chests baked so far: 16
- aetherium: 1 GC collections, 7 ms in GC; renderer: Aetherium pipeline, compatible mode (97 sections in the last layer, 29% of faces skipped; last frame 289 section layers in 181 draws, vanilla: 289; batched: 166 buffers (166 packed), 25 MB (20 MB used), last layer 30 buffers / 62 draws); regions: ; batched: 166 buffers (166 packed), 25 MB (20 MB used), last layer 30 buffers / 62 draws; render scale: 100%; chests baked so far: 20
- new-off: 1 GC collections, 6 ms in GC; renderer: Aetherium pipeline, compatible mode (98 sections in the last layer, 29% of faces skipped; last frame 293 section layers in 184 draws, vanilla: 293; batched: 166 buffers (166 packed), 25 MB (20 MB used), last layer 31 buffers / 61 draws); regions: ; batched: 166 buffers (166 packed), 25 MB (20 MB used), last layer 31 buffers / 61 draws; render scale: 100%; chests baked so far: 24
- no-cull: 1 GC collections, 7 ms in GC; renderer: Aetherium pipeline, compatible mode (98 sections in the last layer, 29% of faces skipped; last frame 292 section layers in 182 draws, vanilla: 292; batched: 166 buffers (166 packed), 26 MB (20 MB used), last layer 31 buffers / 61 draws); regions: ; batched: 166 buffers (166 packed), 26 MB (20 MB used), last layer 31 buffers / 61 draws; render scale: 100%; chests baked so far: 28
- no-gl: 1 GC collections, 7 ms in GC; renderer: Aetherium pipeline, compatible mode (97 sections in the last layer, 29% of faces skipped; last frame 289 section layers in 183 draws, vanilla: 289; batched: 165 buffers (165 packed), 25 MB (20 MB used), last layer 30 buffers / 60 draws); regions: ; batched: 165 buffers (165 packed), 25 MB (20 MB used), last layer 30 buffers / 60 draws; render scale: 100%; chests baked so far: 32
- boost-off: 2 GC collections, 12 ms in GC; renderer: Aetherium pipeline, compatible mode (96 sections in the last layer, 29% of faces skipped; last frame 287 section layers in 179 draws, vanilla: 287; batched: 164 buffers, 40 MB (32 MB used), last layer 30 buffers / 61 draws); regions: ; batched: 164 buffers, 40 MB (32 MB used), last layer 30 buffers / 61 draws; render scale: 100%; chests baked so far: 32
- occlusion: 2 GC collections, 7 ms in GC; renderer: Aetherium pipeline, compatible mode (87 sections in the last layer, 25% of faces skipped; last frame 240 section layers in 149 draws, vanilla: 240; batched: 166 buffers (166 packed), 25 MB (20 MB used), last layer 28 buffers / 57 draws); regions: ; batched: 166 buffers (166 packed), 25 MB (20 MB used), last layer 28 buffers / 57 draws; render scale: 100%; chests baked so far: 36
- aetherium-again: 2 GC collections, 7 ms in GC; renderer: Aetherium pipeline, compatible mode (98 sections in the last layer, 29% of faces skipped; last frame 294 section layers in 184 draws, vanilla: 294; batched: 165 buffers (165 packed), 25 MB (20 MB used), last layer 31 buffers / 62 draws); regions: ; batched: 165 buffers (165 packed), 25 MB (20 MB used), last layer 31 buffers / 62 draws; render scale: 100%; chests baked so far: 40
- scale70: 2 GC collections, 8 ms in GC; renderer: Aetherium pipeline, compatible mode (98 sections in the last layer, 29% of faces skipped; last frame 293 section layers in 184 draws, vanilla: 293; batched: 166 buffers (166 packed), 25 MB (20 MB used), last layer 31 buffers / 62 draws); regions: ; batched: 166 buffers (166 packed), 25 MB (20 MB used), last layer 31 buffers / 62 draws; render scale: 70%; chests baked so far: 44
- fly-vanilla: 5 GC collections, 39 ms in GC; renderer: Vanilla; regions: ; render scale: 100%; chests baked so far: 44
- fly-aetherium: 5 GC collections, 47 ms in GC; renderer: Aetherium pipeline, compatible mode (81 sections in the last layer, 27% of faces skipped; last frame 259 section layers in 137 draws, vanilla: 259; batched: 164 buffers (164 packed), 28 MB (18 MB used), last layer 16 buffers / 21 draws); regions: ; batched: 164 buffers (164 packed), 28 MB (18 MB used), last layer 16 buffers / 21 draws; render scale: 100%; chests baked so far: 51
- fly-boost-off: 5 GC collections, 46 ms in GC; renderer: Aetherium pipeline, compatible mode (39 sections in the last layer, 28% of faces skipped; last frame 59 section layers in 55 draws, vanilla: 59; batched: 118 buffers, 50 MB (41 MB used), last layer 14 buffers / 41 draws); regions: ; batched: 118 buffers, 50 MB (41 MB used), last layer 14 buffers / 41 draws; render scale: 100%; chests baked so far: 51
- fly-aetherium-z: 6 GC collections, 52 ms in GC; renderer: Aetherium pipeline, compatible mode (40 sections in the last layer, 40% of faces skipped; last frame 156 section layers in 160 draws, vanilla: 156; batched: 124 buffers (124 packed), 29 MB (21 MB used), last layer 7 buffers / 7 draws); regions: ; batched: 124 buffers (124 packed), 29 MB (21 MB used), last layer 7 buffers / 7 draws; render scale: 100%; chests baked so far: 58
