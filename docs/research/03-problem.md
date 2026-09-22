# 03. The problem

## Who this is for

About 40.1 million people in the United States have diabetes, roughly 12% of the population, and it costs the healthcare system an estimated $412.9 billion a year. Managing blood glucose depends heavily on knowing what you ate, and above all how many carbohydrates.

## Why carbohydrates matter most

Of the five targets, carbohydrates have the most direct effect on the patient:

- Carbs are the main driver of the rise in blood glucose after a meal (postprandial glycemic response).
- People on insulin often dose by carb count, using an insulin-to-carb ratio. A carb error becomes a dosing error.
- Calories and mass matter for weight management over weeks. Carbs matter for the next two hours.

That ranking shows up in decisions all over this repo:

- **Model choice.** EfficientNetV2B0 would have been a 3x smaller file on the phone. It was rejected because its carb MAE is 7.42 g against 4.12 g for EfficientNetB3 ([15](15-on-device-export.md)).
- **Export gate.** int8 quantization must keep every nutrient within 2%, and carbs within an absolute 0.10 g on top of that ([15](15-on-device-export.md)).
- **Paper framing.** The results section singles out carbohydrate and protein, where the gains were largest.

When a decision trades accuracy for something else (size, speed, simplicity), carbs are the bar it has to clear.

## Why a photo

Existing tools fall into two groups, according to the paper:

1. **Manual logging.** You search a food database, pick an entry, and guess a portion. It's accurate if you're diligent, but it's tedious, and people stop doing it. A tracker nobody uses tracks nothing.
2. **User-friendly but costly.** Paid apps and services.

ByteBite's bet is that one overhead photo is the least effort that still carries enough information. The model reads portion size and food type from the image and returns all five numbers at once. The target platform is Android, about 73% of the global mobile market, so the tool reaches the most people without new hardware.

## Why five outputs from one network

The model predicts calories, mass, fat, carbs and protein together from one shared backbone, instead of training five models. Two reasons:

- **The targets are related.** Calories are roughly 4 kcal per gram of carb or protein plus 9 per gram of fat. Mass bounds everything. Features that help one target (how big is this portion, is that sauce oily) help the others. A shared network learns them once.
- **One pass on the phone.** Five backbones would be five times the file size and five times the latency.

The cost is that the five losses compete for the same weights. That's one reason the targets are standardized first ([06](06-target-scaling.md)): otherwise calories, measured in hundreds, would drown out fat, measured in single grams.

## What "good" means here

The metric is **mean absolute error (MAE)** per nutrient, in real units: "on average the carb estimate is off by 4.12 g". MAE was chosen over RMSE for reporting because it reads directly as a typical error. RMSE squares errors first, so a few badly wrong dishes dominate it. Both are computed in the notebooks.

## Check yourself

<details>
<summary>If you had to cut one target to save compute, which would you keep last, and why?</summary>

Carbohydrates. They drive short-term glucose response and insulin dosing, so a carb error has the most direct clinical cost of the five.
</details>

<details>
<summary>Why not train five small single-target models?</summary>

The targets share structure (portion size, food type, fat content), so a shared backbone learns those features once. On the phone, five models would multiply file size and latency.
</details>
