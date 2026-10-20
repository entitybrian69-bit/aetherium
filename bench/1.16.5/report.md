# Aetherium auto benchmark (1.16.5)

Window 854x480, render distance 8, seed 7294105550031, GPU: Unknown, Java 17.0.20.1, 4 CPUs.

| Scenario | Avg FPS | 1% low FPS | p50 ms | p99 ms | p99.9 ms | Max ms | Frames > 50 ms | Frames > 100 ms |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| Vanilla (Aetherium off) | 23.9 | 17.7 | 41.77 | 54.45 | 57.88 | 57.9 | 24 | 0 |
| Aetherium (defaults) | 26.1 | 17.7 | 37.90 | 52.72 | 58.28 | 58.3 | 12 | 0 |
| Aetherium, all 1.12.0 options off | 26.1 | 18.6 | 38.00 | 49.02 | 60.63 | 60.6 | 5 | 0 |
| Aetherium, directional culling + view-first building off | 26.5 | 20.4 | 37.56 | 47.76 | 52.33 | 52.3 | 1 | 0 |
| Aetherium, GL call trimming + cheap damage tint off | 27.2 | 20.7 | 36.58 | 46.30 | 52.77 | 52.8 | 1 | 0 |
| Aetherium, server lighting + server idle off | 26.4 | 18.1 | 37.19 | 53.40 | 56.55 | 56.6 | 16 | 0 |
| Aetherium, light table rate + AO cache off | 26.7 | 19.6 | 37.09 | 48.88 | 52.33 | 52.3 | 4 | 0 |
| Aetherium (defaults), measured again | 26.9 | 18.8 | 36.72 | 49.37 | 55.52 | 55.5 | 5 | 0 |
| Aetherium + render scale 70% | 29.7 | 22.2 | 33.27 | 42.11 | 49.90 | 49.9 | 0 | 0 |
| Flying into new terrain (+X), Aetherium off | 39.9 | 16.6 | 23.18 | 56.39 | 69.85 | 69.9 | 15 | 0 |
| Flying into new terrain (-X), Aetherium defaults | 30.3 | 16.8 | 31.43 | 55.62 | 64.35 | 64.3 | 23 | 0 |
| Flying into new terrain (+Z), all 1.12.0 options off | 33.9 | 19.9 | 28.40 | 46.08 | 57.13 | 57.1 | 2 | 0 |
| Flying into new terrain (-Z), Aetherium defaults | 38.0 | 17.3 | 24.19 | 53.84 | 69.30 | 69.3 | 16 | 0 |

## Notes

- vanilla: 3 GC collections, 16 ms in GC; renderer: Vanilla; regions: ; render scale: 100%
- aetherium: 3 GC collections, 14 ms in GC; renderer: Aetherium pipeline, compatible mode (92 sections in the last layer, 30% of faces skipped; last frame 281 section layers in 180 draws, vanilla: 281; batched: 164 buffers, 40 MB (33 MB used), last layer 30 buffers / 61 draws); regions: ; batched: 164 buffers, 40 MB (33 MB used), last layer 30 buffers / 61 draws; render scale: 100%
- new-off: 3 GC collections, 12 ms in GC; renderer: Aetherium pipeline, compatible mode (92 sections in the last layer, 30% of faces skipped; last frame 281 section layers in 178 draws, vanilla: 281; batched: 165 buffers, 40 MB (33 MB used), last layer 30 buffers / 61 draws); regions: ; batched: 165 buffers, 40 MB (33 MB used), last layer 30 buffers / 61 draws; render scale: 100%
- no-cull: 3 GC collections, 19 ms in GC; renderer: Aetherium pipeline, compatible mode (92 sections in the last layer, 30% of faces skipped; last frame 281 section layers in 178 draws, vanilla: 281; batched: 163 buffers, 39 MB (33 MB used), last layer 30 buffers / 61 draws); regions: ; batched: 163 buffers, 39 MB (33 MB used), last layer 30 buffers / 61 draws; render scale: 100%
- no-gl: 3 GC collections, 10 ms in GC; renderer: Aetherium pipeline, compatible mode (88 sections in the last layer, 30% of faces skipped; last frame 282 section layers in 183 draws, vanilla: 282; batched: 163 buffers, 39 MB (33 MB used), last layer 30 buffers / 58 draws); regions: ; batched: 163 buffers, 39 MB (33 MB used), last layer 30 buffers / 58 draws; render scale: 100%
- no-server: 3 GC collections, 9 ms in GC; renderer: Aetherium pipeline, compatible mode (92 sections in the last layer, 30% of faces skipped; last frame 281 section layers in 178 draws, vanilla: 281; batched: 164 buffers, 40 MB (33 MB used), last layer 30 buffers / 61 draws); regions: ; batched: 164 buffers, 40 MB (33 MB used), last layer 30 buffers / 61 draws; render scale: 100%
- no-light: 3 GC collections, 16 ms in GC; renderer: Aetherium pipeline, compatible mode (92 sections in the last layer, 30% of faces skipped; last frame 281 section layers in 178 draws, vanilla: 281; batched: 164 buffers, 40 MB (33 MB used), last layer 30 buffers / 61 draws); regions: ; batched: 164 buffers, 40 MB (33 MB used), last layer 30 buffers / 61 draws; render scale: 100%
- aetherium-again: 3 GC collections, 13 ms in GC; renderer: Aetherium pipeline, compatible mode (90 sections in the last layer, 30% of faces skipped; last frame 281 section layers in 178 draws, vanilla: 281; batched: 163 buffers, 39 MB (33 MB used), last layer 30 buffers / 59 draws); regions: ; batched: 163 buffers, 39 MB (33 MB used), last layer 30 buffers / 59 draws; render scale: 100%
- scale70: 3 GC collections, 14 ms in GC; renderer: Aetherium pipeline, compatible mode (92 sections in the last layer, 30% of faces skipped; last frame 281 section layers in 178 draws, vanilla: 281; batched: 165 buffers, 40 MB (33 MB used), last layer 30 buffers / 61 draws); regions: ; batched: 165 buffers, 40 MB (33 MB used), last layer 30 buffers / 61 draws; render scale: 70%
- fly-vanilla: 9 GC collections, 80 ms in GC; renderer: Vanilla; regions: ; render scale: 100%
- fly-aetherium: 15 GC collections, 82 ms in GC; renderer: Aetherium pipeline, compatible mode (71 sections in the last layer, 25% of faces skipped; last frame 217 section layers in 119 draws, vanilla: 217; batched: 160 buffers, 42 MB (26 MB used), last layer 14 buffers / 21 draws); regions: ; batched: 160 buffers, 42 MB (26 MB used), last layer 14 buffers / 21 draws; render scale: 100%
- fly-new-off: 15 GC collections, 79 ms in GC; renderer: Aetherium pipeline, compatible mode (53 sections in the last layer, 28% of faces skipped; last frame 90 section layers in 80 draws, vanilla: 90; batched: 120 buffers, 51 MB (41 MB used), last layer 16 buffers / 55 draws); regions: ; batched: 120 buffers, 51 MB (41 MB used), last layer 16 buffers / 55 draws; render scale: 100%
- fly-aetherium-z: 27 GC collections, 128 ms in GC; renderer: Aetherium pipeline, compatible mode (40 sections in the last layer, 37% of faces skipped; last frame 165 section layers in 173 draws, vanilla: 165; batched: 117 buffers, 43 MB (31 MB used), last layer 5 buffers / 6 draws); regions: ; batched: 117 buffers, 43 MB (31 MB used), last layer 5 buffers / 6 draws; render scale: 100%
