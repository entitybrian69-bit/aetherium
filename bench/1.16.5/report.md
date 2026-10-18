# Aetherium auto benchmark (1.16.5)

Window 854x480, render distance 8, seed 7294105550031, GPU: Unknown, Java 17.0.20.1, 4 CPUs.

| Scenario | Avg FPS | 1% low FPS | p50 ms | p99 ms | p99.9 ms | Max ms | Frames > 50 ms | Frames > 100 ms |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| Vanilla (Aetherium off) | 23.9 | 16.2 | 40.90 | 56.45 | 66.14 | 66.1 | 40 | 0 |
| Aetherium (defaults) | 26.1 | 19.1 | 38.02 | 50.69 | 54.97 | 55.0 | 6 | 0 |
| Aetherium, all 1.12.0 options off | 25.8 | 18.1 | 38.22 | 52.15 | 60.01 | 60.0 | 8 | 0 |
| Aetherium, directional culling + view-first building off | 26.5 | 18.5 | 37.40 | 49.09 | 59.28 | 59.3 | 5 | 0 |
| Aetherium, GL call trimming + cheap damage tint off | 26.2 | 17.8 | 37.80 | 52.55 | 62.14 | 62.1 | 8 | 0 |
| Aetherium, server lighting + server idle off | 26.0 | 17.3 | 37.86 | 53.89 | 59.69 | 59.7 | 14 | 0 |
| Aetherium, light table rate + AO cache off | 26.4 | 19.7 | 37.74 | 48.81 | 52.84 | 52.8 | 2 | 0 |
| Aetherium (defaults), measured again | 26.8 | 18.8 | 36.98 | 48.78 | 56.78 | 56.8 | 4 | 0 |
| Aetherium + render scale 70% | 29.7 | 22.2 | 33.39 | 43.69 | 47.00 | 47.0 | 0 | 0 |
| Flying into new terrain (+X), Aetherium off | 40.1 | 17.2 | 22.93 | 53.97 | 63.07 | 63.1 | 17 | 0 |
| Flying into new terrain (-X), Aetherium defaults | 30.0 | 16.9 | 32.36 | 54.24 | 66.81 | 66.8 | 22 | 0 |
| Flying into new terrain (+Z), all 1.12.0 options off | 34.3 | 20.3 | 28.01 | 47.42 | 52.57 | 52.6 | 2 | 0 |
| Flying into new terrain (-Z), Aetherium defaults | 38.7 | 18.3 | 24.10 | 49.49 | 63.58 | 63.6 | 5 | 0 |

## Notes

- vanilla: 3 GC collections, 21 ms in GC; renderer: Vanilla; regions: ; render scale: 100%
- aetherium: 3 GC collections, 25 ms in GC; renderer: Aetherium pipeline, compatible mode (89 sections in the last layer, 28% of faces skipped; last frame 256 section layers in 165 draws, vanilla: 256; batched: 143 buffers, 38 MB (31 MB used), last layer 28 buffers / 62 draws); regions: ; batched: 143 buffers, 38 MB (31 MB used), last layer 28 buffers / 62 draws; render scale: 100%
- new-off: 2 GC collections, 9 ms in GC; renderer: Aetherium pipeline, compatible mode (87 sections in the last layer, 29% of faces skipped; last frame 261 section layers in 166 draws, vanilla: 261; batched: 144 buffers, 39 MB (31 MB used), last layer 28 buffers / 60 draws); regions: ; batched: 144 buffers, 39 MB (31 MB used), last layer 28 buffers / 60 draws; render scale: 100%
- no-cull: 2 GC collections, 7 ms in GC; renderer: Aetherium pipeline, compatible mode (87 sections in the last layer, 29% of faces skipped; last frame 261 section layers in 166 draws, vanilla: 261; batched: 144 buffers, 39 MB (31 MB used), last layer 28 buffers / 60 draws); regions: ; batched: 144 buffers, 39 MB (31 MB used), last layer 28 buffers / 60 draws; render scale: 100%
- no-gl: 3 GC collections, 10 ms in GC; renderer: Aetherium pipeline, compatible mode (87 sections in the last layer, 29% of faces skipped; last frame 261 section layers in 166 draws, vanilla: 261; batched: 144 buffers, 39 MB (31 MB used), last layer 28 buffers / 60 draws); regions: ; batched: 144 buffers, 39 MB (31 MB used), last layer 28 buffers / 60 draws; render scale: 100%
- no-server: 2 GC collections, 6 ms in GC; renderer: Aetherium pipeline, compatible mode (87 sections in the last layer, 29% of faces skipped; last frame 264 section layers in 169 draws, vanilla: 264; batched: 144 buffers, 39 MB (31 MB used), last layer 28 buffers / 60 draws); regions: ; batched: 144 buffers, 39 MB (31 MB used), last layer 28 buffers / 60 draws; render scale: 100%
- no-light: 3 GC collections, 9 ms in GC; renderer: Aetherium pipeline, compatible mode (88 sections in the last layer, 29% of faces skipped; last frame 256 section layers in 161 draws, vanilla: 256; batched: 144 buffers, 39 MB (31 MB used), last layer 28 buffers / 62 draws); regions: ; batched: 144 buffers, 39 MB (31 MB used), last layer 28 buffers / 62 draws; render scale: 100%
- aetherium-again: 2 GC collections, 10 ms in GC; renderer: Aetherium pipeline, compatible mode (89 sections in the last layer, 28% of faces skipped; last frame 256 section layers in 161 draws, vanilla: 256; batched: 143 buffers, 38 MB (31 MB used), last layer 28 buffers / 62 draws); regions: ; batched: 143 buffers, 38 MB (31 MB used), last layer 28 buffers / 62 draws; render scale: 100%
- scale70: 3 GC collections, 11 ms in GC; renderer: Aetherium pipeline, compatible mode (88 sections in the last layer, 28% of faces skipped; last frame 256 section layers in 161 draws, vanilla: 256; batched: 144 buffers, 39 MB (31 MB used), last layer 28 buffers / 62 draws); regions: ; batched: 144 buffers, 39 MB (31 MB used), last layer 28 buffers / 62 draws; render scale: 70%
- fly-vanilla: 6 GC collections, 70 ms in GC; renderer: Vanilla; regions: ; render scale: 100%
- fly-aetherium: 12 GC collections, 87 ms in GC; renderer: Aetherium pipeline, compatible mode (69 sections in the last layer, 26% of faces skipped; last frame 210 section layers in 115 draws, vanilla: 210; batched: 130 buffers, 33 MB (25 MB used), last layer 13 buffers / 18 draws); regions: ; batched: 130 buffers, 33 MB (25 MB used), last layer 13 buffers / 18 draws; render scale: 100%
- fly-new-off: 11 GC collections, 72 ms in GC; renderer: Aetherium pipeline, compatible mode (52 sections in the last layer, 27% of faces skipped; last frame 88 section layers in 77 draws, vanilla: 88; batched: 117 buffers, 50 MB (39 MB used), last layer 14 buffers / 52 draws); regions: ; batched: 117 buffers, 50 MB (39 MB used), last layer 14 buffers / 52 draws; render scale: 100%
- fly-aetherium-z: 14 GC collections, 93 ms in GC; renderer: Aetherium pipeline, compatible mode (38 sections in the last layer, 37% of faces skipped; last frame 159 section layers in 166 draws, vanilla: 159; batched: 119 buffers, 43 MB (31 MB used), last layer 5 buffers / 5 draws); regions: ; batched: 119 buffers, 43 MB (31 MB used), last layer 5 buffers / 5 draws; render scale: 100%
