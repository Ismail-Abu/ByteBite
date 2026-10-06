# photocheck fixtures

A small set of 27 images used only as **software-behaviour fixtures** for the
food-acceptance (food/non-food) evaluation harness:

- `food_00.jpg` … `food_11.jpg` — 12 images labelled **food**.
- `nonfood_00.jpg` … `nonfood_13.jpg` — 14 images labelled **non-food**.
- `nonfood_screen.jpg` — a food photo shown **on a screen** (a non-food /
  unsupported-scene case: the gate should not treat a photo-of-a-photo as a real
  meal).

## What this set is and is not

- It exercises the code paths of a food-gate boundary (accepted / non-food /
  poor-quality / unsupported / uncertain / unavailable). It is **not** evidence
  that any detector works: 27 images cannot establish non-food acceptance rate,
  legitimate-meal rejection rate, coverage, or accuracy. A real evaluation needs
  a held-out dataset with reported sample counts and confidence intervals.
- These are **held-out** fixtures only. Do not tune thresholds or train against
  them; threshold selection and training must use separate data so this set
  stays an honest check.
- No model/classifier is integrated yet, so these are not currently consumed by
  a passing test; they are staged for when the food-gate service is implemented.

## Provenance and licensing — ACTION REQUIRED

The original **source, author, and license of each image are not yet recorded**
and must be confirmed before these files are relied upon or published. Until a
verified source/license is documented here per image, treat them as
unverified QA placeholders:

- If any image is not clearly licensed for this use, replace it with an image
  that is (e.g. self-captured QA photos or an explicitly-licensed dataset) and
  record the source/author/license/label here.
- Do not assume these are safe to redistribute.

This note exists so the gap is explicit rather than hidden.
