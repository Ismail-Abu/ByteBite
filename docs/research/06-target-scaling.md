# 06. Target scaling

## The problem with raw units

The five targets live on very different scales. From the v1 error numbers alone: calorie errors are around 80 kcal, while fat errors are around 6 g. The targets themselves differ in the same way. A typical dish is hundreds of kcal and hundreds of grams, but only tens of grams of fat, carbs or protein.

The loss adds the five errors together. In raw units, being 50 kcal off contributes far more than being 5 g of fat off, even though a 5 g fat error may matter more to a patient. The gradients would mostly teach calories and mass, and the small targets would get whatever is left.

## The fix: z-scores

Each target is standardized before training:

```
z = (y - mu) / sd
```

where `mu` and `sd` are that target's mean and standard deviation. After this, every target has mean 0 and standard deviation 1. An error of 1.0 means "one standard deviation off" for all five, so each target pulls on the shared weights with comparable force.

The network's last layer, `Dense(5)`, outputs five z-scores. To get grams and kcal back:

```
y = z * sd + mu
```

Every reported number is converted back to real units first. The training-curve figure in the README is the one exception, and its caption says it's in standardized units.

## Why train-only statistics

`mu` and `sd` are computed on the **train split only**:

```python
scaler = TargetScaler().fit(y_train_raw)   # TRAIN ONLY - no leakage
```

Computing them over all dishes would be a small leak. The validation and test labels would shape the numbers the model is trained against. The effect on MAE is small, but the rule is simple and absolute: nothing about validation or test labels may touch training. Small leaks are how pipelines drift into reporting numbers they didn't earn.

## The guard rails

- **Zero variance.** If a target had `sd == 0`, dividing by it would give infinity. The scaler replaces a zero `sd` with 1. It never triggers on this data, but it costs one line.
- **Round trip.** Right after fitting, the notebook checks that `inverse(transform(y))` gives back `y` to within 0.001. That catches a transposed array or a swapped column before it can quietly corrupt a whole run.

## Why this matters on the phone

The phone has to undo exactly the same transform. `mu` and `sd` are written into `bytebite_model.json` next to the model, index-aligned with the target order `[calories, mass, fat, carb, protein]`. `NutritionEstimator.kt` applies one line: `real = z * sd + mu`.

The earlier `model1_control` stand-in model was trained on raw kcal and grams, with no standardization. Rather than special-casing it, the export notebook writes `mu = 0` and `sd = 1` for a raw head, so the same line becomes the identity. The notebook also measures which reading matches the labels instead of assuming, since the two readings differ by orders of magnitude.

## Why Huber pairs with this

The loss is Huber ([08](08-loss-and-training.md)), and Huber has a threshold (delta, default 1.0) where it switches from squared to linear error. In z-units, "1.0" means one standard deviation for every target, so a single threshold makes sense across all five. In raw units, delta 1.0 would mean 1 kcal for calories and 1 g for fat, a meaningless threshold for one or the other.

## Check yourself

<details>
<summary>The model outputs z = 0.5 for carbs. What does that mean before you convert it?</summary>

The dish is predicted to have half a train-set standard deviation more carbohydrate than the average training dish. Converting with carb `mu` and `sd` turns it into grams.
</details>

<details>
<summary>Why does standardization make Huber's delta meaningful?</summary>

Delta is a fixed error size where Huber switches from quadratic to linear. After z-scoring, an error of 1 means one standard deviation for every target, so one delta fits all five. In raw units, delta 1.0 would mean 1 kcal for one target and 1 g for another.
</details>
