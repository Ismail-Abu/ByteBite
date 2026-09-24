# Depth-Informed Portion Estimation

**The single highest-value improvement.** Nutrition5k provides overhead depth maps aligned
with the RGB images, and we currently train on RGB alone. Depth encodes food volume, and
volume is what a flat photo cannot see. Mass estimation is the bottleneck, and every macro
(carbs, protein, fat) scales with portion size, so better mass propagates everywhere.

## Approaches, cheapest first

1. **RGB-D early fusion.** Stack depth as a fourth input channel and adapt the first conv
   layer to accept 4 channels (average the pretrained RGB filters to initialize the depth
   channel). Minimal architecture change.
2. **Two-stream late fusion.** Separate encoders for RGB and depth, concatenate features
   before the regression head. More capacity, more parameters.
3. **Explicit volume feature.** Integrate depth over the segmented plate region to compute a
   crude volume, and feed that scalar alongside CNN features. Interpretable and cheap.

## Deployment reality

Phones do not all have depth cameras, and the model must still work from a single RGB photo.
Plan for **graceful degradation**:

- Train a depth-using teacher, then distill into an RGB-only student, so the shipped model
  benefits from depth at training time without needing it at inference.
- Or predict depth from RGB with a lightweight monocular depth network, then feed it in.
  Adds latency; validate it actually beats RGB-only.

## Experiment plan

- Baseline: current RGB EfficientNetB3 MAE per nutrient.
- Variant A: RGB-D early fusion on the same split/seed.
- Variant B: RGB-only student distilled from an RGB-D teacher.
- Success: mass MAE drops meaningfully and carbohydrate MAE improves with it, CIs
  non-overlapping with baseline.
