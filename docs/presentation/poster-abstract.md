# Poster Abstract

**ByteBite: A Deep Learning-Based Tool for Real-Time Estimation of Food Nutritional
Components to Support Diabetes Self-Management**

Saim Sultan, Ismail Abu-Shanab, Husam Ghazaleh, Ph.D.
Department of Mathematical and Computational Sciences, Benedictine University

## Abstract

ByteBite estimates a meal's nutritional content — calories, carbohydrates, protein, fat,
and total mass — from a single overhead smartphone photograph, and is intended for use as
a smartphone application requiring no manual entry. Three CNN (Convolutional Neural
Network) architectures were developed and trained on Nutrition5k, a Google dataset of
overhead dish images with ingredient-level measured labels. The models were benchmarked
against each other and against SnapNutrition, an existing public baseline, on the same
dish-level split and seed. The best-performing model (a fine-tuned EfficientNetB3)
achieved the lowest mean absolute error on every target. SHAP attribution highlights which
image regions drive each prediction, confirming the model concentrates on food regions
rather than background. A prototype Android interface targets offline, on-device inference.

## Problem statement

Around 40.1 million people in the United States have diabetes, roughly 12% of the
population, at an estimated economic cost of $412.9 billion in 2022. Carbohydrate and
caloric awareness is central to blood-glucose management and insulin dosing. Existing
tracking applications are either burdensome (manual logging of every item) or costly. That
burden causes abandonment. ByteBite is intended to be free and to target zero-effort
tracking: one photograph, five nutritional estimates, no typing.

## Conclusion

On Nutrition5k dishes, ByteBite estimates five nutritional targets from a single overhead
photograph with no manual entry, with the largest accuracy gains over the baseline for
carbohydrate and protein. Next steps: portion-size estimation from the dataset's unused
overhead depth images, and TensorFlow Lite / Core ML deployment for offline on-device
inference.
