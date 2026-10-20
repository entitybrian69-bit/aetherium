# Aetherium auto benchmark (1.16.5)

Window 854x480, render distance 8, seed 7294105550031, GPU: Unknown, Java 17.0.20.1, 4 CPUs.

| Scenario | Avg FPS | 1% low FPS | p50 ms | p99 ms | p99.9 ms | Max ms | Frames > 50 ms | Frames > 100 ms |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| Vanilla (Aetherium off) | 20.2 | 15.6 | 49.29 | 60.91 | 67.74 | 67.7 | 181 | 0 |
| Aetherium (defaults) | 21.8 | 16.1 | 45.33 | 58.46 | 65.02 | 65.0 | 100 | 0 |
| Aetherium, all 1.12.0 options off | 22.1 | 16.7 | 44.98 | 56.25 | 63.94 | 63.9 | 55 | 0 |
| Aetherium, directional culling + view-first building off | 22.4 | 16.8 | 44.58 | 55.35 | 65.08 | 65.1 | 57 | 0 |
| Aetherium, GL call trimming + cheap damage tint off | 22.4 | 16.3 | 44.01 | 59.20 | 64.47 | 64.5 | 56 | 0 |
| Aetherium, server lighting + server idle off | 22.0 | 16.1 | 45.02 | 59.41 | 63.15 | 63.2 | 82 | 0 |
| Aetherium, light table rate + AO cache off | 22.3 | 15.7 | 44.77 | 56.91 | 70.07 | 70.1 | 38 | 0 |
| Aetherium (defaults), measured again | 22.5 | 16.9 | 44.08 | 56.52 | 64.78 | 64.8 | 58 | 0 |
| Aetherium + render scale 70% | 24.7 | 19.2 | 40.20 | 51.06 | 54.01 | 54.0 | 12 | 0 |
| Flying into new terrain (+X), Aetherium off | 39.0 | 17.8 | 24.44 | 49.25 | 63.18 | 63.2 | 7 | 0 |
| Flying into new terrain (-X), Aetherium defaults | 26.0 | 14.0 | 37.03 | 66.04 | 76.39 | 76.4 | 55 | 0 |
| Flying into new terrain (+Z), all 1.12.0 options off | 30.2 | 18.7 | 31.58 | 51.07 | 55.48 | 55.5 | 11 | 0 |
| Flying into new terrain (-Z), Aetherium defaults | 34.2 | 16.8 | 27.34 | 52.64 | 70.64 | 70.6 | 14 | 0 |

## Notes

- vanilla: 4 GC collections, 21 ms in GC; renderer: Vanilla; regions: ; render scale: 100%
- aetherium: 5 GC collections, 20 ms in GC; renderer: Aetherium pipeline, compatible mode (96 sections in the last layer, 28% of faces skipped; last frame 278 section layers in 178 draws, vanilla: 278; batched: 144 buffers, 39 MB (32 MB used), last layer 28 buffers / 69 draws); regions: ; batched: 144 buffers, 39 MB (32 MB used), last layer 28 buffers / 69 draws; render scale: 100%
- new-off: 4 GC collections, 22 ms in GC; renderer: Aetherium pipeline, compatible mode (95 sections in the last layer, 29% of faces skipped; last frame 278 section layers in 177 draws, vanilla: 278; batched: 145 buffers, 39 MB (32 MB used), last layer 28 buffers / 67 draws); regions: ; batched: 145 buffers, 39 MB (32 MB used), last layer 28 buffers / 67 draws; render scale: 100%
- no-cull: 4 GC collections, 20 ms in GC; renderer: Aetherium pipeline, compatible mode (96 sections in the last layer, 29% of faces skipped; last frame 277 section layers in 177 draws, vanilla: 277; batched: 145 buffers, 39 MB (32 MB used), last layer 28 buffers / 69 draws); regions: ; batched: 145 buffers, 39 MB (32 MB used), last layer 28 buffers / 69 draws; render scale: 100%
- no-gl: 4 GC collections, 26 ms in GC; renderer: Aetherium pipeline, compatible mode (96 sections in the last layer, 29% of faces skipped; last frame 280 section layers in 180 draws, vanilla: 280; batched: 145 buffers, 39 MB (32 MB used), last layer 28 buffers / 69 draws); regions: ; batched: 145 buffers, 39 MB (32 MB used), last layer 28 buffers / 69 draws; render scale: 100%
- no-server: 4 GC collections, 14 ms in GC; renderer: Aetherium pipeline, compatible mode (95 sections in the last layer, 29% of faces skipped; last frame 280 section layers in 179 draws, vanilla: 280; batched: 145 buffers, 39 MB (32 MB used), last layer 28 buffers / 67 draws); regions: ; batched: 145 buffers, 39 MB (32 MB used), last layer 28 buffers / 67 draws; render scale: 100%
- no-light: 4 GC collections, 21 ms in GC; renderer: Aetherium pipeline, compatible mode (95 sections in the last layer, 29% of faces skipped; last frame 280 section layers in 181 draws, vanilla: 280; batched: 145 buffers, 39 MB (32 MB used), last layer 28 buffers / 67 draws); regions: ; batched: 145 buffers, 39 MB (32 MB used), last layer 28 buffers / 67 draws; render scale: 100%
- aetherium-again: 4 GC collections, 15 ms in GC; renderer: Aetherium pipeline, compatible mode (96 sections in the last layer, 29% of faces skipped; last frame 277 section layers in 177 draws, vanilla: 277; batched: 145 buffers, 39 MB (32 MB used), last layer 28 buffers / 69 draws); regions: ; batched: 145 buffers, 39 MB (32 MB used), last layer 28 buffers / 69 draws; render scale: 100%
- scale70: 4 GC collections, 19 ms in GC; renderer: Aetherium pipeline, compatible mode (96 sections in the last layer, 28% of faces skipped; last frame 277 section layers in 176 draws, vanilla: 277; batched: 145 buffers, 39 MB (32 MB used), last layer 28 buffers / 69 draws); regions: ; batched: 145 buffers, 39 MB (32 MB used), last layer 28 buffers / 69 draws; render scale: 70%
- fly-vanilla: 13 GC collections, 107 ms in GC; renderer: Vanilla; regions: ; render scale: 100%
- fly-aetherium: 29 GC collections, 149 ms in GC; renderer: Aetherium pipeline, compatible mode (72 sections in the last layer, 26% of faces skipped; last frame 224 section layers in 117 draws, vanilla: 224; batched: 128 buffers, 32 MB (26 MB used), last layer 13 buffers / 18 draws); regions: ; batched: 128 buffers, 32 MB (26 MB used), last layer 13 buffers / 18 draws; render scale: 100%
- fly-new-off: 62 GC collections, 211 ms in GC; renderer: Aetherium pipeline, compatible mode (36 sections in the last layer, 27% of faces skipped; last frame 56 section layers in 44 draws, vanilla: 56; batched: 110 buffers, 46 MB (37 MB used), last layer 10 buffers / 31 draws); regions: ; batched: 110 buffers, 46 MB (37 MB used), last layer 10 buffers / 31 draws; render scale: 100%
- fly-aetherium-z: 80 GC collections, 238 ms in GC; renderer: Aetherium pipeline, compatible mode (30 sections in the last layer, 33% of faces skipped; last frame 141 section layers in 126 draws, vanilla: 141; batched: 121 buffers, 44 MB (31 MB used), last layer 5 buffers / 5 draws); regions: ; batched: 121 buffers, 44 MB (31 MB used), last layer 5 buffers / 5 draws; render scale: 100%
