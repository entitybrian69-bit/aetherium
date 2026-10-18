# Aetherium auto benchmark (1.16.5)

Window 854x480, render distance 8, seed 7294105550031, GPU: Unknown, Java 17.0.20.1, 4 CPUs.

| Scenario | Avg FPS | 1% low FPS | p50 ms | p99 ms | p99.9 ms | Max ms | Frames > 50 ms | Frames > 100 ms |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| Vanilla (Aetherium off) | 21.8 | 17.1 | 46.28 | 56.35 | 60.82 | 60.8 | 70 | 0 |
| Aetherium (defaults) | 23.3 | 17.7 | 42.90 | 53.61 | 60.22 | 60.2 | 15 | 0 |
| Aetherium, all 1.12.0 options off | 22.6 | 17.0 | 43.94 | 57.12 | 60.62 | 60.6 | 35 | 0 |
| Aetherium, directional culling + view-first building off | 23.2 | 18.3 | 42.90 | 51.95 | 60.29 | 60.3 | 14 | 0 |
| Aetherium, GL call trimming + cheap damage tint off | 23.3 | 18.1 | 42.91 | 54.55 | 56.02 | 56.0 | 16 | 0 |
| Aetherium, server lighting + server idle off | 23.1 | 17.0 | 42.99 | 55.87 | 60.22 | 60.2 | 15 | 0 |
| Aetherium, light table rate + AO cache off | 23.1 | 19.1 | 43.53 | 51.50 | 53.45 | 53.5 | 15 | 0 |
| Aetherium (defaults), measured again | 23.2 | 18.2 | 42.93 | 54.06 | 56.14 | 56.1 | 25 | 0 |
| Aetherium + render scale 70% | 25.8 | 19.9 | 38.48 | 47.85 | 52.92 | 52.9 | 2 | 0 |
| Flying into new terrain (+X), Aetherium off | 41.2 | 16.3 | 21.77 | 56.38 | 70.30 | 70.3 | 16 | 0 |
| Flying into new terrain (-X), Aetherium defaults | 28.4 | 15.8 | 33.62 | 60.55 | 69.10 | 69.1 | 34 | 0 |
| Flying into new terrain (+Z), all 1.12.0 options off | 31.8 | 19.3 | 29.58 | 50.69 | 52.71 | 52.7 | 8 | 0 |
| Flying into new terrain (-Z), Aetherium defaults | 36.2 | 16.8 | 26.12 | 51.49 | 75.50 | 75.5 | 9 | 0 |

## Notes

- vanilla: 4 GC collections, 21 ms in GC; renderer: Vanilla; regions: ; render scale: 100%
- aetherium: 3 GC collections, 13 ms in GC; renderer: Aetherium pipeline, compatible mode (95 sections in the last layer, 29% of faces skipped; last frame 279 section layers in 181 draws, vanilla: 279; batched: 144 buffers, 38 MB (32 MB used), last layer 28 buffers / 67 draws); regions: ; batched: 144 buffers, 38 MB (32 MB used), last layer 28 buffers / 67 draws; render scale: 100%
- new-off: 4 GC collections, 16 ms in GC; renderer: Aetherium pipeline, compatible mode (95 sections in the last layer, 29% of faces skipped; last frame 279 section layers in 183 draws, vanilla: 279; batched: 144 buffers, 38 MB (32 MB used), last layer 28 buffers / 67 draws); regions: ; batched: 144 buffers, 38 MB (32 MB used), last layer 28 buffers / 67 draws; render scale: 100%
- no-cull: 4 GC collections, 18 ms in GC; renderer: Aetherium pipeline, compatible mode (95 sections in the last layer, 29% of faces skipped; last frame 278 section layers in 180 draws, vanilla: 278; batched: 144 buffers, 38 MB (32 MB used), last layer 28 buffers / 68 draws); regions: ; batched: 144 buffers, 38 MB (32 MB used), last layer 28 buffers / 68 draws; render scale: 100%
- no-gl: 3 GC collections, 11 ms in GC; renderer: Aetherium pipeline, compatible mode (95 sections in the last layer, 29% of faces skipped; last frame 278 section layers in 180 draws, vanilla: 278; batched: 141 buffers, 38 MB (32 MB used), last layer 28 buffers / 68 draws); regions: ; batched: 141 buffers, 38 MB (32 MB used), last layer 28 buffers / 68 draws; render scale: 100%
- no-server: 4 GC collections, 19 ms in GC; renderer: Aetherium pipeline, compatible mode (95 sections in the last layer, 29% of faces skipped; last frame 278 section layers in 180 draws, vanilla: 278; batched: 143 buffers, 38 MB (32 MB used), last layer 28 buffers / 68 draws); regions: ; batched: 143 buffers, 38 MB (32 MB used), last layer 28 buffers / 68 draws; render scale: 100%
- no-light: 4 GC collections, 18 ms in GC; renderer: Aetherium pipeline, compatible mode (95 sections in the last layer, 29% of faces skipped; last frame 278 section layers in 180 draws, vanilla: 278; batched: 144 buffers, 38 MB (32 MB used), last layer 28 buffers / 68 draws); regions: ; batched: 144 buffers, 38 MB (32 MB used), last layer 28 buffers / 68 draws; render scale: 100%
- aetherium-again: 4 GC collections, 14 ms in GC; renderer: Aetherium pipeline, compatible mode (95 sections in the last layer, 29% of faces skipped; last frame 278 section layers in 180 draws, vanilla: 278; batched: 144 buffers, 38 MB (32 MB used), last layer 28 buffers / 68 draws); regions: ; batched: 144 buffers, 38 MB (32 MB used), last layer 28 buffers / 68 draws; render scale: 100%
- scale70: 4 GC collections, 16 ms in GC; renderer: Aetherium pipeline, compatible mode (95 sections in the last layer, 28% of faces skipped; last frame 278 section layers in 180 draws, vanilla: 278; batched: 144 buffers, 38 MB (32 MB used), last layer 28 buffers / 68 draws); regions: ; batched: 144 buffers, 38 MB (32 MB used), last layer 28 buffers / 68 draws; render scale: 70%
- fly-vanilla: 13 GC collections, 108 ms in GC; renderer: Vanilla; regions: ; render scale: 100%
- fly-aetherium: 7 GC collections, 65 ms in GC; renderer: Aetherium pipeline, compatible mode (74 sections in the last layer, 26% of faces skipped; last frame 232 section layers in 119 draws, vanilla: 232; batched: 129 buffers, 32 MB (26 MB used), last layer 13 buffers / 18 draws); regions: ; batched: 129 buffers, 32 MB (26 MB used), last layer 13 buffers / 18 draws; render scale: 100%
- fly-new-off: 7 GC collections, 76 ms in GC; renderer: Aetherium pipeline, compatible mode (44 sections in the last layer, 27% of faces skipped; last frame 63 section layers in 49 draws, vanilla: 63; batched: 118 buffers, 51 MB (39 MB used), last layer 9 buffers / 37 draws); regions: ; batched: 118 buffers, 51 MB (39 MB used), last layer 9 buffers / 37 draws; render scale: 100%
- fly-aetherium-z: 9 GC collections, 100 ms in GC; renderer: Aetherium pipeline, compatible mode (38 sections in the last layer, 35% of faces skipped; last frame 171 section layers in 154 draws, vanilla: 171; batched: 123 buffers, 45 MB (33 MB used), last layer 7 buffers / 9 draws); regions: ; batched: 123 buffers, 45 MB (33 MB used), last layer 7 buffers / 9 draws; render scale: 100%
