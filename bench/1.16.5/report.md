# Aetherium auto benchmark (1.16.5)

Window 854x480, render distance 8, seed 7294105550031, GPU: Unknown, Java 17.0.20.1, 4 CPUs.

| Scenario | Avg FPS | 1% low FPS | p50 ms | p99 ms | p99.9 ms | Max ms | Frames > 50 ms | Frames > 100 ms |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| Vanilla (Aetherium off) | 35.6 | 21.0 | 27.64 | 39.03 | 61.04 | 61.0 | 2 | 0 |
| Aetherium (defaults) | 39.8 | 20.2 | 24.84 | 39.89 | 59.33 | 59.3 | 3 | 0 |
| Aetherium, all 1.12.0 options off | 40.3 | 26.9 | 24.72 | 31.58 | 54.49 | 54.5 | 1 | 0 |
| Aetherium, directional culling + view-first building off | 40.5 | 22.3 | 24.56 | 37.04 | 57.37 | 57.4 | 2 | 0 |
| Aetherium, GL call trimming + cheap damage tint off | 41.7 | 29.0 | 23.76 | 31.92 | 37.22 | 37.2 | 0 | 0 |
| Aetherium, all 1.17.2 options off | 40.9 | 23.3 | 24.31 | 32.67 | 53.80 | 53.8 | 3 | 0 |
| Aetherium + GPU occlusion culling | 42.5 | 26.0 | 23.22 | 32.02 | 53.08 | 53.1 | 1 | 0 |
| Aetherium (defaults), measured again | 40.2 | 27.6 | 24.94 | 30.57 | 50.95 | 51.0 | 1 | 0 |
| Aetherium + render scale 70% | 45.7 | 33.4 | 21.86 | 27.93 | 38.05 | 38.0 | 0 | 0 |
| Flying into new terrain (+X), Aetherium off | 60.3 | 21.2 | 14.92 | 38.59 | 61.02 | 64.2 | 3 | 0 |
| Flying into new terrain (-X), Aetherium defaults | 52.3 | 26.7 | 18.09 | 34.36 | 41.05 | 43.5 | 0 | 0 |
| Flying into new terrain (+Z), all 1.17.2 options off | 58.8 | 30.3 | 15.93 | 30.01 | 34.38 | 42.0 | 0 | 0 |
| Flying into new terrain (-Z), Aetherium defaults | 60.8 | 27.0 | 15.45 | 31.62 | 42.98 | 46.6 | 0 | 0 |

## Notes

- vanilla: 3 GC collections, 17 ms in GC; renderer: Vanilla; regions: ; render scale: 100%
- aetherium: 2 GC collections, 10 ms in GC; renderer: Aetherium pipeline, compatible mode (99 sections in the last layer, 27% of faces skipped; last frame 297 section layers in 192 draws, vanilla: 297; batched: 143 buffers (143 packed), 24 MB (20 MB used), last layer 29 buffers / 72 draws); regions: ; batched: 143 buffers (143 packed), 24 MB (20 MB used), last layer 29 buffers / 72 draws; render scale: 100%
- new-off: 3 GC collections, 14 ms in GC; renderer: Aetherium pipeline, compatible mode (100 sections in the last layer, 27% of faces skipped; last frame 289 section layers in 187 draws, vanilla: 289; batched: 143 buffers (143 packed), 24 MB (20 MB used), last layer 28 buffers / 73 draws); regions: ; batched: 143 buffers (143 packed), 24 MB (20 MB used), last layer 28 buffers / 73 draws; render scale: 100%
- no-cull: 2 GC collections, 5 ms in GC; renderer: Aetherium pipeline, compatible mode (100 sections in the last layer, 27% of faces skipped; last frame 297 section layers in 192 draws, vanilla: 297; batched: 142 buffers (142 packed), 23 MB (20 MB used), last layer 29 buffers / 74 draws); regions: ; batched: 142 buffers (142 packed), 23 MB (20 MB used), last layer 29 buffers / 74 draws; render scale: 100%
- no-gl: 2 GC collections, 5 ms in GC; renderer: Aetherium pipeline, compatible mode (99 sections in the last layer, 27% of faces skipped; last frame 292 section layers in 190 draws, vanilla: 292; batched: 142 buffers (142 packed), 23 MB (20 MB used), last layer 28 buffers / 73 draws); regions: ; batched: 142 buffers (142 packed), 23 MB (20 MB used), last layer 28 buffers / 73 draws; render scale: 100%
- boost-off: 2 GC collections, 5 ms in GC; renderer: Aetherium pipeline, compatible mode (99 sections in the last layer, 27% of faces skipped; last frame 288 section layers in 187 draws, vanilla: 288; batched: 142 buffers, 38 MB (32 MB used), last layer 28 buffers / 74 draws); regions: ; batched: 142 buffers, 38 MB (32 MB used), last layer 28 buffers / 74 draws; render scale: 100%
- occlusion: 2 GC collections, 10 ms in GC; renderer: Aetherium pipeline, compatible mode (90 sections in the last layer, 25% of faces skipped; last frame 250 section layers in 165 draws, vanilla: 250; batched: 142 buffers (142 packed), 23 MB (20 MB used), last layer 29 buffers / 70 draws); regions: ; batched: 142 buffers (142 packed), 23 MB (20 MB used), last layer 29 buffers / 70 draws; render scale: 100%
- aetherium-again: 2 GC collections, 5 ms in GC; renderer: Aetherium pipeline, compatible mode (100 sections in the last layer, 27% of faces skipped; last frame 289 section layers in 187 draws, vanilla: 289; batched: 142 buffers (142 packed), 24 MB (20 MB used), last layer 28 buffers / 74 draws); regions: ; batched: 142 buffers (142 packed), 24 MB (20 MB used), last layer 28 buffers / 74 draws; render scale: 100%
- scale70: 3 GC collections, 8 ms in GC; renderer: Aetherium pipeline, compatible mode (100 sections in the last layer, 27% of faces skipped; last frame 289 section layers in 187 draws, vanilla: 289; batched: 141 buffers (141 packed), 23 MB (20 MB used), last layer 28 buffers / 73 draws); regions: ; batched: 141 buffers (141 packed), 23 MB (20 MB used), last layer 28 buffers / 73 draws; render scale: 70%
- fly-vanilla: 11 GC collections, 58 ms in GC; renderer: Vanilla; regions: ; render scale: 100%
- fly-aetherium: 14 GC collections, 64 ms in GC; renderer: Aetherium pipeline, compatible mode (76 sections in the last layer, 26% of faces skipped; last frame 239 section layers in 125 draws, vanilla: 239; batched: 135 buffers (135 packed), 21 MB (17 MB used), last layer 14 buffers / 19 draws); regions: ; batched: 135 buffers (135 packed), 21 MB (17 MB used), last layer 14 buffers / 19 draws; render scale: 100%
- fly-boost-off: 14 GC collections, 65 ms in GC; renderer: Aetherium pipeline, compatible mode (42 sections in the last layer, 27% of faces skipped; last frame 66 section layers in 50 draws, vanilla: 66; batched: 120 buffers, 52 MB (42 MB used), last layer 8 buffers / 37 draws); regions: ; batched: 120 buffers, 52 MB (42 MB used), last layer 8 buffers / 37 draws; render scale: 100%
- fly-aetherium-z: 18 GC collections, 83 ms in GC; renderer: Aetherium pipeline, compatible mode (45 sections in the last layer, 38% of faces skipped; last frame 181 section layers in 174 draws, vanilla: 181; batched: 135 buffers (135 packed), 32 MB (23 MB used), last layer 7 buffers / 9 draws); regions: ; batched: 135 buffers (135 packed), 32 MB (23 MB used), last layer 7 buffers / 9 draws; render scale: 100%
