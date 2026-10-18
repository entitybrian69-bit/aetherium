# Aetherium auto benchmark (1.16.5)

Window 854x480, render distance 8, seed 7294105550031, GPU: Unknown, Java 17.0.20.1, 4 CPUs.

| Scenario | Avg FPS | 1% low FPS | p50 ms | p99 ms | p99.9 ms | Max ms | Frames > 50 ms | Frames > 100 ms |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| Vanilla renderer (Aetherium off) | 23.4 | 16.4 | 42.19 | 57.46 | 67.45 | 67.5 | 27 | 0 |
| Aetherium 1.9.0 (defaults) | 24.5 | 17.3 | 40.09 | 54.54 | 60.22 | 60.2 | 22 | 0 |
| Aetherium + render scale 70% | 27.1 | 18.3 | 36.24 | 51.54 | 57.78 | 57.8 | 6 | 0 |
| Flying into new terrain, Aetherium off | 27.2 | 12.6 | 34.47 | 75.70 | 85.24 | 85.2 | 55 | 0 |
| Flying into new terrain, Aetherium on | 28.0 | 12.8 | 34.04 | 59.31 | 110.68 | 110.7 | 28 | 1 |

## Notes

- vanilla: 5 GC collections, 31 ms in GC; renderer: Vanilla; regions: ; render scale: 100%
- aetherium: 4 GC collections, 14 ms in GC; renderer: Aetherium pipeline, compatible mode (96 sections in the last layer, 30% of faces skipped; last frame 292 section layers in 189 draws, vanilla: 292; batched: 163 buffers, 40 MB (32 MB used), last layer 30 buffers / 63 draws); regions: ; batched: 163 buffers, 40 MB (32 MB used), last layer 30 buffers / 63 draws; render scale: 100%
- scale70: 5 GC collections, 17 ms in GC; renderer: Aetherium pipeline, compatible mode (94 sections in the last layer, 30% of faces skipped; last frame 289 section layers in 184 draws, vanilla: 289; batched: 164 buffers, 40 MB (32 MB used), last layer 30 buffers / 62 draws); regions: ; batched: 164 buffers, 40 MB (32 MB used), last layer 30 buffers / 62 draws; render scale: 70%
- fly-vanilla: 17 GC collections, 106 ms in GC; renderer: Vanilla; regions: ; render scale: 100%
- fly-aetherium: 31 GC collections, 135 ms in GC; renderer: Aetherium pipeline, compatible mode (77 sections in the last layer, 25% of faces skipped; last frame 232 section layers in 127 draws, vanilla: 232; batched: 155 buffers, 40 MB (26 MB used), last layer 12 buffers / 19 draws); regions: ; batched: 155 buffers, 40 MB (26 MB used), last layer 12 buffers / 19 draws; render scale: 100%
