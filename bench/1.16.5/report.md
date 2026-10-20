# Aetherium auto benchmark (1.16.5)

Window 854x480, render distance 8, seed 7294105550031, GPU: Unknown, Java 17.0.20.1, 4 CPUs.

| Scenario | Avg FPS | 1% low FPS | p50 ms | p99 ms | p99.9 ms | Max ms | Frames > 50 ms | Frames > 100 ms |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| Vanilla (Aetherium off) | 32.0 | 21.1 | 29.97 | 45.17 | 53.49 | 53.5 | 1 | 0 |
| Aetherium (defaults) | 38.6 | 24.1 | 25.77 | 37.69 | 49.94 | 49.9 | 0 | 0 |
| Aetherium, all 1.12.0 options off | 37.8 | 25.8 | 26.30 | 37.07 | 40.96 | 41.0 | 0 | 0 |
| Aetherium, directional culling + view-first building off | 35.6 | 22.6 | 27.41 | 41.30 | 50.46 | 50.5 | 1 | 0 |
| Aetherium, GL call trimming + cheap damage tint off | 37.8 | 25.4 | 26.23 | 38.58 | 40.32 | 40.3 | 0 | 0 |
| Aetherium, server lighting + server idle off | 38.5 | 26.3 | 25.96 | 33.43 | 45.33 | 45.3 | 0 | 0 |
| Aetherium, light table rate + AO cache off | 38.1 | 28.4 | 26.20 | 32.71 | 41.14 | 41.1 | 0 | 0 |
| Aetherium (defaults), measured again | 38.3 | 27.6 | 26.04 | 32.71 | 39.09 | 39.1 | 0 | 0 |
| Aetherium + render scale 70% | 41.3 | 23.9 | 23.41 | 37.37 | 49.43 | 49.4 | 0 | 0 |
| Flying into new terrain (+X), Aetherium off | 58.2 | 22.0 | 15.02 | 40.43 | 50.50 | 55.3 | 2 | 0 |
| Flying into new terrain (-X), Aetherium defaults | 49.4 | 25.8 | 19.10 | 35.66 | 42.46 | 42.5 | 0 | 0 |
| Flying into new terrain (+Z), all 1.12.0 options off | 55.0 | 29.0 | 16.99 | 29.24 | 37.80 | 42.1 | 0 | 0 |
| Flying into new terrain (-Z), Aetherium defaults | 58.9 | 25.5 | 15.97 | 35.69 | 40.82 | 45.8 | 0 | 0 |

## Notes

- vanilla: 4 GC collections, 17 ms in GC; renderer: Vanilla; regions: ; render scale: 100%
- aetherium: 3 GC collections, 11 ms in GC; renderer: Aetherium pipeline, compatible mode (97 sections in the last layer, 27% of faces skipped; last frame 278 section layers in 181 draws, vanilla: 278; batched: 142 buffers, 38 MB (32 MB used), last layer 28 buffers / 73 draws); regions: ; batched: 142 buffers, 38 MB (32 MB used), last layer 28 buffers / 73 draws; render scale: 100%
- new-off: 3 GC collections, 8 ms in GC; renderer: Aetherium pipeline, compatible mode (97 sections in the last layer, 27% of faces skipped; last frame 285 section layers in 185 draws, vanilla: 285; batched: 142 buffers, 38 MB (32 MB used), last layer 28 buffers / 73 draws); regions: ; batched: 142 buffers, 38 MB (32 MB used), last layer 28 buffers / 73 draws; render scale: 100%
- no-cull: 4 GC collections, 21 ms in GC; renderer: Aetherium pipeline, compatible mode (95 sections in the last layer, 27% of faces skipped; last frame 281 section layers in 183 draws, vanilla: 281; batched: 143 buffers, 38 MB (32 MB used), last layer 28 buffers / 70 draws); regions: ; batched: 143 buffers, 38 MB (32 MB used), last layer 28 buffers / 70 draws; render scale: 100%
- no-gl: 4 GC collections, 10 ms in GC; renderer: Aetherium pipeline, compatible mode (97 sections in the last layer, 27% of faces skipped; last frame 278 section layers in 180 draws, vanilla: 278; batched: 143 buffers, 38 MB (32 MB used), last layer 28 buffers / 73 draws); regions: ; batched: 143 buffers, 38 MB (32 MB used), last layer 28 buffers / 73 draws; render scale: 100%
- no-server: 4 GC collections, 11 ms in GC; renderer: Aetherium pipeline, compatible mode (95 sections in the last layer, 27% of faces skipped; last frame 278 section layers in 180 draws, vanilla: 278; batched: 143 buffers, 38 MB (32 MB used), last layer 28 buffers / 70 draws); regions: ; batched: 143 buffers, 38 MB (32 MB used), last layer 28 buffers / 70 draws; render scale: 100%
- no-light: 4 GC collections, 11 ms in GC; renderer: Aetherium pipeline, compatible mode (95 sections in the last layer, 27% of faces skipped; last frame 281 section layers in 183 draws, vanilla: 281; batched: 142 buffers, 38 MB (32 MB used), last layer 28 buffers / 70 draws); regions: ; batched: 142 buffers, 38 MB (32 MB used), last layer 28 buffers / 70 draws; render scale: 100%
- aetherium-again: 3 GC collections, 15 ms in GC; renderer: Aetherium pipeline, compatible mode (97 sections in the last layer, 27% of faces skipped; last frame 278 section layers in 180 draws, vanilla: 278; batched: 143 buffers, 38 MB (32 MB used), last layer 28 buffers / 73 draws); regions: ; batched: 143 buffers, 38 MB (32 MB used), last layer 28 buffers / 73 draws; render scale: 100%
- scale70: 3 GC collections, 12 ms in GC; renderer: Aetherium pipeline, compatible mode (97 sections in the last layer, 27% of faces skipped; last frame 278 section layers in 180 draws, vanilla: 278; batched: 142 buffers, 38 MB (32 MB used), last layer 28 buffers / 73 draws); regions: ; batched: 142 buffers, 38 MB (32 MB used), last layer 28 buffers / 73 draws; render scale: 70%
- fly-vanilla: 16 GC collections, 82 ms in GC; renderer: Vanilla; regions: ; render scale: 100%
- fly-aetherium: 21 GC collections, 99 ms in GC; renderer: Aetherium pipeline, compatible mode (72 sections in the last layer, 26% of faces skipped; last frame 255 section layers in 134 draws, vanilla: 255; batched: 135 buffers, 34 MB (27 MB used), last layer 12 buffers / 16 draws); regions: ; batched: 135 buffers, 34 MB (27 MB used), last layer 12 buffers / 16 draws; render scale: 100%
- fly-new-off: 35 GC collections, 105 ms in GC; renderer: Aetherium pipeline, compatible mode (49 sections in the last layer, 27% of faces skipped; last frame 78 section layers in 62 draws, vanilla: 78; batched: 122 buffers, 53 MB (42 MB used), last layer 10 buffers / 46 draws); regions: ; batched: 122 buffers, 53 MB (42 MB used), last layer 10 buffers / 46 draws; render scale: 100%
- fly-aetherium-z: 81 GC collections, 202 ms in GC; renderer: Aetherium pipeline, compatible mode (40 sections in the last layer, 37% of faces skipped; last frame 148 section layers in 142 draws, vanilla: 148; batched: 130 buffers, 50 MB (36 MB used), last layer 7 buffers / 9 draws); regions: ; batched: 130 buffers, 50 MB (36 MB used), last layer 7 buffers / 9 draws; render scale: 100%
