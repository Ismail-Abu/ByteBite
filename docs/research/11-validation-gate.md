# 11. The validation gate

## The rule

Before v4 was trained, the decision rule was written down in the notebook:

> v4 replaces v3-FT only if VALIDATION overall MAE improves. Test numbers are reported for the record either way.

Cell 6 of the v4 notebook enforces it. It loads the saved v3-FT model, scores both models on the validation set, and prints a verdict (`GATE: v4 improves validation` or `GATE: v4 does NOT improve validation -> keep v3-FT`). Only then does it compute the test table.

## Why the test set never makes decisions

Every time you look at test results and change something because of them, a little of the test set leaks into your model. Not through the weights, but through you. Do it enough times (try a setting, check test, keep what helped) and you've tuned on the test set by hand. The final number becomes optimistic, because you picked the configuration that happened to do well on those particular 490 dishes.

This is called overfitting to the test set, or the garden of forking paths. It's especially dangerous with a small test set, where the noise between configurations is similar in size to the real differences.

So the roles are strict:

| Split | Used for |
|---|---|
| Train | fitting weights |
| Validation | every decision: early stopping, LR schedule, v3 against v4, which export variant |
| Test | reported once per model, after the decision is made |

## Why write the rule down first

"Pre-registered" means the decision rule is fixed before the result is seen. Without it, it's easy to rationalize after the fact:

- "v4 lost on validation but won on test, so the test set is the real one."
- "It lost overall, but carbs improved, and carbs matter most."

Both might sound reasonable, and both are ways of letting the result pick the rule. Fixing the rule first removes that freedom. The same convention is used again in the export notebook, where the int8 shipping rule is stated in Cell 1 before any accuracy is measured ([15](15-on-device-export.md)).

## Fair comparison details

The gate compares like with like:

- **Same validation dishes.** Guaranteed by the hard-verified split file ([05](05-split-and-leakage.md)).
- **Each model at its own resolution.** v3-FT was trained at 192 px, so it's scored through a 192 px pipeline (`make_eval_ds_192`). Scoring it at 300 px would handicap it and make v4 look better than it is.
- **Real units.** Both are un-standardized with the same train-only scaler before MAE is computed.

## What the gate doesn't do

The gate is one comparison on one seed (42). A single seed can get lucky, so passing the gate makes v4 a candidate, not a claim. Cell 6 says so: "Confirm with Cell V4-4 before claiming it." The claimable number comes from five seeds ([12](12-seeds-and-ensemble.md)).

## Check yourself

<details>
<summary>v4 wins on test but loses on validation. What does the rule say, and why is that the right call?</summary>

Keep v3-FT. The rule was fixed before the results. Switching to test would mean choosing the model by test score, which leaks the test set into the decision and makes the reported number optimistic.
</details>

<details>
<summary>Why score v3-FT at 192 px instead of 300 px in the gate?</summary>

It was trained at 192 px, so that's its correct input. Feeding it 300 px images changes its input distribution and handicaps it, which would make the comparison unfair in v4's favor.
</details>
