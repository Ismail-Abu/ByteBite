# 13. Negative result: text heads

## The idea

The core model sees an image and predicts five numbers. The idea behind text-alignment heads was to give the model a second, related job during training: extra outputs trained to line up its image features with text embeddings of each dish's description. The experiments used MiniLM and Qwen2-VL for the text side.

The reasoning was sound:

- Nutrition5k lists the ingredients of every dish. Text like "white rice, grilled chicken breast, olive oil" contains exactly the information that separates a low-fat dish from a high-fat one that looks similar.
- An auxiliary task can shape the shared features. If the backbone has to produce features that line up with ingredient text, those features might carry more nutrition-relevant information, and the nutrition head could use it.
- At inference the text heads can be dropped. The phone still needs only a photo.

This is sometimes called multi-task learning, or auxiliary supervision. It works in some settings.

## What happened

It was tested across 5 seeds in two regimes, with the backbone frozen and with it fine-tuned. The result in both was no significant effect on nutrition MAE. The text heads were removed entirely for v4, along with the cooking-classification heads. The app's cooking-cue and hidden-sugar features, which relied on those heads, now appear only in sample-data mode.

## Why it briefly looked like a win

On a single seed, the text-head model scored better than the model without them. As covered in [12](12-seeds-and-ensemble.md), one seed's result carries noise about the size of the run-to-run spread. A single comparison happened to fall in the text heads' favor. With five seeds per arm, the difference disappeared into the noise.

This is the most useful lesson in the project: **a result that hasn't been replicated across seeds is a hypothesis, not a finding.** It's why the v4 notebook reports the 5-seed mean as the claimable number, and why passing the single-seed gate only makes v4 a candidate ([11](11-validation-gate.md)).

## Why it may not have helped

These are plausible explanations, not measured ones:

- **The information was already there.** A fine-tuned backbone may already encode what the ingredient text says, so aligning with text adds nothing new.
- **Text describes what, not how much.** "Rice and chicken" is the same text for a small plate and a large one. Portion size is the main source of calorie and mass error, and ingredient text doesn't carry it.
- **Competing objectives.** An auxiliary loss pulls on the same weights as the nutrition loss. If the tasks aren't well aligned, the pulls partly cancel.

## Why keep a negative result

It would be easy to delete the experiment and never mention it. Keeping it is better science and better engineering:

- **It saves the next person the time.** Anyone who has the same idea can see it was tested, how, and what happened.
- **It shows the claims are honest.** A repo that reports only wins invites the question of how many losses were hidden. Reporting a clean negative, tested properly across seeds and regimes, makes the positive results more believable.
- **It shaped the final system.** Dropping the heads simplified the v4 model to one image in and five numbers out, which made the phone export straightforward. It also forced an honest UI: the app doesn't show per-ingredient or cooking-cue claims the model can't back up ([16](16-port-pitfalls-and-next-steps.md)).

## Check yourself

<details>
<summary>What would you need to see before believing the text heads help?</summary>

A consistent improvement across several seeds (at least five per arm), larger than the seed-to-seed spread, on validation. The test set would be read only afterwards to confirm.
</details>

<details>
<summary>Why wouldn't ingredient text fix the biggest error source?</summary>

The biggest error is portion size (mass, and so calories), and ingredient text says what's on the plate, not how much. Two plates with the same ingredients and very different sizes get identical text.
</details>
