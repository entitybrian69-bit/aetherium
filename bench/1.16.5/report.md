# Aetherium auto benchmark (1.16.5)

Window 854x480, render distance 8, seed 7294105550031, GPU: Unknown, Java 17.0.20.1, 4 CPUs.

| Scenario | Avg FPS | 1% low FPS | p50 ms | p99 ms | p99.9 ms | Max ms | Frames > 50 ms | Frames > 100 ms |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| Vanilla (Aetherium off) | 19.4 | 14.1 | 51.37 | 65.77 | 77.74 | 77.7 | 231 | 0 |
| Aetherium (defaults) | 21.2 | 14.9 | 47.09 | 58.54 | 75.31 | 75.3 | 101 | 0 |
| Aetherium, all 1.12.0 options off | 21.3 | 17.2 | 46.72 | 56.30 | 59.93 | 59.9 | 104 | 0 |
| Aetherium, directional culling + view-first building off | 21.1 | 16.2 | 47.28 | 59.87 | 64.67 | 64.7 | 118 | 0 |
| Aetherium, GL call trimming + cheap damage tint off | 21.5 | 16.5 | 45.98 | 56.38 | 67.48 | 67.5 | 88 | 0 |
| Aetherium, server lighting + server idle off | 21.7 | 17.5 | 45.55 | 55.66 | 57.93 | 57.9 | 88 | 0 |
| Aetherium, light table rate + AO cache off | 21.4 | 18.1 | 46.67 | 54.31 | 56.74 | 56.7 | 93 | 0 |
| Aetherium (defaults), measured again | 21.4 | 15.4 | 46.32 | 60.50 | 68.10 | 68.1 | 100 | 0 |
| Aetherium + render scale 70% | 23.7 | 18.2 | 41.79 | 52.85 | 55.62 | 55.6 | 16 | 0 |
| Flying into new terrain (+X), Aetherium off | 34.0 | 14.6 | 27.35 | 64.98 | 74.92 | 74.9 | 30 | 0 |
| Flying into new terrain (-X), Aetherium defaults | 24.5 | 14.2 | 38.50 | 65.42 | 77.67 | 77.7 | 82 | 0 |
| Flying into new terrain (+Z), all 1.12.0 options off | 27.7 | 16.0 | 34.97 | 56.35 | 65.97 | 66.0 | 22 | 0 |
| Flying into new terrain (-Z), Aetherium defaults | 30.7 | 15.5 | 30.41 | 59.98 | 68.38 | 68.4 | 30 | 0 |

## Notes

- vanilla: 2 GC collections, 17 ms in GC; renderer: Vanilla; regions: ; render scale: 100%
- aetherium: 2 GC collections, 17 ms in GC; renderer: Aetherium pipeline, compatible mode (94 sections in the last layer, 30% of faces skipped; last frame 286 section layers in 179 draws, vanilla: 286; batched: 165 buffers, 40 MB (33 MB used), last layer 30 buffers / 60 draws); regions: ; batched: 165 buffers, 40 MB (33 MB used), last layer 30 buffers / 60 draws; render scale: 100%
- new-off: 2 GC collections, 8 ms in GC; renderer: Aetherium pipeline, compatible mode (94 sections in the last layer, 31% of faces skipped; last frame 286 section layers in 178 draws, vanilla: 286; batched: 164 buffers, 40 MB (33 MB used), last layer 30 buffers / 60 draws); regions: ; batched: 164 buffers, 40 MB (33 MB used), last layer 30 buffers / 60 draws; render scale: 100%
- no-cull: 3 GC collections, 18 ms in GC; renderer: Aetherium pipeline, compatible mode (95 sections in the last layer, 30% of faces skipped; last frame 290 section layers in 184 draws, vanilla: 290; batched: 165 buffers, 40 MB (33 MB used), last layer 30 buffers / 60 draws); regions: ; batched: 165 buffers, 40 MB (33 MB used), last layer 30 buffers / 60 draws; render scale: 100%
- no-gl: 3 GC collections, 11 ms in GC; renderer: Aetherium pipeline, compatible mode (95 sections in the last layer, 30% of faces skipped; last frame 290 section layers in 184 draws, vanilla: 290; batched: 164 buffers, 40 MB (33 MB used), last layer 30 buffers / 60 draws); regions: ; batched: 164 buffers, 40 MB (33 MB used), last layer 30 buffers / 60 draws; render scale: 100%
- no-server: 3 GC collections, 11 ms in GC; renderer: Aetherium pipeline, compatible mode (94 sections in the last layer, 31% of faces skipped; last frame 286 section layers in 180 draws, vanilla: 286; batched: 164 buffers, 40 MB (33 MB used), last layer 30 buffers / 61 draws); regions: ; batched: 164 buffers, 40 MB (33 MB used), last layer 30 buffers / 61 draws; render scale: 100%
- no-light: 3 GC collections, 11 ms in GC; renderer: Aetherium pipeline, compatible mode (95 sections in the last layer, 30% of faces skipped; last frame 290 section layers in 184 draws, vanilla: 290; batched: 165 buffers, 40 MB (33 MB used), last layer 30 buffers / 60 draws); regions: ; batched: 165 buffers, 40 MB (33 MB used), last layer 30 buffers / 60 draws; render scale: 100%
- aetherium-again: 3 GC collections, 11 ms in GC; renderer: Aetherium pipeline, compatible mode (94 sections in the last layer, 30% of faces skipped; last frame 284 section layers in 177 draws, vanilla: 284; batched: 164 buffers, 40 MB (33 MB used), last layer 30 buffers / 60 draws); regions: ; batched: 164 buffers, 40 MB (33 MB used), last layer 30 buffers / 60 draws; render scale: 100%
- scale70: 3 GC collections, 15 ms in GC; renderer: Aetherium pipeline, compatible mode (91 sections in the last layer, 30% of faces skipped; last frame 285 section layers in 176 draws, vanilla: 285; batched: 164 buffers, 40 MB (33 MB used), last layer 30 buffers / 59 draws); regions: ; batched: 164 buffers, 40 MB (33 MB used), last layer 30 buffers / 59 draws; render scale: 70%
- fly-vanilla: 9 GC collections, 101 ms in GC; renderer: Vanilla; regions: ; render scale: 100%
- fly-aetherium: 15 GC collections, 110 ms in GC; renderer: Aetherium pipeline, compatible mode (68 sections in the last layer, 24% of faces skipped; last frame 203 section layers in 105 draws, vanilla: 203; batched: 155 buffers, 39 MB (25 MB used), last layer 11 buffers / 17 draws); regions: ; batched: 155 buffers, 39 MB (25 MB used), last layer 11 buffers / 17 draws; render scale: 100%
- fly-new-off: 15 GC collections, 101 ms in GC; renderer: Aetherium pipeline, compatible mode (41 sections in the last layer, 28% of faces skipped; last frame 67 section layers in 64 draws, vanilla: 67; batched: 114 buffers, 48 MB (37 MB used), last layer 13 buffers / 40 draws); regions: ; batched: 114 buffers, 48 MB (37 MB used), last layer 13 buffers / 40 draws; render scale: 100%
- fly-aetherium-z: 24 GC collections, 143 ms in GC; renderer: Aetherium pipeline, compatible mode (41 sections in the last layer, 37% of faces skipped; last frame 168 section layers in 176 draws, vanilla: 168; batched: 115 buffers, 42 MB (30 MB used), last layer 6 buffers / 7 draws); regions: ; batched: 115 buffers, 42 MB (30 MB used), last layer 6 buffers / 7 draws; render scale: 100%
