# 12. Seeds and the ensemble

## Why one training run can lie

Train the same model on the same data twice with different random seeds, and you get different results. The seed controls:

- the random initialization of the new head layers
- the order training dishes are shuffled in each epoch
- which augmentation (flip or rotation) each image gets
- which units dropout zeroes

Those small differences compound, and the final test MAE moves. For v4, the five seeds landed at 18.14 +/- 0.21 overall. That spread is small, but not zero. If you compare two ideas that each differ by less than a couple of those spreads, on one seed each, you're partly comparing luck.

That's exactly what happened with the text-alignment heads. On one seed they looked like an improvement. Across five seeds the effect vanished ([13](13-negative-result-text-heads.md)). That experience is why the walkthrough says the claimable number is the mean over 5 seeds.

## The five-seed procedure

Cell 7 of the v4 notebook trains seeds 42 through 46, one at a time:

1. Build a fresh model with that seed's initialization.
2. Train it with that seed's shuffle order and augmentation.
3. Predict on the identical 489 test dishes.
4. Save the model to disk immediately, free GPU memory, and move to the next seed.

Seed 42 reuses the model from the gate. Any seed already on disk prints "reused", so a crash costs one seed, not five.

It reports:

- each seed's test MAE, overall and per nutrient
- mean +/- standard deviation (18.14 +/- 0.21)
- the ensemble

Later, Cell 8 turns the five per-seed values into t-based 95% confidence intervals per nutrient. With five runs the t critical value is 2.776 (4 degrees of freedom), which gives wider intervals than the familiar 1.96 because five samples are few.

## The ensemble: 18.14 becomes 17.33

The ensemble averages the five models' predictions for each dish:

```python
ens_pred = np.mean(test_preds_v4, axis=0)
```

It scores 17.33, better than every individual model (about 18.1 each).

**Why averaging helps.** Each model's error on a dish has two parts:

- **Shared error.** All five get it wrong the same way, for example because the rice is hidden under the curry. Averaging can't fix this.
- **Individual error.** Each seed's particular quirks: a slightly different feature it latched onto, or a different guess on an ambiguous plate. These point in different directions for different seeds.

Averaging keeps the shared part and shrinks the individual part, because errors in different directions partly cancel. If the individual errors were fully independent, averaging five would cut their size by a factor of about the square root of 5, roughly 2.2. They aren't fully independent (same data, same architecture), so the real gain is smaller: 18.14 to 17.33, about 4.5%.

**Why the gain is modest.** The five models share almost everything, so their errors are highly correlated. Ensembles of different architectures, or models trained on different inputs such as RGB and depth, usually disagree more and gain more.

## The phone ships one model, not five

The app runs a single v4 model (seed 42), not the ensemble. Five EfficientNetB3s would be five times the file size (about 110 MB at float16) and five times the latency, for about 0.8 MAE. On a phone that's a poor trade. The ensemble number shows what's possible. The single-model number (about 18.1) is what the phone delivers.

## Check yourself

<details>
<summary>Why can't an ensemble fix the error on a dish where the rice is hidden under curry?</summary>

All five models see the same photo, and none of them can see the rice. They all err in the same direction, and averaging identical errors leaves them unchanged. Only new information, such as depth, can fix shared errors.
</details>

<details>
<summary>Two ideas differ by 0.15 MAE on one seed each. The seed spread is 0.21. What can you conclude?</summary>

Nothing yet. The difference is smaller than the run-to-run noise. You'd need several seeds for each idea, and the difference would have to hold up against their combined spread, before calling either one better.
</details>
