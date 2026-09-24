# Architecture Experiments

EfficientNetB3 won on our split. These are the next architectures worth trying, with the
on-device constraint kept in view.

## Candidates

- **EfficientNetV2-S / newer ConvNeXt.** Stronger backbones than B3, still convolutional and
  Core ML / TFLite friendly. Likely the safest accuracy win.
- **Vision Transformers (ViT / DeiT).** Can beat CNNs with enough data, but ~3,300 dishes is
  small for a ViT from scratch. Only viable pretrained and fine-tuned, and heavier to
  deploy. Treat as a research branch, not a shipping candidate yet.
- **MobileViT / EfficientFormer.** Purpose-built for phones. Worth testing as the *deployed*
  model if a bigger backbone wins accuracy but is too slow on-device.

## Multi-task head design

We regress five correlated targets. Options:

- **Shared trunk, five heads** (current-style). Simple, strong baseline.
- **Uncertainty-weighted multi-task loss** (learn per-target loss weights) so calories does
  not dominate the gradient over carbohydrate.
- **Physics-consistency term:** softly penalize predictions where implied macros and
  calories disagree (≈4 kcal/g carb and protein, ≈9 kcal/g fat). Encodes real nutrition
  structure the raw targets already follow.

## Ensembling

Averaging the three existing models (EfficientNetB3, EfficientNetV2B0, Residual CNN)
typically beats any single one. Ship a single distilled student that mimics the ensemble to
keep on-device cost flat.

## Rules of engagement

- Same split/seed, same MAE-with-CI reporting.
- A candidate only "wins" if it improves carbohydrate MAE with non-overlapping CIs **and**
  converts cleanly to the on-device runtime within the latency budget.
