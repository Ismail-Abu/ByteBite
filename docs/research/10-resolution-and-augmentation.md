# 10. Resolution and augmentation

## Resolution: 192 to 300 pixels

v3-FT ran at 192x192. v4 runs at 300x300, EfficientNetB3's native resolution. That's about 2.4 times as many pixels.

**Why resolution matters for this task.** Classifying food type ("is this pasta?") works at low resolution, since shape and color survive downscaling. Estimating nutrition needs finer cues:

- **Portion size.** How high is the rice piled, and how much plate shows around the edges? The difference between 150 g and 200 g can be a few dozen pixels at 192 px.
- **Texture.** Oily and dry, fried and steamed, cream sauce and tomato sauce. These live in small highlights and grain that downscaling blurs away.
- **Small, calorie-dense items.** A pat of butter, a drizzle of dressing, or a few nuts can carry a lot of fat in a small area.

**Why native resolution in particular.** EfficientNet's compound scaling designs each model size around one input resolution. At 300 px, B3's pretrained filters see food at roughly the scale ImageNet objects appeared during pretraining, so the features fit better.

**The cost.** More pixels mean more memory and slower training (batch size 16, and several hours for five seeds on an L4). On the phone, 300x300 makes B3 the heaviest of the three paper models. The paper numbers say the accuracy was worth it.

## Augmentation: flips and 90-degree rotations

Every training image, every epoch, gets:

```python
img = tf.image.random_flip_left_right(img)
img = tf.image.random_flip_up_down(img)
k = tf.random.uniform([], 0, 4, dtype=tf.int32)
img = tf.image.rot90(img, k)          # 0, 90, 180 or 270 degrees
```

That's 8 distinct orientations of each plate (the symmetry group of a square). The model rarely sees the exact same picture twice.

**Why these augmentations are valid.** An augmentation is valid only if it doesn't change the correct answer. For overhead food photos:

- There's no natural "up". A plate photographed from above looks equally natural rotated 90 degrees or mirrored.
- Rotating or flipping the image changes neither the food nor its nutrition.

So each augmented image is a genuinely new, correctly labelled example. The walkthrough calls it free data.

**Why not other augmentations?** Some common ones would change the label or break the pipeline:

| Augmentation | Problem here |
|---|---|
| Random crop or zoom | Changes apparent portion size, which is exactly what mass and calories are read from. A zoomed plate looks bigger. |
| Arbitrary-angle rotation (for example 17 degrees) | Leaves blank corners, and on a 4:3 frame stretched to a square it distorts the geometry the phone must reproduce. |
| Strong color jitter | Color carries information (browned against pale, creamy against tomato-based). Heavy jitter can erase it. |

Flips and right-angle rotations are the set that's clearly label-preserving and geometry-preserving.

**Evaluation is never augmented.** Validation and test images are only decoded and resized, in fixed order, so each prediction lines up with its label.

## A benefit that shows up on the phone

Training on all four rotations made the model orientation-invariant. That paid off in the app: a portrait phone photo can simply be rotated 90 degrees to landscape, keeping the whole plate, instead of being cropped to landscape and losing nearly half of it. See [02](02-final-pipeline.md).

## Check yourself

<details>
<summary>Why is random cropping, the most standard augmentation in vision, a bad idea here?</summary>

The model estimates quantity from how much of the frame the food fills. Cropping or zooming changes that without changing the label, which teaches the model that apparent size is unreliable, and apparent size is its main signal for mass.
</details>

<details>
<summary>How many distinct orientations can the augmentation produce, and why that number?</summary>

Eight: four rotations (0, 90, 180, 270) times flipped or not. Combining left-right and up-down flips with the rotations doesn't create more, because those combinations are already in the set of eight symmetries of a square.
</details>
