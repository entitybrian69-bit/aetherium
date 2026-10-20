# Aetherium auto benchmark (1.16.5)

Window 854x480, render distance 8, seed 7294105550031, GPU: Unknown, Java 17.0.20.1, 4 CPUs.

| Scenario | Avg FPS | 1% low FPS | p50 ms | p99 ms | p99.9 ms | Max ms | Frames > 50 ms | Frames > 100 ms |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| Vanilla (Aetherium off) | 24.1 | 17.7 | 41.21 | 54.14 | 59.81 | 59.8 | 19 | 0 |
| Aetherium (defaults) | 26.3 | 19.8 | 37.62 | 49.78 | 50.92 | 50.9 | 5 | 0 |
| Aetherium, all 1.12.0 options off | 25.8 | 19.1 | 38.44 | 50.68 | 54.31 | 54.3 | 7 | 0 |
| Aetherium, directional culling + view-first building off | 26.4 | 20.6 | 37.68 | 47.71 | 48.91 | 48.9 | 0 | 0 |
| Aetherium, GL call trimming + cheap damage tint off | 26.4 | 20.4 | 37.64 | 46.76 | 50.82 | 50.8 | 2 | 0 |
| Aetherium, server lighting + server idle off | 26.3 | 18.4 | 37.48 | 52.12 | 56.11 | 56.1 | 6 | 0 |
| Aetherium, light table rate + AO cache off | 26.2 | 18.2 | 37.73 | 51.16 | 56.79 | 56.8 | 8 | 0 |
| Aetherium (defaults), measured again | 26.6 | 20.9 | 37.46 | 45.86 | 50.07 | 50.1 | 1 | 0 |
| Aetherium + render scale 70% | 29.6 | 23.3 | 33.79 | 42.05 | 44.39 | 44.4 | 0 | 0 |
| Flying into new terrain (+X), Aetherium off | 41.2 | 15.7 | 22.38 | 55.40 | 83.65 | 83.7 | 16 | 0 |
| Flying into new terrain (-X), Aetherium defaults | 29.6 | 16.1 | 32.20 | 56.40 | 72.99 | 73.0 | 23 | 0 |
| Flying into new terrain (+Z), all 1.12.0 options off | 34.9 | 20.8 | 27.54 | 43.80 | 57.02 | 57.0 | 2 | 0 |
| Flying into new terrain (-Z), Aetherium defaults | 38.9 | 16.9 | 23.88 | 53.73 | 70.86 | 70.9 | 13 | 0 |

## Notes

- vanilla: 4 GC collections, 17 ms in GC; renderer: Vanilla; regions: ; render scale: 100%
- aetherium: 4 GC collections, 14 ms in GC; renderer: Aetherium pipeline, compatible mode (88 sections in the last layer, 29% of faces skipped; last frame 262 section layers in 165 draws, vanilla: 262; batched: 143 buffers, 38 MB (32 MB used), last layer 27 buffers / 59 draws); regions: ; batched: 143 buffers, 38 MB (32 MB used), last layer 27 buffers / 59 draws; render scale: 100%
- new-off: 4 GC collections, 13 ms in GC; renderer: Aetherium pipeline, compatible mode (88 sections in the last layer, 28% of faces skipped; last frame 262 section layers in 167 draws, vanilla: 262; batched: 144 buffers, 39 MB (32 MB used), last layer 27 buffers / 59 draws); regions: ; batched: 144 buffers, 39 MB (32 MB used), last layer 27 buffers / 59 draws; render scale: 100%
- no-cull: 4 GC collections, 22 ms in GC; renderer: Aetherium pipeline, compatible mode (89 sections in the last layer, 29% of faces skipped; last frame 257 section layers in 161 draws, vanilla: 257; batched: 143 buffers, 38 MB (32 MB used), last layer 27 buffers / 62 draws); regions: ; batched: 143 buffers, 38 MB (32 MB used), last layer 27 buffers / 62 draws; render scale: 100%
- no-gl: 4 GC collections, 13 ms in GC; renderer: Aetherium pipeline, compatible mode (88 sections in the last layer, 28% of faces skipped; last frame 262 section layers in 167 draws, vanilla: 262; batched: 143 buffers, 38 MB (32 MB used), last layer 27 buffers / 59 draws); regions: ; batched: 143 buffers, 38 MB (32 MB used), last layer 27 buffers / 59 draws; render scale: 100%
- no-server: 4 GC collections, 15 ms in GC; renderer: Aetherium pipeline, compatible mode (88 sections in the last layer, 29% of faces skipped; last frame 262 section layers in 166 draws, vanilla: 262; batched: 142 buffers, 38 MB (32 MB used), last layer 27 buffers / 59 draws); regions: ; batched: 142 buffers, 38 MB (32 MB used), last layer 27 buffers / 59 draws; render scale: 100%
- no-light: 4 GC collections, 14 ms in GC; renderer: Aetherium pipeline, compatible mode (88 sections in the last layer, 29% of faces skipped; last frame 262 section layers in 165 draws, vanilla: 262; batched: 143 buffers, 38 MB (32 MB used), last layer 27 buffers / 59 draws); regions: ; batched: 143 buffers, 38 MB (32 MB used), last layer 27 buffers / 59 draws; render scale: 100%
- aetherium-again: 4 GC collections, 13 ms in GC; renderer: Aetherium pipeline, compatible mode (88 sections in the last layer, 29% of faces skipped; last frame 262 section layers in 165 draws, vanilla: 262; batched: 142 buffers, 38 MB (32 MB used), last layer 27 buffers / 59 draws); regions: ; batched: 142 buffers, 38 MB (32 MB used), last layer 27 buffers / 59 draws; render scale: 100%
- scale70: 4 GC collections, 14 ms in GC; renderer: Aetherium pipeline, compatible mode (88 sections in the last layer, 28% of faces skipped; last frame 262 section layers in 165 draws, vanilla: 262; batched: 143 buffers, 38 MB (32 MB used), last layer 27 buffers / 59 draws); regions: ; batched: 143 buffers, 38 MB (32 MB used), last layer 27 buffers / 59 draws; render scale: 70%
- fly-vanilla: 11 GC collections, 70 ms in GC; renderer: Vanilla; regions: ; render scale: 100%
- fly-aetherium: 15 GC collections, 90 ms in GC; renderer: Aetherium pipeline, compatible mode (79 sections in the last layer, 26% of faces skipped; last frame 247 section layers in 131 draws, vanilla: 247; batched: 130 buffers, 32 MB (26 MB used), last layer 14 buffers / 20 draws); regions: ; batched: 130 buffers, 32 MB (26 MB used), last layer 14 buffers / 20 draws; render scale: 100%
- fly-new-off: 14 GC collections, 75 ms in GC; renderer: Aetherium pipeline, compatible mode (52 sections in the last layer, 28% of faces skipped; last frame 90 section layers in 78 draws, vanilla: 90; batched: 115 buffers, 47 MB (38 MB used), last layer 16 buffers / 54 draws); regions: ; batched: 115 buffers, 47 MB (38 MB used), last layer 16 buffers / 54 draws; render scale: 100%
- fly-aetherium-z: 26 GC collections, 110 ms in GC; renderer: Aetherium pipeline, compatible mode (42 sections in the last layer, 37% of faces skipped; last frame 176 section layers in 180 draws, vanilla: 176; batched: 120 buffers, 44 MB (31 MB used), last layer 7 buffers / 8 draws); regions: ; batched: 120 buffers, 44 MB (31 MB used), last layer 7 buffers / 8 draws; render scale: 100%
