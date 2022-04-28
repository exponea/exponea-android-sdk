## 🚀 Fetching Data

Exponea Android SDK has some methods to retrieve your data from the Exponea web application.
All the responses will be available in the `onSuccess` and `onFailure` callback properties.

#### Get customer recommendation

Get items recommended for a customer.

```
fun fetchRecommendation(
        recommendationOptions: CustomerRecommendationOptions,
        onSuccess: (Result<ArrayList<CustomerRecommendation>>) -> Unit,
        onFailure: (Result<FetchError>) -> Unit
)
```

Resulting `CustomerRecommendation` contains system data from recommendation engine and `itemId` as defined on Exponea servers. Custom properties set for the item are returned in `data` map.

#### 💻 Usage

```
// Preparing the data.
val recommendationOptions = CustomerRecommendationOptions(
        id = "<recommendation_id>",
        fillWithRandom = true,
        size = 10
)

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

#### Consent Categories

Fetch the list of your existing consent categories.

```
fun getConsents(
        onSuccess: (Result<ArrayList<Consent>>) -> Unit,
        onFailure: (String) -> Unit
)
```

#### 💻 Usage

```
// Call getConsents to get the consent categories.
Exponea.getConsents(
        onSuccess = {
			// SDK will return a list of Consent objects.
        },
        onFailure = {
			// SDK will return a FetchError object.
        }
)
```
