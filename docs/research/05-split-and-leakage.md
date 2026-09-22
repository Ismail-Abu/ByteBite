# 05. The split and the leakage bug

## Why split at the dish level

The model is graded on dishes it has never seen. If any dish, or a copy of it, appears in both training and test, the score partly measures memory. Nutrition5k has one overhead image per dish, so splitting by dish ID is the same as splitting by image, and each dish goes into exactly one of train, validation or test.

- **Train (70%)** is what the weights learn from.
- **Validation (15%)** is what every decision is made on: early stopping, the learning-rate schedule, whether v4 replaces v3 ([11](11-validation-gate.md)).
- **Test (15%)** is read once per model, for the record, and never used to choose anything.

## The bug

The first pipeline built the split roughly like this. The original code isn't in this repo, so this is a reconstruction of the pattern the walkthrough describes ("take/skip after reshuffle"):

```python
ds = tf.data.Dataset.from_tensor_slices((paths, labels))
ds = ds.shuffle(n, reshuffle_each_iteration=True)   # new order every epoch
train = ds.take(n_train)
val   = ds.skip(n_train).take(n_val)
test  = ds.skip(n_train + n_val)
```

It looks right: shuffle once, cut into three pieces. But `reshuffle_each_iteration=True` (the default) means the shuffle runs again every time the dataset is iterated, which is every epoch. `take` and `skip` then cut a new random order each time. So:

- Epoch 1 trains on one random 70% of dishes.
- Epoch 2 trains on a different random 70%.
- After a handful of epochs, the model has trained on nearly every dish, including the ones labelled "test".
- The "test" pieces don't even agree with each other across epochs, or between training and evaluation.

The result is test numbers that look better than the model really is, and a model that's quietly worse than reported. It's one of the most common bugs in `tf.data` code because nothing errors out. The only symptom is scores that are too good.

## The fix

The v1 pipeline moved all split logic out of `tf.data` and made it plain, deterministic Python:

1. **Count before shuffling.** `image_count = len(final_df)` is computed from the cleaned table before anything is shuffled. That line carries the comment `v1 bug fix preserved`.
2. **One seeded permutation.** `np.random.default_rng(42).permutation(n)` is sliced once into train, val and test indices. The indices never change during training.
3. **Shuffle only inside train.** The training pipeline still shuffles every epoch, but only among the train indices. Shuffling training order is good practice. Shuffling across the split boundary is the bug.
4. **Evaluation pipelines don't shuffle at all**, so predictions line up row by row with the label arrays.

## Why the split is hard-verified

Fixing the code once isn't enough. Across months of experiments, v1, v3 and v4 were trained in different sessions, on different machines (local and RunPod), sometimes with the dataset re-downloaded. Any of those can silently change the split:

- a different file listing order from the filesystem
- one more or one fewer image on disk
- a different NumPy version

So the first run writes the split to `outputs/data_split_seed42.csv` (dish ID plus train/val/test). Every later run rebuilds the split and compares it row by row against that file. If a single dish moved, `verify_split` raises and names the first row that differs. A size assert backs it up.

This is what makes the numbers comparable across versions. "v4 beats v3-FT" only means something if both were scored on the same 489 test dishes. The walkthrough states it directly: train, validation and test can never overlap and never drift between experiments.

## The general lesson

When a result looks too good, check the split before celebrating. The pattern that prevents this class of bug:

- make the split a file, not a side effect of a shuffle
- assert against it everywhere
- keep the test set out of every decision

## Check yourself

<details>
<summary>Shuffling training data every epoch is good practice. Why was this shuffle a bug?</summary>

It ran before the split instead of inside it. Shuffling inside the train set only changes the order the model sees its own training dishes. Shuffling the whole dataset and then cutting it changes which dishes are in each set, so test dishes leak into training.
</details>

<details>
<summary>What would you expect to see in the metrics if the bug came back?</summary>

Test and validation errors that fall suspiciously close to training error, and results that don't reproduce when the model is scored on a truly held-out file. Nothing crashes, which is why the split file and the assert exist.
</details>
