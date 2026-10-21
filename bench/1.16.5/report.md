# Aetherium auto benchmark (1.16.5)

Window 854x480, render distance 8, seed 7294105550031, GPU: Unknown, Java 17.0.20.1, 4 CPUs.

| Scenario | Avg FPS | 1% low FPS | p50 ms | p99 ms | p99.9 ms | Max ms | Frames > 50 ms | Frames > 100 ms |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| Vanilla (Aetherium off) | 23.4 | 15.5 | 42.32 | 59.91 | 66.65 | 66.7 | 44 | 0 |
| Aetherium (defaults) | 26.1 | 19.9 | 37.87 | 49.01 | 51.27 | 51.3 | 3 | 0 |
| Aetherium, all 1.12.0 options off | 25.8 | 16.8 | 38.26 | 50.68 | 84.81 | 84.8 | 7 | 0 |
| Aetherium, directional culling + view-first building off | 26.2 | 19.8 | 37.91 | 48.86 | 51.56 | 51.6 | 3 | 0 |
| Aetherium, GL call trimming + cheap damage tint off | 26.2 | 19.2 | 37.96 | 49.65 | 54.20 | 54.2 | 5 | 0 |
| Aetherium, all 1.17.2 options off | 25.8 | 16.5 | 38.47 | 52.36 | 67.64 | 67.6 | 11 | 0 |
| Aetherium + GPU occlusion culling | 26.6 | 20.6 | 37.33 | 46.04 | 51.16 | 51.2 | 1 | 0 |
| Aetherium (defaults), measured again | 26.4 | 19.7 | 37.33 | 49.45 | 52.45 | 52.5 | 4 | 0 |
| Aetherium + render scale 70% | 29.4 | 22.4 | 33.97 | 43.60 | 45.09 | 45.1 | 0 | 0 |
| Flying into new terrain (+X), Aetherium off | 38.9 | 15.9 | 23.70 | 57.16 | 71.28 | 71.3 | 19 | 0 |
| Flying into new terrain (-X), Aetherium defaults | 29.8 | 16.7 | 31.98 | 58.36 | 60.43 | 60.4 | 24 | 0 |
| Flying into new terrain (+Z), all 1.17.2 options off | 34.3 | 19.3 | 27.65 | 48.09 | 57.41 | 57.4 | 3 | 0 |
| Flying into new terrain (-Z), Aetherium defaults | 37.8 | 17.0 | 24.26 | 53.94 | 67.49 | 67.5 | 15 | 0 |

## Notes

- vanilla: 4 GC collections, 19 ms in GC; renderer: Vanilla; regions: ; render scale: 100%
- aetherium: 3 GC collections, 14 ms in GC; renderer: Aetherium pipeline, compatible mode (92 sections in the last layer, 28% of faces skipped; last frame 270 section layers in 173 draws, vanilla: 270; batched: 144 buffers (144 packed), 24 MB (20 MB used), last layer 28 buffers / 64 draws); regions: ; batched: 144 buffers (144 packed), 24 MB (20 MB used), last layer 28 buffers / 64 draws; render scale: 100%
- new-off: 3 GC collections, 10 ms in GC; renderer: Aetherium pipeline, compatible mode (91 sections in the last layer, 28% of faces skipped; last frame 268 section layers in 171 draws, vanilla: 268; batched: 145 buffers (145 packed), 24 MB (20 MB used), last layer 28 buffers / 63 draws); regions: ; batched: 145 buffers (145 packed), 24 MB (20 MB used), last layer 28 buffers / 63 draws; render scale: 100%
- no-cull: 3 GC collections, 13 ms in GC; renderer: Aetherium pipeline, compatible mode (92 sections in the last layer, 28% of faces skipped; last frame 270 section layers in 172 draws, vanilla: 270; batched: 145 buffers (145 packed), 24 MB (20 MB used), last layer 28 buffers / 63 draws); regions: ; batched: 145 buffers (145 packed), 24 MB (20 MB used), last layer 28 buffers / 63 draws; render scale: 100%
- no-gl: 3 GC collections, 13 ms in GC; renderer: Aetherium pipeline, compatible mode (93 sections in the last layer, 28% of faces skipped; last frame 272 section layers in 178 draws, vanilla: 272; batched: 145 buffers (145 packed), 24 MB (20 MB used), last layer 28 buffers / 64 draws); regions: ; batched: 145 buffers (145 packed), 24 MB (20 MB used), last layer 28 buffers / 64 draws; render scale: 100%
- boost-off: 4 GC collections, 12 ms in GC; renderer: Aetherium pipeline, compatible mode (92 sections in the last layer, 28% of faces skipped; last frame 270 section layers in 174 draws, vanilla: 270; batched: 144 buffers, 39 MB (32 MB used), last layer 28 buffers / 63 draws); regions: ; batched: 144 buffers, 39 MB (32 MB used), last layer 28 buffers / 63 draws; render scale: 100%
- occlusion: 4 GC collections, 17 ms in GC; renderer: Aetherium pipeline, compatible mode (87 sections in the last layer, 26% of faces skipped; last frame 251 section layers in 162 draws, vanilla: 251; batched: 144 buffers (144 packed), 24 MB (20 MB used), last layer 28 buffers / 62 draws); regions: ; batched: 144 buffers (144 packed), 24 MB (20 MB used), last layer 28 buffers / 62 draws; render scale: 100%
- aetherium-again: 3 GC collections, 9 ms in GC; renderer: Aetherium pipeline, compatible mode (91 sections in the last layer, 28% of faces skipped; last frame 268 section layers in 171 draws, vanilla: 268; batched: 145 buffers (145 packed), 24 MB (20 MB used), last layer 28 buffers / 63 draws); regions: ; batched: 145 buffers (145 packed), 24 MB (20 MB used), last layer 28 buffers / 63 draws; render scale: 100%
- scale70: 3 GC collections, 9 ms in GC; renderer: Aetherium pipeline, compatible mode (93 sections in the last layer, 28% of faces skipped; last frame 272 section layers in 175 draws, vanilla: 272; batched: 144 buffers (144 packed), 24 MB (20 MB used), last layer 28 buffers / 64 draws); regions: ; batched: 144 buffers (144 packed), 24 MB (20 MB used), last layer 28 buffers / 64 draws; render scale: 70%
- fly-vanilla: 11 GC collections, 70 ms in GC; renderer: Vanilla; regions: ; render scale: 100%
- fly-aetherium: 18 GC collections, 90 ms in GC; renderer: Aetherium pipeline, compatible mode (75 sections in the last layer, 26% of faces skipped; last frame 239 section layers in 123 draws, vanilla: 239; batched: 131 buffers (131 packed), 20 MB (16 MB used), last layer 15 buffers / 20 draws); regions: ; batched: 131 buffers (131 packed), 20 MB (16 MB used), last layer 15 buffers / 20 draws; render scale: 100%
- fly-boost-off: 25 GC collections, 111 ms in GC; renderer: Aetherium pipeline, compatible mode (43 sections in the last layer, 27% of faces skipped; last frame 66 section layers in 57 draws, vanilla: 66; batched: 114 buffers, 47 MB (36 MB used), last layer 14 buffers / 42 draws); regions: ; batched: 114 buffers, 47 MB (36 MB used), last layer 14 buffers / 42 draws; render scale: 100%
- fly-aetherium-z: 67 GC collections, 197 ms in GC; renderer: Aetherium pipeline, compatible mode (41 sections in the last layer, 37% of faces skipped; last frame 151 section layers in 151 draws, vanilla: 151; batched: 121 buffers (121 packed), 28 MB (20 MB used), last layer 6 buffers / 7 draws); regions: ; batched: 121 buffers (121 packed), 28 MB (20 MB used), last layer 6 buffers / 7 draws; render scale: 100%
