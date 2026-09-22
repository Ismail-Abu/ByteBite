# 08. Loss and training loop

## Huber loss

Huber loss behaves like squared error for small mistakes and like absolute error for large ones:

```
error e = prediction - target (in z-units)

L(e) = 0.5 * e^2              if |e| <= delta   (delta = 1.0)
     = delta * (|e| - 0.5*delta)  otherwise
```

Why not plain MSE? MSE squares errors, so a dish that's 5 standard deviations off contributes 25 times the loss of one that's 1 standard deviation off. Nutrition5k has extreme dishes: very large trays, dense desserts, strange single items. With MSE, a handful of them would dominate the gradient, and the model would bend toward them at the expense of ordinary plates.

Why not plain MAE? MAE's gradient has the same size whether the error is tiny or large. Near the answer it doesn't shrink, so training jitters around the minimum instead of settling. It also has a kink at zero.

Huber takes the useful half of each. Near zero it's smooth like MSE, so it converges cleanly. Far out it's linear like MAE, so outliers pull with bounded force. Because targets are z-scored ([06](06-target-scaling.md)), delta = 1.0 means "one standard deviation" for every target.

## Adam

Adam keeps a running average of each weight's gradient (momentum) and of its squared gradient (scale), and uses them to give every weight its own step size. For fine-tuning a pretrained network, that means:

- rarely-updated weights still get useful steps
- noisy gradients from batches of 16 are smoothed
- one base learning rate works reasonably across layers with very different gradient sizes

The base learning rate for v4 is 3e-5, small on purpose. See [09](09-fine-tuning.md).

## Early stopping with restored weights

```python
EarlyStopping(monitor="val_loss", patience=6, restore_best_weights=True)
```

- **Watch validation loss, not training loss.** Training loss almost always keeps falling. Validation loss tells you whether the model is still getting better on dishes it doesn't train on.
- **Patience 6.** Validation loss is noisy from epoch to epoch. Stopping at the first bad epoch would stop too early. Six epochs without a new best is strong evidence the model has plateaued or started overfitting.
- **Restore best weights.** Without this, the saved model is the last epoch, which by definition is six epochs past the best one. With it, the final model is the checkpoint that generalized best.
- **Cost.** The run is capped at 60 epochs, but early stopping usually ends it well before that. The cost control is built into the script rather than depending on someone watching the log.

## ReduceLROnPlateau

```python
ReduceLROnPlateau(monitor="val_loss", factor=0.5, patience=3, min_lr=1e-7)
```

If validation loss hasn't improved for 3 epochs, halve the learning rate. The idea: when big steps stop helping, the model may be bouncing around a minimum. Smaller steps can settle into it.

Its patience (3) is deliberately shorter than early stopping's (6). The learning rate gets at least one cut, with a few epochs to take effect, before training gives up. The two callbacks work in sequence: slow down first, and stop only if slowing down didn't help.

## Batch size 16

At 300x300 with blocks 4-7 unfrozen, activation memory grows fast, and 16 fits comfortably on the L4 GPU used for training. Small batches also add a bit of gradient noise, which acts as mild regularization. The side effect is that BatchNorm statistics estimated from 16 images are unreliable, which is why BatchNorm stays frozen ([09](09-fine-tuning.md)).

## Built to survive crashes

Training is fully sequential: one model at a time, and one seed at a time inside the multi-seed cell. Each model is saved as soon as it finishes. If the session dies at seed 45, rerunning the cell prints "reused" for seeds 42-44 and continues from there. On rented GPUs, which can disconnect, that saves hours.

## Check yourself

<details>
<summary>A dish is 4 standard deviations off. How much more does it contribute to the loss than a dish 1 SD off, under MSE and under Huber?</summary>

MSE: 16 against 1, so 16 times. Huber with delta 1: (4 - 0.5) = 3.5 against 0.5, so 7 times. Huber roughly halves the pull of that outlier here, and the gap grows the further out the dish is.
</details>

<details>
<summary>Why is ReduceLROnPlateau's patience shorter than early stopping's?</summary>

So the learning rate gets cut at least once, with time to take effect, before training is abandoned. If both used 6, training could stop without ever trying a smaller step.
</details>
