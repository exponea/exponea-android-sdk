## 🔍 Flush events

Flushing is the process of uploading tracking events to Exponea servers.
All tracked events and customer properties are stored in the internal database of the Exponea SDK and later flushed based on flushing settings. When an event is successfully sent to Exponea API, the object will be deleted from the local database.

> By default, Exponea SDK automatically takes care of flushing events to the Exponea API. This feature can be turned off setting the property FlushMode to MANUAL. Please be careful with turning automatic flushing off because if you turn it off, you need to manually call Exponea.flush() to flush the tracked events manually every time there is something to flush.

Exponea SDK will only flush data when the device has a stable internet connection. If a connection/server error occurs when flushing the data, it will keep the data stored until it can be flushed at a later time.

You can configure the flushing mode to work differently to suit your needs.

#### 🔧 Flush Configuration
```
enum class FlushMode {
    /**
     * Periodic mode flushes data in your specified interval(60 minutes).
     * Flushing only happens while app is running.
     * When app enters background, all remaining events are flushed.
     */
    PERIOD,

    /**
     * All events will be flushed once the applications enters background.
     */
    APP_CLOSE,

    /**
     * Manual flushing mode disables any automatic upload and it's your responsibility to flush data.
     */
    MANUAL,

    /**
     * [ DEFAULT VALUE ]
     * Flushes all data immediately as it is received.
     */
    IMMEDIATE
}
```

It's possible to change the flushing period by setting the property flushPeriod on Exponea object. The default value is 60 minutes. Due to platform/implementation limitations, minimum value is 15 minutes.

The Exponea SDK Flush service will retry to flush events recorded in the database in case of a failure, but if the maximum limit of retries has achieved, the SDK will delete the specific event from the database and will not try to send it again. You can configure this value by setting the property maxTries in the Exponea Configuration.

#### Manual Flush
To manually trigger a data flush to the API, use the following method.


```
fun flushData()
```

#### 💻 Usage
```
Exponea.flushData()
```
