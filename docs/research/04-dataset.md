# 04. The dataset

## What Nutrition5k is

Nutrition5k (Thames et al., 2021) is a set of real cafeteria dishes from two sites, weighed and labelled ingredient by ingredient, so the nutrition labels are far more accurate than crowd-sourced food photos. The full release is about 180 GB: overhead RGB images, overhead depth images, rotating side-angle videos, and CSV metadata with dish IDs and totals. It has 5,006 overhead RGB dish entries.

ByteBite uses only the overhead RGB image and the five dish totals. Why overhead only: a phone user can take that shot in one second, and the rotating videos need a turntable rig no patient has. Why no depth: most phones don't have a reliable depth sensor, and the first goal was a model any Android phone can run. Depth is the top item under future work.

## The filters, and why each one exists

| Step | Dishes left | Why |
|---|---|---|
| Start | 5,006 | overhead RGB entries in the metadata |
| Drop zero-calorie dishes | 4,766 | A zero-calorie "dish" is almost always a recording error or an empty tray. Training on it teaches the model that some full plates weigh nothing. |
| Keep dishes with an overhead image on disk | 3,260 | Metadata rows without a `rgb.png` can't be used by an image model. This is the biggest cut, and it's a property of the release, not a choice. |
| Drop one implausible label | 3,259 | One dish had nutrition numbers that can't physically be true. One wild target inflates the loss and the error metrics, and the model would be graded against a wrong answer forever. |

Two details in the parser are worth knowing:

- **The metadata CSVs are ragged.** Each line lists dish totals first, followed by a variable number of ingredient fields. The parser takes the first six fields (ID plus five totals) and skips anything that isn't a `dish_` row. Reading it with plain `pd.read_csv` fails on the uneven line lengths.
- **Duplicates are dropped by dish ID, keeping the first.** Otherwise a dish could land in both train and test.

## Why two split sizes appear in this repo

You'll see two sets of numbers:

| Source | Dishes | Train / val / test |
|---|---|---|
| Paper, root README, export notebook for `v4_` models | 3,259 | 2,281 / 488 / 490 |
| `bytebite_v4_walkthrough.md` code, export notebook for `model1_control` | 3,260 | 2,282 / 489 / 489 |

The split code is `int(0.70 * n)` for train, `int(0.15 * n)` for validation, and the rest for test. `int()` truncates, so:

- n = 3,260: train 2,282, val 489, test 489
- n = 3,259: train int(2281.3) = 2,281, val int(488.85) = 488, test 490

Removing a single dish moves one dish out of train, one out of validation, and adds one to test. The implausible-label removal explains the whole difference.

Why it matters: **a model is tied to its split.** The target scaler is fit on the train rows ([06](06-target-scaling.md)), and test numbers are only comparable on the same test dishes. That's why the export notebook keys the expected split sizes by model filename and refuses to export a model against the wrong split. Mixing them up would quietly corrupt every number the phone shows.

## What the data can't tell the model

- **Hidden food.** Rice under a curry, or sauce soaked into bread. An overhead photo can't see it.
- **Density.** A heaped plate of lettuce and a thin layer of butter can look similar in area. Mass is the weakest target for this reason, and depth would help most here.
- **Cafeteria style.** Two cafeterias, one kind of plate, controlled lighting. Home cooking under kitchen lights is a distribution shift the test set doesn't measure. Test MAE is a best case for real-world use, not a promise.

## Check yourself

<details>
<summary>Why is the image-availability filter the biggest cut, and could you avoid it?</summary>

Only about two-thirds of the calorie-labelled metadata rows have a matching overhead RGB image in the release. You can't recover those dishes for an image model. The only way around it would be a different input (such as the side-angle videos), which a phone user can't reproduce.
</details>

<details>
<summary>The test set went from 489 to 490 dishes when one dish was removed. How?</summary>

`int()` truncation. Train and validation are rounded down from percentages of n, and test gets the remainder. Dropping one dish lowers both rounded-down counts by one, so test gains one.
</details>
