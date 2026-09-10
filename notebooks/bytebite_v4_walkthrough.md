# ByteBite v4 - Full Code 

This is the exact notebook that produced the v4 results (5-seed test overall MAE 18.14 +/- 0.21, ensemble 17.33). Code blocks below are copied verbatim from the notebook; the text before each block explains what it does and why it is set up that way.

What changed vs the previous fine-tuned model (v3-FT, 21.59):

1. Input resolution 192x192 -> 300x300 (EfficientNetB3's native size). Portion cues are fine-grained, so resolution matters a lot here.
2. Data augmentation added: random horizontal/vertical flips and random 90-degree rotations. Overhead food photos have no natural orientation, so every rotation/flip is a valid extra training view - free data.
3. More of the backbone unfrozen: blocks 4-7 + the top conv now train (v3-FT only opened block 6+). Learning rate lowered to 3e-5 to compensate - the more pretrained layers you let move, the gentler the updates must be or the ImageNet features get destroyed.
4. Text-alignment heads removed entirely (tested across 5 seeds in both the frozen and fine-tuned regimes: no significant effect).

Training is fully sequential: one model at a time, and inside the multi-seed cell one seed at a time, each saved to disk as it finishes so a crash never loses completed work.

## Cell 1 - Config and reproducibility

Seeds everything (Python, NumPy, TF) with 42, finds the dataset whether the notebook runs locally or on RunPod, and defines every hyperparameter in one place. `set_memory_growth` makes TF request GPU memory as needed instead of grabbing it all up front - avoids out-of-memory crashes in long multi-model sessions. The v1 and v3-FT numbers are kept here as fixed reference points.

```python
# =============================================================================
# CELL 1: IMPORTS, CONFIG, REPRODUCIBILITY  (ByteBite v4 - fine-tuning line)
# =============================================================================
%matplotlib inline

import os
os.environ.setdefault("TF_CPP_MIN_LOG_LEVEL", "2")

import sys
import time
import random
import warnings
from pathlib import Path

import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
from IPython.display import display

import tensorflow as tf
from tensorflow import keras
from tensorflow.keras import layers

warnings.filterwarnings("ignore")

# --------------------------- Reproducibility --------------------------------
SEED = 42
os.environ["PYTHONHASHSEED"] = str(SEED)
random.seed(SEED)
np.random.seed(SEED)
tf.random.set_seed(SEED)
keras.utils.set_random_seed(SEED)

# ------------------------------- Paths --------------------------------------
_BASE_CANDIDATES = [Path(os.environ.get("NUTRITION5K_DIR", "nutrition5k_dataset")),
                    Path("/workspace/nutrition5k_dataset"),
                    Path("nutrition5k_dataset")]
BASE_DIR     = next((p for p in _BASE_CANDIDATES if p.exists()), _BASE_CANDIDATES[0])
METADATA_DIR = BASE_DIR / "metadata"
IMAGERY_DIR  = BASE_DIR / "imagery" / "realsense_overhead"
CAFE1_CSV    = METADATA_DIR / "dish_metadata_cafe1.csv"
CAFE2_CSV    = METADATA_DIR / "dish_metadata_cafe2.csv"

OUTPUT_DIR   = Path("outputs"); OUTPUT_DIR.mkdir(exist_ok=True)
SPLIT_CSV    = OUTPUT_DIR / "data_split_seed42.csv"
P_BASE_FT    = OUTPUT_DIR / "finetune_baseline.keras"   # v3-FT reference (for the val gate)
P_V4_MODEL   = OUTPUT_DIR / "v4_model_seed42.keras"

# --------------------------- v4 experiment config ----------------------------
IMG_V4      = (300, 300)                 # EfficientNetB3 native resolution
BATCH_V4    = 16
LR_V4       = 3e-5
EPOCHS_V4   = 60
PATIENCE_V4 = 6
UNFREEZE_V4 = ("block4", "block5", "block6", "block7", "top")

NUTRIENTS   = ["calories", "mass", "fat", "carb", "protein"]
TARGET_COLS = ["total_calories", "total_mass", "total_fat", "total_carb", "total_protein"]
BASELINE_MAE = {"calories": 79.88, "mass": 47.46, "fat": 5.92,
                "carb": 6.88, "protein": 7.70}   # v1 reference
BASELINE_OVERALL = float(np.mean(list(BASELINE_MAE.values())))
V3FT_5SEED_OVERALL = 21.59                        # v3 fine-tuned, 5-seed mean

print(f"Python {sys.version.split()[0]} | TF {tf.__version__} | Keras {keras.__version__}")
print(f"GPUs visible to TF: {len(tf.config.list_physical_devices('GPU'))}"
      + ("" if tf.config.list_physical_devices('GPU') else "  <-- WARNING: v4 needs the GPU"))
_missing = [p for p in (CAFE1_CSV, CAFE2_CSV, IMAGERY_DIR) if not p.exists()]
if _missing:
    raise FileNotFoundError(f"Missing dataset paths: {_missing}")
print(f"Dataset: {BASE_DIR}")
print(f"References: v1 overall {BASELINE_OVERALL:.2f} | v3-FT 5-seed overall {V3FT_5SEED_OVERALL:.2f}")
print("Cell 1 complete.")
```

## Cell 2 - Data loading and the fixed split

Parses the two cafeteria metadata CSVs (variable-length lines - dish totals are the first six fields), keeps dishes that have an overhead RGB image and calories > 0, and splits 70/15/15. The critical part: the split is a single seeded permutation, written to `data_split_seed42.csv` on first run and HARD-VERIFIED against that file on every later run - the assert fails if even one dish moved. Train/val/test can never overlap and never drift between experiments, which is what makes v1/v3/v4 numbers comparable. (This is also exactly the property the old take/skip-after-reshuffle split was missing.)

```python
# =============================================================================
# CELL 2: REBUILD final_df (EXACT v1 PIPELINE) + SPLIT + HARD VERIFICATION
# =============================================================================
META_COLS = ["dish_id", "total_calories", "total_mass",
             "total_fat", "total_carb", "total_protein"]

def load_dish_metadata(csv_path):
    """v1 parser: ragged Nutrition5k CSV -> clean 6-column DataFrame."""
    rows, skipped = [], 0
    with open(csv_path, "r", encoding="utf-8", errors="replace") as f:
        for line in f:
            parts = line.strip().split(",")
            if len(parts) < 6 or not parts[0].startswith("dish_"):
                skipped += 1
                continue
            try:
                rows.append([parts[0]] + [float(v) for v in parts[1:6]])
            except ValueError:
                skipped += 1
    df = pd.DataFrame(rows, columns=META_COLS)
    print(f"{os.path.basename(csv_path)}: parsed {len(df)} dishes ({skipped} skipped lines)")
    return df

meta = pd.concat([load_dish_metadata(CAFE1_CSV), load_dish_metadata(CAFE2_CSV)],
                 ignore_index=True)
meta = meta.drop_duplicates(subset="dish_id", keep="first").reset_index(drop=True)
meta = meta[meta["total_calories"] > 0].reset_index(drop=True)

available_ids = set()
for entry in os.scandir(IMAGERY_DIR):
    if entry.is_dir() and os.path.isfile(os.path.join(entry.path, "rgb.png")):
        available_ids.add(entry.name)

meta_indexed = meta.set_index("dish_id")
matched_ids  = sorted(set(meta_indexed.index) & available_ids)
final_df = meta_indexed.loc[matched_ids].reset_index()
final_df["image_path"] = final_df["dish_id"].map(lambda d: str(IMAGERY_DIR / d / "rgb.png"))

image_count = len(final_df)     # BEFORE any shuffle (v1 bug fix preserved)
print(f"final_df rebuilt: {image_count} dishes (v1 had 3,260)")

# ---- 70/15/15 split, seed 42 (identical procedure to v1) --------------------
rng  = np.random.default_rng(SEED)
perm = rng.permutation(image_count)
n_train, n_val = int(0.70 * image_count), int(0.15 * image_count)
train_idx = perm[:n_train]
val_idx   = perm[n_train:n_train + n_val]
test_idx  = perm[n_train + n_val:]
split_arr = np.array(["train"] * image_count, dtype=object)
split_arr[val_idx], split_arr[test_idx] = "val", "test"
our_split = pd.DataFrame({"dish_id": final_df["dish_id"], "split": split_arr})
print(f"Split -> train {len(train_idx)} / val {len(val_idx)} / test {len(test_idx)}")

def verify_split(our_df, ref_csv_path):
    """HARD verification against the v1 split file. Raises on any mismatch."""
    ref = pd.read_csv(ref_csv_path)
    if len(ref) != len(our_df):
        raise ValueError(f"SPLIT MISMATCH: rebuilt {len(our_df)} rows vs saved "
                         f"{len(ref)} rows. Dataset on disk changed since v1.")
    ids_ok   = ref["dish_id"].tolist() == our_df["dish_id"].tolist()
    split_ok = ref["split"].tolist()   == our_df["split"].tolist()
    if not (ids_ok and split_ok):
        bad = next(i for i in range(len(ref))
                   if ref["dish_id"].iloc[i] != our_df["dish_id"].iloc[i]
                   or ref["split"].iloc[i] != our_df["split"].iloc[i])
        raise ValueError(
            f"SPLIT MISMATCH at row {bad}: "
            f"saved=({ref['dish_id'].iloc[bad]},{ref['split'].iloc[bad]}) "
            f"rebuilt=({our_df['dish_id'].iloc[bad]},{our_df['split'].iloc[bad]})")
    return True

if SPLIT_CSV.exists():
    verify_split(our_split, SPLIT_CSV)
    print("[OK] Split HARD-VERIFIED against outputs/data_split_seed42.csv "
          "- identical to v1.")
else:
    our_split.to_csv(SPLIT_CSV, index=False)
    print("WARNING: no saved v1 split found; wrote a fresh one. Results are only "
          "comparable to v1 if the dataset on disk is unchanged.")

assert (len(train_idx), len(val_idx), len(test_idx)) == (2282, 489, 489), \
    f"Unexpected split sizes {len(train_idx)}/{len(val_idx)}/{len(test_idx)} - expected 2282/489/489"

y_all_raw   = final_df[TARGET_COLS].to_numpy(dtype=np.float32)
y_train_raw = y_all_raw[train_idx]
y_val_raw   = y_all_raw[val_idx]
y_test_raw  = y_all_raw[test_idx]
print("Cell 2 complete.")```

## Cell 3 - Target scaling

The five targets live on very different scales (calories in the hundreds, fat in tens of grams). Each is z-scored so no single target dominates the loss. The mean/std are computed on the TRAINING set only - using val/test statistics would leak information. All reported numbers are converted back to real units first (the round-trip assert proves inverse(transform(y)) == y).

```python
# =============================================================================
# CELL 3: TARGET SCALER (TRAIN-ONLY STATS)
# =============================================================================
class TargetScaler:
    """Z-score using TRAIN-ONLY statistics; inverse before any reported number."""
    def fit(self, y):
        self.mu = y.mean(axis=0)
        sd = y.std(axis=0)
        self.sd = np.where(sd == 0, 1.0, sd)
        return self
    def transform(self, y):
        return (y - self.mu) / self.sd
    def inverse(self, z):
        return z * self.sd + self.mu

scaler = TargetScaler().fit(y_train_raw)          # TRAIN ONLY - no leakage
z_all  = scaler.transform(y_all_raw).astype(np.float32)
rt = scaler.inverse(scaler.transform(y_val_raw))
assert np.allclose(rt, y_val_raw, atol=1e-3), "z-score inverse round-trip failed"
print(f"Target scaler fit on train only. mu={np.round(scaler.mu, 1)} sd={np.round(scaler.sd, 1)}")
print("Cell 3 complete.")
```

## Cell 4 - Input pipelines and augmentation

tf.data pipelines that read PNGs from disk, resize to 300x300, and batch. The TRAINING pipeline additionally applies random left-right flip, up-down flip, and a random 0/90/180/270-degree rotation on every image, every epoch - so the model effectively never sees the exact same picture twice. Evaluation pipelines apply NO augmentation and keep dish order fixed so predictions line up with the label arrays. Note the shuffle happens INSIDE the fixed training subset - shuffling training order is good practice; the bug to avoid is shuffling across the split boundary. `make_eval_ds_192` exists only to evaluate the older 192px v3-FT model for the comparison gate.

```python
# =============================================================================
# CELL 4 (V4-1): DATA PIPELINES + AUGMENTATION (300px, AUGMENTED, NUTRITION-ONLY)
# =============================================================================
# Text heads are permanently dropped. This is the fine-tuning line only.

# (config constants defined in Cell 1)

all_paths_v4 = final_df["image_path"].tolist()

def decode_img_v4(path):
    img = tf.io.read_file(path)
    img = tf.io.decode_png(img, channels=3)
    img = tf.image.resize(img, IMG_V4)
    return tf.cast(img, tf.float32)      # EfficientNet normalizes internally

def _augment_v4(img):
    img = tf.image.random_flip_left_right(img)
    img = tf.image.random_flip_up_down(img)
    k = tf.random.uniform([], 0, 4, dtype=tf.int32)
    return tf.image.rot90(img, k)        # overhead images are rotation-invariant

def make_train_ds_v4(idx, run_seed):
    paths = [all_paths_v4[i] for i in idx]
    ds = tf.data.Dataset.from_tensor_slices((paths, z_all[idx]))
    ds = ds.shuffle(len(paths), seed=run_seed, reshuffle_each_iteration=True)
    ds = ds.map(lambda p, y: (_augment_v4(decode_img_v4(p)), y),
                num_parallel_calls=tf.data.AUTOTUNE)
    return ds.batch(BATCH_V4).prefetch(tf.data.AUTOTUNE)

def make_eval_ds_v4(idx):
    paths = [all_paths_v4[i] for i in idx]
    ds = tf.data.Dataset.from_tensor_slices((paths, z_all[idx]))
    ds = ds.map(lambda p, y: (decode_img_v4(p), y),
                num_parallel_calls=tf.data.AUTOTUNE)
    return ds.batch(BATCH_V4).prefetch(tf.data.AUTOTUNE)

def make_eval_ds_192(idx):
    """192px pipeline for evaluating the saved v3 fine-tuned baseline."""
    paths = [all_paths_v4[i] for i in idx]
    ds = tf.data.Dataset.from_tensor_slices(paths)
    ds = ds.map(lambda p: tf.cast(tf.image.resize(
        tf.io.decode_png(tf.io.read_file(p), channels=3), (192, 192)), tf.float32),
        num_parallel_calls=tf.data.AUTOTUNE)
    return ds.batch(32).prefetch(tf.data.AUTOTUNE)

def _pred_nut(pred):
    if isinstance(pred, dict):
        return np.asarray(pred["nutrition"])
    if isinstance(pred, (list, tuple)):
        return np.asarray(pred[0])
    return np.asarray(pred)

n_gpu = len(tf.config.list_physical_devices("GPU"))
print(f"GPUs visible: {n_gpu}" + ("" if n_gpu else "  <-- WARNING: this needs the GPU"))
print(f"v4 config: {IMG_V4[0]}px | batch {BATCH_V4} | lr {LR_V4} | "
      f"unfreeze from {UNFREEZE_V4[0]} | flips + 90-degree rotations")
print("Cell V4-1 complete.")

```

## Cell 5 - Model architecture and single-seed training

Architecture: EfficientNetB3 pretrained on ImageNet, then GlobalAveragePooling -> Dense(256, relu) -> Dropout(0.3) -> Dense(5) predicting all five targets at once (one shared network, multitask by output width - same head stack as v1).

Which parts train: blocks 4-7 and the top conv of the backbone are unfrozen; blocks 1-3 stay frozen (low-level edge/texture filters transfer fine from ImageNet). Two BatchNorm details: every BatchNormalization layer is kept non-trainable, and the backbone is called with `training=False`, which keeps BN using its ImageNet running statistics. With small batches (8), letting BN re-estimate statistics destabilizes training - freezing them is the standard fine-tuning recipe. Gradients still flow into the unfrozen conv weights.

Training style: Huber loss (like MSE near zero, like MAE for outliers - robust to the extreme dishes) on the z-scored targets, Adam at 3e-5, up to 60 epochs. EarlyStopping watches validation loss with patience 6 and restores the best-epoch weights, so the final model is the checkpoint that generalized best, not the last epoch. ReduceLROnPlateau halves the learning rate when validation stalls for 3 epochs.

```python
# =============================================================================
# CELL 5 (V4-2): BUILD + TRAIN v4 (SEED 42)
# =============================================================================
def build_v4(run_seed):
    keras.backend.clear_session()
    keras.utils.set_random_seed(run_seed)
    backbone = keras.applications.EfficientNetB3(include_top=False,
                                                 weights="imagenet",
                                                 input_shape=IMG_V4 + (3,))
    backbone.trainable = True
    n_unf = 0
    for lyr in backbone.layers:
        unf = lyr.name.startswith(UNFREEZE_V4)
        if isinstance(lyr, layers.BatchNormalization):
            unf = False
        lyr.trainable = unf
        n_unf += int(unf)
    inp = keras.Input(shape=IMG_V4 + (3,), name="image")
    h = backbone(inp, training=False)
    h = layers.GlobalAveragePooling2D(name="gap")(h)
    h = layers.Dense(256, activation="relu", name="shared_dense")(h)
    h = layers.Dropout(0.3, name="dropout")(h)
    out = layers.Dense(5, name="nutrition")(h)
    m = keras.Model(inp, out, name="bytebite_v4")
    m.compile(optimizer=keras.optimizers.Adam(LR_V4),
              loss=keras.losses.Huber(),
              metrics=[keras.metrics.MeanAbsoluteError(name="mae")])
    return m, n_unf

model_v4, n_unf = build_v4(SEED)
print(f"v4 built | unfrozen backbone layers: {n_unf} | trainable params: "
      f"{sum(int(np.prod(w.shape)) for w in model_v4.trainable_weights):,}")

cbs_v4 = [keras.callbacks.EarlyStopping(monitor="val_loss", patience=PATIENCE_V4,
                                        mode="min", restore_best_weights=True, verbose=1),
          keras.callbacks.ReduceLROnPlateau(monitor="val_loss", factor=0.5,
                                            patience=3, mode="min", min_lr=1e-7)]
keras.utils.set_random_seed(SEED)
t0 = time.time()
print(f"Training v4 on {len(train_idx)} images at {IMG_V4[0]}px "
      f"(batch {BATCH_V4}, up to {EPOCHS_V4} epochs, ~143 steps/epoch)...")
hist_v4 = model_v4.fit(make_train_ds_v4(train_idx, SEED),
                       validation_data=make_eval_ds_v4(val_idx),
                       epochs=EPOCHS_V4, callbacks=cbs_v4, verbose=2)
print(f"v4 done: stopped {len(hist_v4.history['loss'])} | "
      f"best {int(np.argmin(hist_v4.history['val_loss'])) + 1} | "
      f"{(time.time() - t0) / 60:.1f} min")
print("Cell V4-2 complete.")

```

## Cell 6 - Validation gate, then test

Decision rule fixed BEFORE looking at results: v4 only replaces v3-FT if it improves VALIDATION MAE. The test set never participates in any decision - it is only reported afterwards. This prevents accidentally tuning on the test set. The cell loads the saved v3-FT model, scores both on validation, prints the gate verdict, then reports the full per-label test table in real units and saves the model.

```python
# =============================================================================
# CELL 6 (V4-3): VALIDATION GATE vs v3 FINE-TUNED BASELINE, THEN TEST REPORT
# =============================================================================
# Decision rule (pre-registered): v4 replaces v3-FT only if VALIDATION overall
# MAE improves. Test numbers are reported for the record either way.

pred_val_v4 = scaler.inverse(_pred_nut(model_v4.predict(make_eval_ds_v4(val_idx), verbose=0)))
val_mae_v4  = float(np.mean(np.abs(pred_val_v4 - y_val_raw)))

_m3 = keras.models.load_model(P_BASE_FT)
pred_val_v3 = scaler.inverse(_pred_nut(_m3.predict(make_eval_ds_192(val_idx), verbose=0)))
val_mae_v3  = float(np.mean(np.abs(pred_val_v3 - y_val_raw)))
del _m3

print(f"VALIDATION overall MAE: v3-FT {val_mae_v3:.2f} | v4 {val_mae_v4:.2f} | "
      f"delta {val_mae_v4 - val_mae_v3:+.2f}")
if val_mae_v4 < val_mae_v3:
    print("GATE: v4 improves validation -> v4 is the new candidate. "
          "Confirm with Cell V4-4 before claiming it.")
else:
    print("GATE: v4 does NOT improve validation -> keep v3-FT as the model. "
          "(Test numbers below are for the record only.)")

pred_test_v4 = scaler.inverse(_pred_nut(model_v4.predict(make_eval_ds_v4(test_idx), verbose=0)))
assert len(pred_test_v4) == 489, "must evaluate the FULL test set"
mae_v4  = np.mean(np.abs(pred_test_v4 - y_test_raw), axis=0)
rmse_v4 = np.sqrt(np.mean((pred_test_v4 - y_test_raw) ** 2, axis=0))
print(f"\n{'label':10} {'test MAE':>9} {'test RMSE':>10}")
for j, nname in enumerate(NUTRIENTS):
    print(f"{nname:10} {mae_v4[j]:9.2f} {rmse_v4[j]:10.2f}")
print(f"{'overall':10} {mae_v4.mean():9.2f} {rmse_v4.mean():10.2f}")
print("Reference: v3-FT test overall 22.21 (seed 42) / 21.59 (5-seed mean) | v1 29.57")

model_v4.save(P_V4_MODEL)
print(f"Saved: {P_V4_MODEL}")
print("Cell V4-3 complete.")

```

## Cell 7 - Five seeds and the ensemble

One seed can get lucky (that is exactly how the text-head idea briefly looked like a win), so the claimable number is the mean over 5 seeds (42-46). Sequentially per seed: build a fresh model with that seed's initialization, train with that seed's shuffle order, evaluate on the identical test set, save the model to disk, free GPU memory, next seed. Because every model is saved immediately, the loop resumes for free after any crash - already-saved seeds print 'reused'. At the end it reports the per-seed table, mean +/- std, and the ENSEMBLE: the five models' test predictions averaged per dish. The ensemble (17.33) beats every individual model (~18.1) because the five seeds make partly independent errors that cancel when averaged.

```python
# =============================================================================
# CELL 7 (V4-4): 5-SEED CONFIRMATION + ENSEMBLE (RUN ONLY IF THE V4-3 GATE PASSED)
# =============================================================================
# Trains seeds 43-46 fresh (42 reuses the model above), saves every model,
# reports per-seed test MAE and the 5-model prediction-average ensemble.
# Expect several hours on the L4. Safe to leave running.

import gc
V4_SEEDS = [42, 43, 44, 45, 46]
test_preds_v4, rows_v4 = [], []
t_all = time.time()
for s in V4_SEEDS:
    t_s = time.time()
    p_s = OUTPUT_DIR / f"v4_s{s}.keras"
    if s == SEED and P_V4_MODEL.exists():
        m = keras.models.load_model(P_V4_MODEL)
        print(f"seed {s}: reused {P_V4_MODEL.name}")
    elif p_s.exists():
        m = keras.models.load_model(p_s)
        print(f"seed {s}: reused {p_s.name}")
    else:
        m, _ = build_v4(s)
        cbs = [keras.callbacks.EarlyStopping(monitor="val_loss", patience=PATIENCE_V4,
                                             mode="min", restore_best_weights=True, verbose=0),
               keras.callbacks.ReduceLROnPlateau(monitor="val_loss", factor=0.5,
                                                 patience=3, mode="min", min_lr=1e-7)]
        keras.utils.set_random_seed(s)
        m.fit(make_train_ds_v4(train_idx, s),
              validation_data=make_eval_ds_v4(val_idx),
              epochs=EPOCHS_V4, callbacks=cbs, verbose=0)
        m.save(p_s)
        print(f"seed {s}: trained + saved {p_s.name}")
    pt = scaler.inverse(_pred_nut(m.predict(make_eval_ds_v4(test_idx), verbose=0)))
    test_preds_v4.append(pt)
    o = float(np.mean(np.abs(pt - y_test_raw)))
    _row = {"seed": s, "test_overall": o}
    _per = np.mean(np.abs(pt - y_test_raw), axis=0)
    for _j, _nn in enumerate(NUTRIENTS):
        _row[_nn] = float(_per[_j])
    rows_v4.append(_row)
    print(f"seed {s}: test overall {o:6.2f} | {(time.time() - t_s) / 60:.1f} min "
          f"| total {(time.time() - t_all) / 60:.1f} min")
    del m
    keras.backend.clear_session()
    gc.collect()

seed_tbl = pd.DataFrame(rows_v4)
ov = seed_tbl["test_overall"].to_numpy()
print("\n" + "=" * 60)
print(seed_tbl.round(3).to_string(index=False))
print(f"v4 5-seed test overall: {ov.mean():.2f} +/- {ov.std():.2f} "
      f"(v3-FT reference: 21.59)")

ens_pred = np.mean(test_preds_v4, axis=0)
ens_mae = np.mean(np.abs(ens_pred - y_test_raw), axis=0)
print(f"\nENSEMBLE (mean of {len(V4_SEEDS)} models) test MAE:")
for j, nname in enumerate(NUTRIENTS):
    print(f"  {nname:10} {ens_mae[j]:8.2f}")
print(f"  {'overall':10} {ens_mae.mean():8.2f}")
seed_tbl.to_csv(OUTPUT_DIR / "v4_multiseed.csv", index=False)
np.save(OUTPUT_DIR / "v4_ensemble_test_pred.npy", ens_pred)
print(f"\nSaved: outputs/v4_multiseed.csv, outputs/v4_ensemble_test_pred.npy, "
      f"v4_s43..46.keras")
print("Cell V4-4 complete.")
```

## Cell 8 - Report generation

Produces the shared report format: overall + per-label MAE/RMSE for train/val/test, actual-vs-predicted sample rows, a per-dish absolute-error distribution table, 5-run t-based confidence intervals per label (read from the multi-seed CSV), and the scatter/training-curve plots. Everything is written to outputs/ as a text file and PNGs.

```python
# =============================================================================
# CELL 8: FULL REPORT (SAIM'S FORMAT) + PER-DISH ERROR TABLE + PLOTS + FILES
# =============================================================================
# Works after Cell 6 (single seed). If Cell 7 ran, the 5-run CI block and
# ensemble numbers are included automatically.

L = []
def _rep(s=""):
    L.append(s)
    print(s)

_mv4 = keras.models.load_model(P_V4_MODEL)
_p_tr = scaler.inverse(_pred_nut(_mv4.predict(make_eval_ds_v4(train_idx), verbose=0)))
_p_va = scaler.inverse(_pred_nut(_mv4.predict(make_eval_ds_v4(val_idx),   verbose=0)))
_p_te = scaler.inverse(_pred_nut(_mv4.predict(make_eval_ds_v4(test_idx),  verbose=0)))
del _mv4

LABEL_NAMES = ["Calories", "Mass", "Fat", "Carb", "Protein"]
_rep("=" * 60)
_rep("  EVALUATION: ByteBite v4 - Fine-tuned EfficientNetB3, 300px + aug")
_rep("  (block4+ trainable, BN frozen, Adam 3e-5, seed-42 split, seed 42)")
_rep("=" * 60)
_rep()
for sname, yt, yp in [("TRAINING",   y_train_raw, _p_tr),
                      ("VALIDATION", y_val_raw,   _p_va),
                      ("TEST",       y_test_raw,  _p_te)]:
    _rep(f"{sname} - Overall MAE: {float(np.mean(np.abs(yt - yp))):.4f}  "
         f"Overall RMSE: {float(np.sqrt(np.mean((yt - yp) ** 2))):.4f}")
for sname, yt, yp in [("TRAINING",   y_train_raw, _p_tr),
                      ("VALIDATION", y_val_raw,   _p_va),
                      ("TEST",       y_test_raw,  _p_te)]:
    mae_per  = np.mean(np.abs(yt - yp), axis=0)
    rmse_per = np.sqrt(np.mean((yt - yp) ** 2, axis=0))
    _rep()
    _rep(f"--- {sname} SET ---")
    _rep("MAE per label:")
    for l, v in zip(LABEL_NAMES, mae_per):
        _rep(f"  {l}: {v:.4f}")
    _rep("RMSE per label:")
    for l, v in zip(LABEL_NAMES, rmse_per):
        _rep(f"  {l}: {v:.4f}")

results_df = pd.DataFrame({
    "actual_calories": y_test_raw[:, 0], "pred_calories": _p_te[:, 0],
    "actual_mass":     y_test_raw[:, 1], "pred_mass":     _p_te[:, 1],
    "actual_fat":      y_test_raw[:, 2], "pred_fat":      _p_te[:, 2],
    "actual_carb":     y_test_raw[:, 3], "pred_carb":     _p_te[:, 3],
    "actual_protein":  y_test_raw[:, 4], "pred_protein":  _p_te[:, 4]})
_rep()
_rep("Actual vs Predicted (first 5 rows):")
_rep(results_df.head().to_string())

_rep()
_rep("Per-dish overall absolute error, TEST set - describe():")
_per_dish = np.abs(_p_te - y_test_raw).mean(axis=1)
_rep(pd.Series(_per_dish, name="abs_error").describe().round(2).to_string())

_ms_path = OUTPUT_DIR / "v4_multiseed.csv"
if _ms_path.exists():
    _ms = pd.read_csv(_ms_path)
    if all(n in _ms.columns for n in NUTRIENTS) and len(_ms) >= 2:
        _rep()
        _rep("Confidence For ByteBite v4 (fine-tuned EfficientNetB3, 300px + aug)")
        _rep(f"=== CONFIDENCE INTERVALS ({len(_ms)} runs) ===")
        _TCRIT = {2: 12.706, 3: 4.303, 4: 3.182, 5: 2.776, 6: 2.571,
                  7: 2.447, 8: 2.365, 9: 2.306}          # t(0.975, df=n-1)
        _tcrit = _TCRIT.get(len(_ms) - 1, 1.960)
        for _nn, _ln in zip(NUTRIENTS, LABEL_NAMES):
            _v = _ms[_nn].to_numpy()
            _m, _sd = _v.mean(), _v.std(ddof=1)
            _hw = _tcrit * _sd / np.sqrt(len(_v))
            _rep(f"{_ln:9}: {_m:.2f} +/- {_hw:.2f} (95% CI: {_m - _hw:.2f} - {_m + _hw:.2f})")
        _ov = _ms["test_overall"].to_numpy()
        _rep(f"Overall  : {_ov.mean():.2f} +/- {_tcrit * _ov.std(ddof=1) / np.sqrt(len(_ov)):.2f}")
    else:
        _rep()
        _rep("NOTE: v4_multiseed.csv found but without per-label columns - re-run Cell 7.")
else:
    _rep()
    _rep("NOTE: multi-seed CI block skipped (run Cell 7 first for 5-run CIs).")

_rep()
_rep("Reference points (same seed-42 split, full 489-dish test set):")
_rep(f"  v1 frozen EfficientNetB3 : overall test MAE {BASELINE_OVERALL:.2f}")
_rep(f"  v3 fine-tuned (5 seeds)  : overall test MAE {V3FT_5SEED_OVERALL:.2f}")
_rep(f"  v4 (this notebook)       : overall test MAE {float(np.mean(np.abs(_p_te - y_test_raw))):.2f} (seed 42)")

with open(OUTPUT_DIR / "bytebite_v4_report.txt", "w") as f:
    f.write("\n".join(L) + "\n")
print(f"\nSaved: {OUTPUT_DIR / 'bytebite_v4_report.txt'}")

fig, axs = plt.subplots(1, 5, figsize=(22, 4))
fig.suptitle("Actual vs Predicted - Test Set (ByteBite v4)")
for j, l in enumerate(LABEL_NAMES):
    ax = axs[j]
    ax.scatter(y_test_raw[:, j], _p_te[:, j], s=6, alpha=0.4)
    lim = [0, max(y_test_raw[:, j].max(), _p_te[:, j].max()) * 1.05]
    ax.plot(lim, lim, "r--", linewidth=1, label="Perfect")
    ax.set_xlim(lim); ax.set_ylim(lim)
    ax.set_title(f"{l}\nMAE: {np.mean(np.abs(y_test_raw[:, j] - _p_te[:, j])):.2f}", fontsize=9)
    ax.set_xlabel(f"Actual {l}", fontsize=8)
    ax.set_ylabel(f"Predicted {l}", fontsize=8)
    ax.legend(fontsize=7)
plt.tight_layout()
plt.savefig(OUTPUT_DIR / "v4_scatter_test.png", dpi=150, bbox_inches="tight")
plt.show()

try:
    hb = hist_v4.history
    fig, axs = plt.subplots(1, 2, figsize=(14, 5))
    ep = range(1, len(hb["loss"]) + 1)
    axs[0].plot(ep, hb["loss"], label="Training Loss")
    axs[0].plot(ep, hb["val_loss"], label="Validation Loss")
    axs[0].set_title("Loss Over Epochs - ByteBite v4")
    axs[0].set_xlabel("Epoch"); axs[0].set_ylabel("Huber Loss"); axs[0].legend()
    axs[1].plot(ep, hb["mae"], label="Training MAE")
    axs[1].plot(ep, hb["val_mae"], label="Validation MAE")
    axs[1].set_title("MAE Over Epochs (z-units) - ByteBite v4")
    axs[1].set_xlabel("Epoch"); axs[1].set_ylabel("MAE"); axs[1].legend()
    plt.tight_layout()
    plt.savefig(OUTPUT_DIR / "v4_curves.png", dpi=150, bbox_inches="tight")
    plt.show()
    print(f"Saved: {OUTPUT_DIR / 'v4_curves.png'}")
except NameError:
    print("NOTE: hist_v4 not in this kernel - curve plot skipped.")
print("Cell 8 complete. Files: bytebite_v4_report.txt, v4_scatter_test.png"
      + (", v4_curves.png" if 'hist_v4' in dir() else ""))
```

## Results this notebook produced

Seed-42 split, full 489-dish test set, real units:

| model | overall test MAE |
|-------|------------------|
| v1 frozen EfficientNetB3 | 29.57 |
| v3 fine-tuned (5-seed mean) | 21.59 |
| v4 single model (5-seed mean) | 18.14 +/- 0.21 |
| v4 ensemble of 5 | 17.33 |

Per-label, v4 5-seed means: calories 49.32, mass 28.88, fat 3.83, carb 4.33, protein 4.32.

One honest caveat: v4 changed resolution, augmentation, and unfreeze depth together, so the gain is attributed to the combination - a per-change ablation would be needed to split the credit.
