# 07. Model choice

## The three candidates

| Model | Backbone | Input | Head |
|---|---|---|---|
| EfficientNetB3 | pretrained on ImageNet | 300x300 | GAP, Dense 256 ReLU, Dropout 0.3, Dense 5 |
| EfficientNetV2B0 | pretrained on ImageNet | 180x180 | GAP, Dropout 0.3, Dense 256 ReLU, Dense 5 |
| Residual CNN | trained from scratch | 180x180 | Flatten, Dropout 0.4, Dense 256, Dropout 0.3, Dense 128, Dense 5 |

All three do the same job: one RGB image in, five numbers out. They were trained with the same loss (Huber), optimizer (Adam) and standardized targets, five runs each, and scored on the same test dishes.

## Why transfer learning wins here

The training set is about 2,300 images. That's very small for learning vision from scratch. ImageNet has 1.2 million images, and a network trained on it has already learned what edges, textures, shapes, glossy surfaces and granular piles look like. Those features are general. The early layers of any good vision network look much alike whatever they were trained on.

Starting from ImageNet weights means the 2,300 food images only have to teach the last step: how those visual features map to grams of carbohydrate. A network from scratch has to learn everything, including what an edge is, from 2,300 examples. It will either underfit, or memorize the training plates and overfit.

The results show it. Both pretrained models beat the scratch model on every target, and the paper draws the same conclusion.

| Carb MAE (g) | |
|---|---|
| EfficientNetB3, pretrained | 4.12 |
| EfficientNetV2B0, pretrained | 7.42 |
| Residual CNN, scratch | 7.92 |
| SnapNutrition baseline | 10.47 |

## Why the Residual CNN was still worth training

It's the control. Without a scratch model, "transfer learning helps" is an assumption. With one, it's a measured result. Its architecture is a sensible from-scratch design: five conv blocks (32 up to 512 filters), batch norm, max pooling, and skip connections in the deeper blocks so gradients flow. Heavier dropout (0.4, then 0.3) fights overfitting on a small dataset.

## Why EfficientNetB3 beat EfficientNetV2B0

Both are pretrained, so the gap comes from elsewhere. The paper points to two differences:

1. **Resolution: 300 px against 180 px.** 300x300 has about 2.8x as many pixels. Portion size and food texture are fine-grained cues. The difference between a spoon of rice and two spoons is a few dozen pixels at 180 px. See [10](10-resolution-and-augmentation.md).
2. **Orientation augmentation.** The B3 line trained with random flips and 90-degree rotations, which is free, valid extra data for overhead photos.

EfficientNet itself uses "compound scaling": B0 through B7 grow depth, width and input resolution together, and B3's native input is 300 px. Running it at its native size means its pretrained features see objects at the scale they were trained on.

One caveat, the same one the walkthrough makes: resolution and augmentation changed together, so their separate credit isn't measured. The paper says they "appear to" explain the gap. That's the honest wording.

## Why not the smaller model for the phone

EfficientNetV2B0 at 180 px would be about 7 MB on the phone, against 22.3 MB for B3 at float16. It was rejected on the paper's own numbers: carb MAE 7.42 g against 4.12 g. That's 80% worse on the target that drives insulin dosing, for a smaller download. In a diabetes tool that's a bad trade ([03](03-problem.md)).

## The head, piece by piece

- **Global average pooling (GAP)** averages each feature channel over the whole image, giving one vector per plate. It has no weights, which means fewer parameters to overfit, and it ignores where food sits on the plate. That's what you want, since position shouldn't change the nutrition.
- **Dense 256 with ReLU** is a small learned layer that mixes features into nutrition-relevant combinations.
- **Dropout 0.3** randomly zeroes 30% of those 256 values during training, so the head can't lean on a few features. It's regularization for a small dataset, and it switches off at inference.
- **Dense 5** has no activation, because the targets are z-scores and can be negative. A ReLU here would make "below average" impossible.

## Check yourself

<details>
<summary>Why is the last layer linear instead of ReLU, when nutrition can't be negative?</summary>

The head predicts z-scores, which are negative for any dish below the train-set average. A ReLU would clamp all of those to the average. Non-negativity is enforced after converting back to grams (the app floors negatives at 0).
</details>

<details>
<summary>What does the Residual CNN prove that the other two can't?</summary>

That pretraining is what helps. With the same data, loss and targets, the only big difference is ImageNet initialization, and the scratch model loses on every target.
</details>
