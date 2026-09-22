# 02. Final pipeline

Two diagrams. The first is what happens on the phone every time someone scans a plate. The second is how the weights inside that phone were produced.

## On the phone: photo to numbers

```mermaid
flowchart TD
    P["Overhead photo<br/>camera or gallery"] --> R{"Portrait frame?"}
    R -->|"yes"| ROT["Rotate 90 degrees to landscape"]
    R -->|"no"| CROP
    ROT --> CROP["Centre-crop to the dataset's aspect ratio"]
    CROP --> S["Stretch to 300 x 300<br/>pixel values stay 0-255"]
    S --> BB["EfficientNetB3 backbone<br/>rescaling and normalization are inside the graph"]
    BB --> GAP["Global average pooling<br/>feature map to one vector"]
    GAP --> D1["Dense 256, ReLU"]
    D1 --> DO["Dropout 0.3<br/>does nothing at inference"]
    DO --> D5["Dense 5<br/>five z-scores"]
    D5 --> UN["real = z * sd + mu<br/>mu and sd from the TRAIN split only"]
    UN --> CL["Show negatives as 0<br/>a negative gram count is never real"]
    CL --> OUT["calories, mass, fat, carb, protein"]
    OUT --> LOG["Logged to that experience's history<br/>Fusion, Kitchen, or Glucose"]

    subgraph assets["Shipped in assets/"]
        T1["bytebite_v4.tflite<br/>float16, 22.3 MB"]
        T2["bytebite_model.json<br/>range, geometry, mu, sd, target order"]
        T3["fixture dish + expected answer"]
    end
    T2 -.-> CROP
    T2 -.-> UN
    T1 -.-> BB
```

### Stage by stage

1. **Rotate portrait to landscape.** Phones default to 3:4 portrait, but the Nutrition5k rig shot 4:3 landscape. Cropping a portrait photo to 4:3 would throw away almost half the plate. v4 trained with random 90-degree rotations, so the model doesn't care which way is up, and rotating loses nothing.
2. **Centre-crop to the dataset's aspect ratio, then stretch.** Training resized the full 4:3 frame to a 300x300 square with no crop, which squashes everything horizontally. The phone must squash the same way, or food looks a different size than it did in training. A square crop would be the natural instinct, and it would make portions look smaller and bias mass and calories low.
3. **Keep pixels in 0-255.** Keras' EfficientNetB3 does its own rescaling inside the model. Dividing by 255 in Kotlin would normalize twice and produce confident, plausible, wrong numbers. See [16](16-port-pitfalls-and-next-steps.md).
4. **Backbone, pooling, head.** The same layers as training. Global average pooling turns the spatial feature map into one vector describing the whole plate. That's why the model gives one answer per dish and has no per-ingredient breakdown.
5. **Dropout.** It's only active during training. At inference it passes values through unchanged.
6. **Un-standardize.** The head predicts z-scores (see [06](06-target-scaling.md)). The phone converts them back with the train-split mean and standard deviation, which travel in the JSON sidecar. Target order is `[calories, mass, fat, carb, protein]`, not alphabetical. Mixing it up swaps fat and carbs.
7. **Floor at zero.** On lean plates the regression head can dip slightly below zero (the app once showed "-0.2 g" fat). A negative gram count is never a real reading, so the display floors it at 0.
8. **Log it.** Each scan is stored in the history of the experience it came from. Inference runs on one background thread behind a mutex, because a LiteRT interpreter can't be called concurrently.

## Offline: how the weights were made

```mermaid
flowchart LR
    TR["Train split"] --> AUG["Augment every epoch<br/>random flips and 90-degree rotations"]
    AUG --> FWD["Forward pass at 300 px"]
    FWD --> LOSS["Huber loss<br/>on z-scored targets"]
    LOSS --> OPT["Adam, lr 3e-5<br/>blocks 4-7 and top conv train<br/>BatchNorm frozen"]
    OPT --> VAL{"Validation loss improved<br/>in the last 6 epochs?"}
    VAL -->|"yes"| AUG
    VAL -->|"flat 3 epochs"| LR["Halve the learning rate"]
    LR --> AUG
    VAL -->|"no"| STOP["Stop and restore<br/>best-epoch weights"]
    STOP --> GATE{"Beats v3-FT<br/>on validation?"}
    GATE -->|"yes"| KEEP["Keep. Train seeds 43-46.<br/>Report test once."]
    GATE -->|"no"| OLD["Keep v3-FT"]
    KEEP --> EXPORT["Export notebook:<br/>convert, measure, gate, write assets"]
```

Why each piece is there is covered in [08](08-loss-and-training.md) (loss, optimizer, stopping), [09](09-fine-tuning.md) (what trains), [10](10-resolution-and-augmentation.md) (augmentation), [11](11-validation-gate.md) (the gate), and [15](15-on-device-export.md) (export).

## Check yourself

<details>
<summary>Why does the phone crop at all, if training never cropped?</summary>

Training never cropped because every Nutrition5k frame already had the same 4:3 shape. Phone photos come in other shapes. Cropping to 4:3 first gives the phone the same starting shape, so the stretch to 300x300 distorts food exactly the way training did.
</details>

<details>
<summary>What goes wrong if the sidecar's mu and sd come from all 3,259 dishes instead of the train split?</summary>

The model learned to output z-scores relative to train-split statistics. Using different statistics shifts and rescales every prediction a little. The numbers still look reasonable, so nothing flags the error. It's also leakage: validation and test labels would have influenced the pipeline.
</details>
