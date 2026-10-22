# Aetherium auto benchmark (1.16.5)

Window 854x480, render distance 8, seed 7294105550031, GPU: Unknown, Java 17.0.20.1, 4 CPUs.

| Scenario | Avg FPS | 1% low FPS | p50 ms | p99 ms | p99.9 ms | Max ms | Frames > 50 ms | Frames > 100 ms |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| Vanilla (Aetherium off) | 33.7 | 22.8 | 29.37 | 39.69 | 52.70 | 52.7 | 1 | 0 |
| Aetherium (defaults) | 38.4 | 23.4 | 25.76 | 37.97 | 53.36 | 53.4 | 1 | 0 |
| Aetherium, all 1.12.0 options off | 38.5 | 24.5 | 25.63 | 38.23 | 42.99 | 43.0 | 0 | 0 |
| Aetherium, directional culling + view-first building off | 40.0 | 26.1 | 24.73 | 34.50 | 46.19 | 46.2 | 0 | 0 |
| Aetherium, GL call trimming + cheap damage tint off | 40.0 | 28.9 | 24.81 | 31.75 | 38.06 | 38.1 | 0 | 0 |
| Aetherium, all 1.17.2 options off | 39.2 | 25.0 | 25.25 | 35.86 | 46.80 | 46.8 | 0 | 0 |
| Aetherium + GPU occlusion culling | 39.8 | 29.3 | 24.98 | 31.90 | 38.96 | 39.0 | 0 | 0 |
| Aetherium (defaults), measured again | 38.6 | 27.0 | 25.78 | 31.23 | 48.97 | 49.0 | 0 | 0 |
| Aetherium + render scale 70% | 41.7 | 27.1 | 23.60 | 34.70 | 41.78 | 41.8 | 0 | 0 |
| Flying into new terrain (+X), Aetherium off | 56.6 | 21.0 | 15.87 | 40.46 | 54.92 | 67.5 | 4 | 0 |
| Flying into new terrain (-X), Aetherium defaults | 47.0 | 24.2 | 19.71 | 37.64 | 51.45 | 51.5 | 1 | 0 |
| Flying into new terrain (+Z), all 1.17.2 options off | 55.2 | 32.0 | 17.01 | 29.54 | 32.14 | 34.0 | 0 | 0 |
| Flying into new terrain (-Z), Aetherium defaults | 55.5 | 25.1 | 16.87 | 33.55 | 49.29 | 55.0 | 1 | 0 |

## Notes

- vanilla: 5 GC collections, 23 ms in GC; renderer: Vanilla; regions: ; render scale: 100%; chests baked so far: 16
- aetherium: 6 GC collections, 22 ms in GC; renderer: Aetherium pipeline, compatible mode (109 sections in the last layer, 27% of faces skipped; last frame 317 section layers in 197 draws, vanilla: 317; batched: 142 buffers (142 packed), 23 MB (20 MB used), last layer 29 buffers / 74 draws); regions: ; batched: 142 buffers (142 packed), 23 MB (20 MB used), last layer 29 buffers / 74 draws; render scale: 100%; chests baked so far: 20
- new-off: 4 GC collections, 12 ms in GC; renderer: Aetherium pipeline, compatible mode (109 sections in the last layer, 27% of faces skipped; last frame 317 section layers in 194 draws, vanilla: 317; batched: 142 buffers (142 packed), 23 MB (20 MB used), last layer 29 buffers / 74 draws); regions: ; batched: 142 buffers (142 packed), 23 MB (20 MB used), last layer 29 buffers / 74 draws; render scale: 100%; chests baked so far: 24
- no-cull: 4 GC collections, 12 ms in GC; renderer: Aetherium pipeline, compatible mode (109 sections in the last layer, 27% of faces skipped; last frame 317 section layers in 192 draws, vanilla: 317; batched: 142 buffers (142 packed), 23 MB (20 MB used), last layer 29 buffers / 74 draws); regions: ; batched: 142 buffers (142 packed), 23 MB (20 MB used), last layer 29 buffers / 74 draws; render scale: 100%; chests baked so far: 28
- no-gl: 4 GC collections, 16 ms in GC; renderer: Aetherium pipeline, compatible mode (111 sections in the last layer, 27% of faces skipped; last frame 317 section layers in 193 draws, vanilla: 317; batched: 142 buffers (142 packed), 23 MB (20 MB used), last layer 30 buffers / 75 draws); regions: ; batched: 142 buffers (142 packed), 23 MB (20 MB used), last layer 30 buffers / 75 draws; render scale: 100%; chests baked so far: 32
- boost-off: 5 GC collections, 22 ms in GC; renderer: Aetherium pipeline, compatible mode (108 sections in the last layer, 27% of faces skipped; last frame 315 section layers in 193 draws, vanilla: 315; batched: 142 buffers, 38 MB (32 MB used), last layer 29 buffers / 74 draws); regions: ; batched: 142 buffers, 38 MB (32 MB used), last layer 29 buffers / 74 draws; render scale: 100%; chests baked so far: 32
- occlusion: 4 GC collections, 11 ms in GC; renderer: Aetherium pipeline, compatible mode (99 sections in the last layer, 25% of faces skipped; last frame 278 section layers in 171 draws, vanilla: 278; batched: 142 buffers (142 packed), 23 MB (20 MB used), last layer 30 buffers / 70 draws); regions: ; batched: 142 buffers (142 packed), 23 MB (20 MB used), last layer 30 buffers / 70 draws; render scale: 100%; chests baked so far: 36
- aetherium-again: 4 GC collections, 16 ms in GC; renderer: Aetherium pipeline, compatible mode (109 sections in the last layer, 27% of faces skipped; last frame 317 section layers in 193 draws, vanilla: 317; batched: 142 buffers (142 packed), 23 MB (20 MB used), last layer 29 buffers / 74 draws); regions: ; batched: 142 buffers (142 packed), 23 MB (20 MB used), last layer 29 buffers / 74 draws; render scale: 100%; chests baked so far: 40
- scale70: 4 GC collections, 10 ms in GC; renderer: Aetherium pipeline, compatible mode (111 sections in the last layer, 27% of faces skipped; last frame 317 section layers in 193 draws, vanilla: 317; batched: 142 buffers (142 packed), 23 MB (20 MB used), last layer 30 buffers / 75 draws); regions: ; batched: 142 buffers (142 packed), 23 MB (20 MB used), last layer 30 buffers / 75 draws; render scale: 70%; chests baked so far: 44
- fly-vanilla: 23 GC collections, 119 ms in GC; renderer: Vanilla; regions: ; render scale: 100%; chests baked so far: 44
- fly-aetherium: 47 GC collections, 145 ms in GC; renderer: Aetherium pipeline, compatible mode (77 sections in the last layer, 26% of faces skipped; last frame 249 section layers in 126 draws, vanilla: 249; batched: 133 buffers (133 packed), 21 MB (17 MB used), last layer 14 buffers / 17 draws); regions: ; batched: 133 buffers (133 packed), 21 MB (17 MB used), last layer 14 buffers / 17 draws; render scale: 100%; chests baked so far: 51
- fly-boost-off: 12 GC collections, 70 ms in GC; renderer: Aetherium pipeline, compatible mode (40 sections in the last layer, 27% of faces skipped; last frame 65 section layers in 52 draws, vanilla: 65; batched: 122 buffers, 53 MB (41 MB used), last layer 12 buffers / 36 draws); regions: ; batched: 122 buffers, 53 MB (41 MB used), last layer 12 buffers / 36 draws; render scale: 100%; chests baked so far: 51
- fly-aetherium-z: 16 GC collections, 86 ms in GC; renderer: Aetherium pipeline, compatible mode (51 sections in the last layer, 39% of faces skipped; last frame 206 section layers in 196 draws, vanilla: 206; batched: 138 buffers (138 packed), 33 MB (24 MB used), last layer 7 buffers / 10 draws); regions: ; batched: 138 buffers (138 packed), 33 MB (24 MB used), last layer 7 buffers / 10 draws; render scale: 100%; chests baked so far: 58
