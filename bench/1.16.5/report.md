# Aetherium auto benchmark (1.16.5)

Window 854x480, render distance 8, seed 7294105550031, GPU: Unknown, Java 17.0.20.1, 4 CPUs.

| Scenario | Avg FPS | 1% low FPS | p50 ms | p99 ms | p99.9 ms | Max ms | Frames > 50 ms | Frames > 100 ms |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| Vanilla renderer (Aetherium off) | 39.8 | 30.1 | 25.31 | 31.28 | 36.07 | 36.1 | 0 | 0 |
| Aetherium 1.9.0 (defaults) | 43.7 | 31.3 | 22.81 | 28.35 | 35.13 | 35.1 | 0 | 0 |
| Aetherium + render scale 70% | 49.0 | 30.5 | 20.28 | 27.23 | 46.16 | 46.2 | 0 | 0 |
| Flying into new terrain, Aetherium off | 53.7 | 25.8 | 17.28 | 36.60 | 40.32 | 41.9 | 0 | 0 |
| Flying into new terrain, Aetherium on | 53.4 | 26.4 | 17.70 | 34.58 | 40.53 | 41.6 | 0 | 0 |

## Notes

- vanilla: 2 GC collections, 11 ms in GC; renderer: Vanilla; regions: ; render scale: 100%
- aetherium: 3 GC collections, 18 ms in GC; renderer: Aetherium pipeline, compatible mode (110 sections in the last layer, 28% of faces skipped; last frame 324 section layers in 208 draws, vanilla: 324; batched: 169 buffers, 41 MB (33 MB used), last layer 34 buffers / 73 draws); regions: ; batched: 169 buffers, 41 MB (33 MB used), last layer 34 buffers / 73 draws; render scale: 100%
- scale70: 2 GC collections, 9 ms in GC; renderer: Aetherium pipeline, compatible mode (110 sections in the last layer, 28% of faces skipped; last frame 329 section layers in 203 draws, vanilla: 329; batched: 169 buffers, 41 MB (33 MB used), last layer 34 buffers / 73 draws); regions: ; batched: 169 buffers, 41 MB (33 MB used), last layer 34 buffers / 73 draws; render scale: 70%
- fly-vanilla: 9 GC collections, 50 ms in GC; renderer: Vanilla; regions: ; render scale: 100%
- fly-aetherium: 11 GC collections, 62 ms in GC; renderer: Aetherium pipeline, compatible mode (85 sections in the last layer, 25% of faces skipped; last frame 266 section layers in 135 draws, vanilla: 266; batched: 159 buffers, 42 MB (29 MB used), last layer 14 buffers / 18 draws); regions: ; batched: 159 buffers, 42 MB (29 MB used), last layer 14 buffers / 18 draws; render scale: 100%
