# Aetherium auto benchmark (1.16.5)

Window 854x480, render distance 8, seed 7294105550031, GPU: Unknown, Java 17.0.20.1, 4 CPUs.

| Scenario | Avg FPS | 1% low FPS | p50 ms | p99 ms | p99.9 ms | Max ms | Frames > 50 ms | Frames > 100 ms |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| Vanilla renderer (Aetherium off) | 22.5 | 16.6 | 44.66 | 56.54 | 65.90 | 65.9 | 28 | 0 |
| Aetherium 1.9.0 (defaults) | 24.1 | 19.2 | 41.25 | 50.45 | 53.24 | 53.2 | 6 | 0 |
| Aetherium + render scale 70% | 26.8 | 19.8 | 37.14 | 46.96 | 56.22 | 56.2 | 2 | 0 |
| Flying into new terrain, Aetherium off | 39.2 | 16.6 | 23.97 | 53.96 | 71.90 | 71.9 | 13 | 0 |
| Flying into new terrain, Aetherium on | 37.4 | 19.3 | 25.22 | 49.13 | 56.23 | 56.2 | 5 | 0 |

## Notes

- vanilla: 2 GC collections, 17 ms in GC; renderer: Vanilla; regions: ; render scale: 100%
- aetherium: 1 GC collections, 4 ms in GC; renderer: Aetherium pipeline, compatible mode (96 sections in the last layer, 30% of faces skipped; last frame 300 section layers in 199 draws, vanilla: 300; batched: 169 buffers, 43 MB (36 MB used), last layer 29 buffers / 62 draws); regions: ; batched: 169 buffers, 43 MB (36 MB used), last layer 29 buffers / 62 draws; render scale: 100%
- scale70: 1 GC collections, 4 ms in GC; renderer: Aetherium pipeline, compatible mode (96 sections in the last layer, 29% of faces skipped; last frame 289 section layers in 187 draws, vanilla: 289; batched: 168 buffers, 43 MB (36 MB used), last layer 29 buffers / 62 draws); regions: ; batched: 168 buffers, 43 MB (36 MB used), last layer 29 buffers / 62 draws; render scale: 70%
- fly-vanilla: 3 GC collections, 38 ms in GC; renderer: Vanilla; regions: ; render scale: 100%
- fly-aetherium: 5 GC collections, 52 ms in GC; renderer: Aetherium pipeline, compatible mode (63 sections in the last layer, 26% of faces skipped; last frame 195 section layers in 104 draws, vanilla: 195; batched: 122 buffers, 31 MB (20 MB used), last layer 9 buffers / 15 draws); regions: ; batched: 122 buffers, 31 MB (20 MB used), last layer 9 buffers / 15 draws; render scale: 100%
