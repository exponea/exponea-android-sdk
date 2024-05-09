---
title: Data Flushing
excerpt: Learn how the Android SDK uploads data to the Engagement API and how to customize this behavior
slug: android-sdk-data-flushing
categorySlug: integrations
parentDocSlug: android-sdk-setup
---

## Data Flushing
The SDK caches data (sessions, events, customer properties, etc.) in an internal database and periodically sends it to the Engagement API. After the data has been uploaded, the values in the Engagement web app are updated, and the cached data is removed from the SDK's internal database. This process is called **data flushing**.

By default, the SDK automatically flushes the data as soon as it is tracked or when the application is backgrounded. You can configure the [flushing mode](#flushing-modes) to customize this behavior to suit your needs.
 
 You can also turn off automatic flushing completely. In this case, you must [manually flush](#manual-flushing) every time there is data to flush.

The SDK will only flush data when the device has a stable network connection. If a connection error occurs while flushing, it will keep the data cached until the connection is stable and the data is flushed successfully.

## Flushing Modes

The SDK supports the following 4 flushing modes (defined in `com.exponea.sdk.models.FlushMode`) to specify how often or if data is flushed automatically.

| Name                  | Description |
| --------------------- | ----------- |
| `IMMEDIATE` (default) | Flushes all data immediately as it is received. |
| `APP_CLOSE`           | Flush data any time the application resigns active state. |
| `PERIOD`              | Flushes data in the interval specified in `Exponea.flushPeriod` and when the application is closed or goes to the background. |
| `MANUAL`              | Disables any automatic upload. It's the responsibility of the developer to [flush data manually](#manual-flushing). |

To set the flushing mode, [initialize the SDK](https://documentation.bloomreach.com/engagement/docs/android-sdk-setup) first, then set `flushMode` directly on the `Exponea` singleton:

```swift
Exponea.flushMode = FlushMode.MANUAL
```

When using flush mode `PERIOD`, the default interval is 60 minutes. To specify a different interval using a `com.exponea.sdk.models.FlushPeriod` value, set `flushPeriod` on the `Exponea` singleton:

```swift
Exponea.flushMode = FlushMode.PERIOD
Exponea.flushPeriod = FlushPeriod(10, TimeUnit.MINUTES)
```

## Manual Flushing

To manually trigger a data flush to the API, use the following method:

```swift
Exponea.flushData()
```
