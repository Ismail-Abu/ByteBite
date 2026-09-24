# Q&A Notes from the Poster Session

Questions attendees asked, and the answers we gave (or wish we had).

### "How is this different from MyFitnessPal or a barcode scanner?"
No manual entry and no database lookup. ByteBite regresses nutrition directly from pixels,
and it estimates **mass**, which portion-size apps require the user to input by hand.

### "What happens with mixed or occluded dishes?"
The model was trained on Nutrition5k cafeteria dishes, many of which are mixed plates. It
degrades gracefully but has not been validated on heavily stacked or occluded food. This is
an open limitation.

### "Why overhead photos specifically?"
Nutrition5k's high-accuracy labels are paired with overhead RGB and depth captures. The
fixed camera angle reduces perspective variance, which helps mass estimation. A future
version should relax this with multi-view or augmentation.

### "How do you know it isn't cheating on the tray or background?"
SHAP attribution. Most attribution mass lands inside the plate region, not on the tray,
utensils, or table. We showed the heatmaps on the poster.

### "Is the data private?"
Inference runs fully on-device. No photo leaves the phone, which matters for health data.

### "What's the accuracy?"
Mean absolute error per nutrient with 95% bootstrap confidence intervals. EfficientNetB3
had the lowest MAE across all five targets, beating the SnapNutrition baseline most clearly
on carbohydrate and protein.

### Questions we could not fully answer (follow-ups)
- Calibration: how confident is any single prediction? (See uncertainty roadmap.)
- Generalization to home-cooked, non-cafeteria meals.
- Real-world latency on a mid-range phone, not just GPU.
