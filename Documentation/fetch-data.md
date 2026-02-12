---
title: Fetch data for Android SDK
excerpt: Fetch data from Bloomreach Engagement using the Android SDK
slug: android-sdk-fetch-data
categorySlug: integrations
parentDocSlug: android-sdk
---

The SDK provides methods to retrieve data from the Engagement platform. Responses are available in the `onSuccess` and `onFailure` callback properties.

## Fetch recommendations

Use the `fetchRecommendation` method to get personalized recommendations for the current customer from an Engagement [recommendation model](https://documentation.bloomreach.com/engagement/docs/recommendations).

The method returns a list of `CustomerRecommendation` objects containing the recommendation engine data and recommended item IDs.

### Arguments

| Name    | Type                          | Description |
| ------- | ----------------------------- | ----------- |
| options | CustomerRecommendationOptions | Recommendation options (see below for details )

### CustomerRecommendationOptions

| Name                       | Type                | Description |
| -------------------------- | ------------------- | ----------- |
| id (required)              | String              | ID of your recommendation model. |
| fillWithRandom             | Boolean             | If true, fills the recommendations with random items until size is reached. This is utilized when models cannot recommend enough items. |
| size                       | Int                 | Specifies the upper limit for the number of recommendations to return. Defaults to 10. |
| items                      | Map<String, String> | If present, the recommendations are related not only to a customer, but to products with IDs specified in this array. Item IDs from the catalog used to train the recommendation model must be used. Input product IDs in a dictionary as `[product_id: weight]`, where the value weight determines the preference strength for the given product (bigger number = higher preference).<br/><br/>Example:<br/>`["product_id_1": "1", "product_id_2": "2",]` |
| noTrack                    | Boolean             | Default value: false |
| catalogAttributesWhitelist | List<String>        | Returns only the specified attributes from catalog items. If empty or not set, returns all attributes.<br/><br/>Example:<br/>`["item_id", "title", "link", "image_link"]` |


### Example

```kotlin
// Prepare the recommendation options
val recommendationOptions = CustomerRecommendationOptions(
        id = "<recommendation_id>",
        fillWithRandom = true,
        size = 10
)

// Get recommendations for the current customer
Exponea.fetchRecommendation(
        customerRecommendation = recommendation, 
        onSuccess = {
            // SDK will return a list of a CustomerRecommendation objects.
        },
        onFailure = {
            // SDK will return a FetchError object.
        }
)
```

### Result object

#### CustomerRecommendation

| Name                    | Type                     | Description |
| ----------------------- | ------------------------ | ----------- |
| engineName              | String                   | Name of the recommendation engine used. |
| itemId                  | String                   | ID of the recommended item. |
| recommendationId        | String                   | ID of the recommendation engine (model) used. |
| recommendationVariantId | String                   | ID of the recommendation engine variant used. |
| data                    | Map<String, JsonElement> | The recommendation engine data and recommended item IDs returned from the server. |

## Fetch consent categories

Use the `getConsents` method to get a list of your consent categories and their definitions.

Use when you want to get a list of your existing consent categories and their properties, such as sources and translations. This is useful when rendering a consent form.

The method returns a list of `Consent` objects.

### Example

```kotlin
Exponea.getConsents(
        onSuccess = {
            // SDK will return a list of a Consent objects.
        },
        onFailure = {
            // SDK will return a FetchError object.
        }
)

```

### Result object

#### Consent

| Name               | Type                                     | Description |
| -------------------| ---------------------------------------- | ----------- |
| id                 | String                                   | Name of the consent category. |
| legitimateInterest | Boolean                                  | If the user has legitimate interest. |
| sources            | ConsentSources                           | The sources of this consent. |
| translations       | HashMap<String, HashMap<String, String>> | Contains the translations for the consent.<br/><br/>Keys of this dictionary are the short ISO language codes (eg. "en", "cz", "sk"...) and the values are dictionaries containing the translation key as the dictionary key and translation value as the dictionary value. |

#### ConsentSources

| Name                  | Type | Description |
| ----------------------| -------------------------- | ----------- |
| createdFromCRM      | Boolean | Manually created from the web application. |
| imported            | Boolean | Imported from the importing wizard. |
| fromConsentPage     | Boolean | Tracked from the consent page. |
| privateAPI          | Boolean | API which uses basic authentication. |
| publicAPI           | Boolean | API which only uses public token for authentication. |
| trackedFromScenario | Boolean | Tracked from the scenario from event node. |
