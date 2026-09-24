# IEEE RTC 2026 — Presentation Recap

**Event:** IEEE Real-Time Communications Conference and Expo at Illinois Tech (IIT), Chicago, IL
**Dates:** September 22–23, 2026
**Project:** ByteBite — A Deep Learning-Based Tool for Real-Time Estimation of Food Nutritional Components to Support Diabetes Self-Management
**Presenters:** Saim Sultan, Ismail Abu-Shanab
**Mentor:** Dr. Husam Ghazaleh
**Funding:** NSSRP, Benedictine University

## What we presented

ByteBite estimates a meal's calories, mass, carbohydrates, protein, and fat from a
single overhead smartphone photo, with no manual logging. The pitch to attendees:
carbohydrate tracking is central to managing blood glucose, but manual-entry apps are
tedious enough that most patients abandon them. One photo replaces the whole
search-and-enter workflow.

## Talking points that landed

- **The friction argument.** Framing the problem around abandonment (not accuracy alone)
  resonated with the clinical-leaning attendees.
- **On-device, offline.** No network round trip means no latency and no data leaving the
  phone. This drew the most follow-up questions.
- **SHAP attribution.** Showing that the model reads the plate rather than the tray or
  background made the "is it actually learning food?" question easy to answer.

## Takeaways for next time

- Lead with a live demo photo, not the architecture table.
- Have a one-sentence answer ready for "how is this different from MyFitnessPal?"
  (Answer: zero manual entry, and it estimates mass, which portion-size apps cannot.)
- Bring a printed error table; people wanted concrete MAE numbers per nutrient.
