# Social / LinkedIn Posts

Announcement copy for sharing the IEEE RTC 2026 presentation.

## Post 1 — presentation announcement

40 million Americans manage diabetes, and the daily grind of logging every meal is why most
tracking apps get abandoned. So we built ByteBite: a deep learning tool that estimates a
meal's full nutrition (calories, carbs, protein, fat, and mass) from a single overhead
photo, no manual entry required.

I presented it this week at the IEEE RTC Conference at Illinois Tech. We trained three CNN
architectures on Google's Nutrition5k dataset, and our best model (a fine-tuned
EfficientNetB3) beat the existing public baseline on carbs and protein. We ran SHAP
attribution to verify the network actually reads the food and not the background, and
packaged it into an Android prototype that runs fully on-device and offline.

Huge thanks to my mentor Dr. Husam Ghazaleh, and to NSSRP and Benedictine University for
backing this work. Grateful to have built it with my partner Saim Sultan.

#IEEE #MachineLearning #DeepLearning #ComputerVision #DiabetesTech

## Post 2 — follow-up (what's next)

A quick follow-up on ByteBite, the meal-nutrition-from-a-photo project I presented at IEEE
RTC this week.

The version we demoed is only the start. Nutrition5k ships overhead depth maps we have not
used yet, and depth is exactly what you need to turn a flat image into a real portion-size
estimate. Next on the roadmap: depth-informed mass estimation, per-prediction uncertainty
so the app can say how sure it is, and a Core ML build so this runs natively on iPhone.

Still amazed by how much a small team can ship in a summer. Thank you again to Dr. Husam
Ghazaleh and the NSSRP program at Benedictine University.

#MachineLearning #EdgeAI #iOS #CoreML #DiabetesTech
