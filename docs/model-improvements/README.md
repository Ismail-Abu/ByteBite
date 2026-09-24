# Model Improvement Roadmap

Ideas to push ByteBite's accuracy, robustness, and trustworthiness past the conference
prototype. Ordered roughly by expected payoff per unit effort.

## Priorities

1. **Depth-informed portion size** (`depth-portion-estimation.md`) — Nutrition5k ships
   overhead depth we do not use yet. Depth is the missing signal for mass, and mass drives
   the macro estimates. Highest expected payoff.
2. **Stronger, food-aware augmentation** (`data-augmentation.md`) — cheap, reduces
   overfitting on a ~3,300-dish set.
3. **Architecture experiments** (`architecture-experiments.md`) — vision transformers,
   ensembles, and multi-task heads.
4. **Uncertainty and calibration** (`uncertainty-calibration.md`) — let the app say how sure
   it is, which matters for a health tool.
5. **Multi-view fusion** (`multi-view-fusion.md`) — relax the single-overhead-photo
   constraint for users willing to take two shots.
6. **Evaluation and benchmarking** (`evaluation-and-benchmarking.md`) — the harness that
   keeps all of the above honest.

## Guardrails

- Every change is judged on the **same dish-level 70/15/15 split and seed** used in the
  paper. No leakage across train/val/test.
- Report **per-nutrient MAE with 95% bootstrap CIs**, not a single averaged number.
- Carbohydrate error is the headline metric: it drives insulin dosing and is the clinical
  point of the project.
- Any accuracy gain must survive conversion to the on-device runtime (TFLite / Core ML).
