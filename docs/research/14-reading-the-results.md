# 14. Reading the results

## The paper table

Mean absolute error per nutrient, with 95% bootstrap confidence intervals (1,000 resamples of pooled test predictions from five runs):

| Model | Calories (kcal) | Mass (g) | Carbs (g) | Protein (g) | Fat (g) |
|---|---|---|---|---|---|
| EfficientNetB3 | 42.02 (40.20-44.00) | 27.00 (26.00-28.06) | 4.12 (3.95-4.28) | 4.29 (4.03-4.54) | 3.17 (3.02-3.31) |
| EfficientNetV2B0 | 48.99 (46.58-51.31) | 31.84 (30.54-33.29) | 7.42 (7.14-7.69) | 6.72 (6.38-7.04) | 4.41 (4.20-4.61) |
| Residual CNN | 49.63 (47.38-51.86) | 34.02 (32.49-35.68) | 7.92 (7.64-8.22) | 7.09 (6.71-7.52) | 4.70 (4.50-4.91) |
| SnapNutrition | 101.89 (98.48-105.36) | 69.23 (66.64-71.70) | 10.47 (10.17-10.79) | 11.29 (10.84-11.72) | 8.33 (8.00-8.65) |

**How to read it.** "Carbs 4.12 g (3.95-4.28)" means: on unseen dishes, the carb estimate is off by 4.12 g on average, and if the experiment were repeated with slightly different test dishes, the average would likely land between 3.95 and 4.28.

**What it shows.**

- EfficientNetB3 wins on every target. Its carb interval (3.95-4.28) doesn't overlap V2B0's (7.14-7.69), so that gap is real, not noise.
- V2B0 and the Residual CNN overlap on calories (46.58-51.31 against 47.38-51.86), so they aren't clearly different there.
- Every model beats the SnapNutrition baseline on every target. EfficientNetB3 cuts its error by about 60% on all five.

## What a bootstrap CI is

A bootstrap estimates uncertainty by resampling. The test predictions (5 runs times the test dishes, pooled) are sampled with replacement 1,000 times. For each resample the MAE is computed, and the middle 95% of those 1,000 MAEs is the interval. It asks: if the test dishes had come out a bit differently, how much would the score move?

## Two kinds of interval in this repo

| | Paper table | v4 notebook, Cell 8 |
|---|---|---|
| Method | bootstrap over pooled test predictions | t-interval over the 5 per-seed MAEs |
| Captures | which dishes happen to be in test | seed-to-seed training variation |
| Width | narrower, since it's based on ~2,450 predictions | wider, since it's based on 5 numbers |

Neither is wrong. They answer different questions. The bootstrap treats the pooled predictions as the sample. Because predictions from the same dish across five seeds are correlated, it probably understates uncertainty somewhat. The t-interval captures training randomness but has only four degrees of freedom. When citing a CI, say which kind it is.

## The baseline

SnapNutrition is a public transfer-learning model trained on the same dataset (Yeramosu, AC215 project). A baseline answers "compared to what?" Without one, 4.12 g of carb error is just a number. With one, it's 60% lower error than an existing public approach on the same data.

## Where the numbers disagree (open)

The per-nutrient MAEs in the paper table don't match the per-nutrient 5-seed means in `bytebite_v4_walkthrough.md`:

| | Calories | Mass | Fat | Carbs | Protein |
|---|---|---|---|---|---|
| Paper, EfficientNetB3 | 42.02 | 27.00 | 3.17 | 4.12 | 4.29 |
| v4 walkthrough, 5-seed mean | 49.32 | 28.88 | 3.83 | 4.33 | 4.32 |

MAE over pooled predictions from five runs equals the mean of the five per-run MAEs. So if these were the same five models on the same test dishes, the two rows would match. They don't, so something differs. The repo doesn't say what. What the files do show:

- The two use different splits. The walkthrough's code builds a 3,260-dish split (test 489, and it asserts that). The paper uses 3,259 dishes (test 490). The export notebook expects `v4_` models on the 3,259 split ([04](04-dataset.md)).
- One changed dish in 489 can't account for a 7 kcal gap by itself. The paper's EfficientNetB3 runs were therefore most likely trained separately from the five seeds in the walkthrough, on the 3,259 split.

**That is a hypothesis.** Before quoting either table outside this repo, confirm which runs produced the paper table and note the split next to the number. Until then, the safe statements are "EfficientNetB3 beats the other two models and the baseline on every target" and "v4 improved on v3-FT by 3.45 overall MAE on the same split". Both hold under either reading.

## The v4 caveat

v4 changed resolution, augmentation and unfreeze depth together. The 3.45 MAE gain over v3-FT belongs to the combination. The walkthrough says so directly: a per-change ablation would be needed to split the credit. The ablation would be four more 5-seed runs (each change alone, plus each one left out), several GPU-hours each.

## Check yourself

<details>
<summary>Two models' 95% CIs overlap slightly. Are they definitely the same?</summary>

No. Overlapping intervals don't prove equality, and a slight overlap can still hide a real difference. A direct test on the paired difference (same dishes, both models) is more sensitive than comparing two separate intervals.
</details>

<details>
<summary>Why would a bootstrap over pooled predictions from 5 seeds understate uncertainty?</summary>

The five predictions for a single dish are strongly correlated, since it's the same photo and a similar model. Resampling them as if they were independent treats about 2,450 predictions as 2,450 independent pieces of evidence, when the real number of independent dishes is about 490.
</details>
