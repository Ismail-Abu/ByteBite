# Converting the Model to Core ML

The trained model is a Keras EfficientNetB3 regressor (see the notebooks). To run it on
iPhone we convert it to a Core ML `.mlpackage` with `coremltools`.

## Conversion outline

```python
import coremltools as ct
import tensorflow as tf

keras_model = tf.keras.models.load_model("bytebite_efficientnetb3.keras")

mlmodel = ct.convert(
    keras_model,
    source="tensorflow",
    inputs=[ct.ImageType(
        name="image",
        shape=(1, 300, 300, 3),   # EfficientNetB3 input size
        scale=1/255.0,            # match training preprocessing
        bias=[0, 0, 0],
    )],
    convert_to="mlprogram",       # ML Program => runs on the Neural Engine
    compute_units=ct.ComputeUnit.ALL,
)
mlmodel.save("ByteBite.mlpackage")
```

## Critical correctness notes

- **Preprocessing must match training exactly.** If training used
  `efficientnet.preprocess_input`, replicate that scale/bias here, not a naive `/255`.
  A mismatch silently shifts every prediction.
- **Outputs are standardized.** The model predicts z-scored targets. Store the training-set
  mean and standard deviation for each of the five nutrients and de-standardize on device:
  `value = z * std + mean`. Ship these constants alongside the model as JSON.
- **Five-output regression head.** Confirm the output order (calories, mass, carbs, protein,
  fat) is documented and read in the same order in Swift.

## Validation gate

Before shipping, run the same 490 test images through both the Python model and the Core ML
model and assert the per-nutrient MAE differs by less than 1%. Convert this into a small CI
check so a future model swap cannot regress the iOS numbers unnoticed.

## Size and quantization

EfficientNetB3 in float16 is small enough to bundle in the app. If size becomes an issue,
palettize weights with `coremltools.optimize`, but re-run the validation gate afterward —
quantization can move nutrient estimates.
