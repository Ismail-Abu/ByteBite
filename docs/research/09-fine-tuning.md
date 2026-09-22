# 09. Fine-tuning

## Frozen, then partly unfrozen

A pretrained network can be used three ways:

1. **Frozen.** The backbone is a fixed feature extractor, and only the new head trains. This is v1 (test MAE 29.57). It's fast and safe, but the features are tuned for telling dogs from cars, not for judging portion size.
2. **Partly unfrozen.** The top of the backbone trains too, so its high-level features can specialize to food. This is v3-FT (block 6 and up, 21.59) and v4 (blocks 4-7 and the top conv, 18.14).
3. **Fully unfrozen.** Everything trains. With about 2,300 images, this risks destroying useful low-level features for very little gain.

## Which layers, and why

EfficientNetB3 is a stack of blocks. Early blocks detect simple things like edges, color gradients and small textures. Later blocks combine them into complex patterns like "piled granular food" or "glossy sauce".

- **Blocks 1-3 stay frozen.** Edges and textures transfer from ImageNet almost perfectly. Retraining them on 2,300 images would add noise, not knowledge.
- **Blocks 4-7 and the top conv train (v4).** This is where the network decides what matters. ImageNet taught it to care about object identity. Here it needs to care about volume, density and food type.

v3-FT only opened block 6 and up. v4 opened more (block 4 and up), which gives the model more freedom to adapt. That's only safe with the lower learning rate below.

## Why the learning rate is 3e-5

The learning rate sets how far each update moves the weights. Pretrained weights are valuable because they already sit somewhere good. A large learning rate can knock them out of that region in a few hundred steps. This is often called catastrophic forgetting: the ImageNet knowledge is overwritten before the food knowledge is learned.

The walkthrough puts the rule simply: the more pretrained layers you let move, the gentler the updates must be. v4 unfroze more layers than v3-FT, so it trains at 3e-5. That's about 33 times smaller than the textbook Adam default of 1e-3.

## BatchNorm stays frozen

This is the least obvious choice in the model, and it matters a lot.

**What BatchNorm does.** Each BatchNorm layer normalizes its inputs using a mean and variance. During training it uses the current batch's statistics. At inference it uses running averages collected during pretraining. It also has two learned parameters (scale and shift).

**Why that's a problem here.** With a batch size of 16:

- The batch statistics are noisy. Sixteen images of food make a poor estimate of the mean and variance of all food.
- If BatchNorm is allowed to update, those noisy statistics overwrite the stable ImageNet running averages.
- Training then sees one normalization and inference sees another, so the model effectively changes between train time and test time.

**What the code does.** Two things, and both are needed:

```python
if isinstance(lyr, layers.BatchNormalization):
    unf = False                      # BN parameters don't train
...
h = backbone(inp, training=False)    # BN uses its ImageNet running stats
```

- Setting each BatchNorm layer to non-trainable stops its scale and shift from updating.
- Calling the backbone with `training=False` makes BatchNorm use its stored running statistics even during training, instead of the batch's. (Dropout in the backbone is also held at inference behavior. The head's own dropout still trains normally, because it lives outside the backbone call.)

Gradients still flow through BatchNorm into the unfrozen conv weights. The convolutions learn, and the normalization stays fixed. This is the standard Keras fine-tuning recipe, and skipping it is one of the most common ways fine-tuning quietly fails.

## Why v3 to v4 is a package

v4 changed three things at once: resolution (192 to 300), augmentation (none to flips and rotations), and unfreeze depth plus learning rate. They interact:

- More unfrozen layers need more varied data, or they overfit. Augmentation supplies that.
- Higher resolution means more detail for those layers to learn from.
- A deeper unfreeze needs a lower learning rate to stay stable.

That's a good reason to change them together. It's also why the credit can't be split: no ablation ran one change at a time. See [14](14-reading-the-results.md).

## Check yourself

<details>
<summary>You unfreeze all of BatchNorm and train with batch size 16. What do you expect?</summary>

Unstable training and a train/inference mismatch. The noisy 16-image statistics replace the ImageNet running averages, validation loss jumps around, and the final model does worse than the frozen-BN version, even though more weights are trainable.
</details>

<details>
<summary>Why leave blocks 1-3 frozen instead of training everything at a small learning rate?</summary>

Their features (edges, color, texture) already transfer well, and training them on 2,300 images adds overfitting risk and compute for almost no gain. Freezing them also cuts memory, which helps fit 300 px batches on the GPU.
</details>
