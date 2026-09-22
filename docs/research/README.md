# ByteBite research, explained

These notes break ByteBite into its parts and explain why each decision was made, not just what was done. Read them in order the first time. After that, each one stands on its own.

Every number comes from a file in this repo:

- the root `README.md`
- `paper/ByteBite_Paper.pdf`
- `notebooks/bytebite_v4_walkthrough.md`
- `notebooks/bytebite_android_export.ipynb`
- `android/README_MODEL.md` and the Kotlin sources

Where those files disagree, or don't say, the note points that out instead of guessing.

## Contents

| # | Note | The question it answers |
|---|---|---|
| 01 | [Research journey](01-research-journey.md) | What did we try, in what order, and what survived? |
| 02 | [Final pipeline](02-final-pipeline.md) | What happens between taking a photo and seeing "carbs 42 g"? |
| 03 | [The problem](03-problem.md) | Why this project, and why carbohydrates are the target that matters most |
| 04 | [The dataset](04-dataset.md) | How 5,006 dishes became 3,259, and why each cut was made |
| 05 | [The split and the leakage bug](05-split-and-leakage.md) | How a shuffled pipeline leaked test dishes into training, and how the fix is enforced |
| 06 | [Target scaling](06-target-scaling.md) | Why the model predicts z-scores instead of grams |
| 07 | [Model choice](07-model-choice.md) | Why a pretrained EfficientNetB3 beats a network trained from scratch here |
| 08 | [Loss and training loop](08-loss-and-training.md) | Why Huber loss, Adam, early stopping and LR decay |
| 09 | [Fine-tuning](09-fine-tuning.md) | Which layers train, why BatchNorm stays frozen, why the learning rate is so small |
| 10 | [Resolution and augmentation](10-resolution-and-augmentation.md) | Why 300 px and random rotations bought most of the v4 gain |
| 11 | [The validation gate](11-validation-gate.md) | Why the test set never makes a decision |
| 12 | [Seeds and the ensemble](12-seeds-and-ensemble.md) | Why one training run can lie, and why averaging five models helps |
| 13 | [Negative result: text heads](13-negative-result-text-heads.md) | An idea that looked like a win and wasn't |
| 14 | [Reading the results](14-reading-the-results.md) | How to read the tables, the CIs, and where the numbers disagree |
| 15 | [On-device export](15-on-device-export.md) | How a 44 MB Keras model became a 22 MB file on a phone |
| 16 | [Port pitfalls and next steps](16-port-pitfalls-and-next-steps.md) | The three silent bugs in the port, privacy, what's next, glossary |

## How to use these

Start with the two flowcharts (01 and 02). They're the map, and every later note zooms into one box.

Each note ends with a few self-check questions, with the answers folded underneath. If you can answer them without opening the fold, you understand that part well enough to defend it to a reviewer.
