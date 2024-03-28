## App Inbox

Exponea SDK feature App Inbox allows you to use message list in your app. You can find information on creating your messages in [Exponea documentation](https://documentation.bloomreach.com/engagement/docs/app-inbox).

### Using App Inbox

Only required step to use App Inbox in your application is to add a button into your screen. Messages are then displayed by clicking on a button:

```kotlin
val button = Exponea.getAppInboxButton(context)
button?.let {
    yourContainerView.addView(it, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))
}
```

App Inbox button has registered a click action to show an Activity `AppInboxListActivity`.

> Always check for retrieved button instance nullability. Button cannot be build for non-initialized Exponea SDK.

No more work is required for showing App Inbox but may be customized in multiple ways.

## Default App Inbox behavior

Exponea SDK is fetching and showing an App Inbox for you automatically in default steps:

1. Shows a button to access App Inbox list (need to be done by developer)
2. Shows a screen for App Inbox list. Each item is shown with:
   1. Flag if message is read or unread
   2. Delivery time in human-readable form (i.e. `2 hours ago`)
   3. Single-lined title of message ended by '...' for longer value
   4. Two-lined content of message ended by '...' for longer value
   5. Squared image if message contains any
   6. Shows a loading state of list (indeterminate progress)
   7. Shows an empty state of list with title and message
   8. Shows an error state of list with title and description
3. Screen for App Inbox list calls a `Exponea.trackAppInboxOpened` on item click and marks message as read automatically
4. Shows a screen for App Inbox message detail that contains:
   1. Large squared image. A gray placeholder is shown if message has no image
   2. Delivery time in human-readable form (i.e. `2 hours ago`)
   3. Full title of message
   4. Full content of message
   5. Buttons for each reasonable action (actions to open browser link or invoking of universal link). Action that just opens current app is meaningless so is not listed
5. Screen for message detail calls `Exponea.trackAppInboxClick` on action button click automatically

> The behavior of `trackAppInboxOpened` and `trackAppInboxClick` may be affected by the tracking consent feature, which in enabled mode considers the requirement of explicit consent for tracking. Read more in [tracking consent documentation](./TRACKING_CONSENT.md).

### Localization

Exponea SDK contains only texts in EN translation. To modify this or add a localization, you are able to define customized strings (i.e. in your `strings.xml` files)

```xml
<string name="exponea_inbox_button">Inbox</string>
<string name="exponea_inbox_title">Inbox</string>
<string name="exponea_inbox_defaultTitle">Inbox message</string>
<string name="exponea_inbox_emptyTitle">Empty Inbox</string>
<string name="exponea_inbox_emptyMessage">You have no messages yet.</string>
<string name="exponea_inbox_errorTitle">Something went wrong :(</string>
<string name="exponea_inbox_errorMessage">We could not retrieve your messages.</string>
<string name="exponea_inbox_mainActionTitle">See more</string>
```

### UI components styling

App Inbox screens are designed with love and to fulfill customers needs but may not fit design of your application. Please check these style-rules that are applied to UI components and override them (i.e. in your `styles.xml` files)

```xml
<style name="Theme.AppInboxListActivity" parent="@style/Theme.AppCompat.NoActionBar">
    <!-- Screen shows App Inbox list -->
    <!-- This style is applied to 'android:theme' activity parameter -->
</style>
<style name="Theme.AppInboxDetailActivity" parent="@style/Theme.AppCompat.NoActionBar">
    <!-- Screen shows App Inbox message detail -->
    <!-- This style is applied to 'android:theme' activity parameter -->
</style>
<style name="AppInboxItemReceivedTime">
    <!-- TextView shows delivery time in App Inbox list -->
    <!-- This style is applied to 'style' parameter -->
</style>
<style name="AppInboxItemTitle">
    <!-- TextView shows title in App Inbox list -->
    <!-- This style is applied to 'style' parameter -->
</style>
<style name="AppInboxItemContent">
    <!-- TextView shows message content in App Inbox list -->
    <!-- This style is applied to 'style' parameter -->
</style>
<style name="AppInboxDetailReceivedTime">
    <!-- TextView shows delivery time in message detail -->
    <!-- This style is applied to 'style' parameter -->
</style>
<style name="AppInboxDetailTitle">
    <!-- TextView shows title in message detail -->
    <!-- This style is applied to 'style' parameter -->
</style>
<style name="AppInboxDetailContent">
    <!-- TextView shows message content in message detail -->
    <!-- This style is applied to 'style' parameter -->
</style>
<style name="Theme.AppInboxAppBarOverlay" parent="ThemeOverlay.AppCompat.Dark.ActionBar">
    <!-- AppBarLayout is shown in both list and detail screens -->
    <!-- This style is applied to 'android:theme' parameter -->
</style>
<style name="Theme.AppInboxAppBarPopupOverlay" parent="ThemeOverlay.AppCompat.Light">
    <!-- Toolbar is shown inside AppBarLayout -->
    <!-- This style is applied to 'app:popupTheme' parameter -->
</style>
<style name="AppInboxButton" parent="@style/Widget.AppCompat.Button">
    <!-- Button opens App Inbox list screen -->
    <!-- This style is applied to 'style' button parameter -->
</style>
<style name="AppInboxActionButton" parent="@style/Widget.AppCompat.Button">
    <!-- Button invokes App Inbox message action in detail -->
    <!-- This style is applied to 'style' parameter -->
</style>
<style name="AppInboxEmptyStatusTitle">
    <!-- TextView shows title for App Inbox list empty state -->
    <!-- This style is applied to 'style' parameter -->
</style>
<style name="AppInboxEmptyStatusMessage">
    <!-- TextView shows message for App Inbox list empty state -->
    <!-- This style is applied to 'style' parameter -->
</style>
<style name="AppInboxErrorStatusTitle">
    <!-- TextView shows title for App Inbox list error state -->
    <!-- This style is applied to 'style' parameter -->
</style>
<style name="AppInboxErrorStatusMessage">
<!-- TextView shows message for App Inbox list error state -->
<!-- This style is applied to 'style' parameter -->
</style>
<style name="AppInboxStatusLoading" parent="android:Widget.DeviceDefault.ProgressBar.Large">
    <!-- ProgressBar shown for App Inbox list loading state -->
    <!-- This style is applied to 'style' parameter -->
</style>
```

## Advanced customization

If you want to override UI elements more, you are able to register your own `AppInboxProvider` implementation:

```kotlin
Exponea.appInboxProvider = ExampleAppInboxProvider()
```

> You may register your own provider at any time - before Exponea SDK init or later in some of your screens. Every action in scope of App Inbox is using currently registered provider instance. Nevertheless, we recommend to set your provider right after Exponea SDK initialization.

`AppInboxProvider` instance must contain implementation for building of all UI components, but you are allowed to extend from SDKs `DefaultAppInboxProvider` and re-implement only UI views that you need to.

### Building App Inbox button

Method `getAppInboxButton(Context)` is used to build a `android.widget.Button` instance.
Default implementation builds a simple button instance with icon ![INBOX](./inbox.png) and `exponea_inbox_button` text and styled by `AppInboxButton` style.
Click action for that button is set to open `AppInboxListActivity` screen.
To override this behavior you are able to write own method:

```kotlin
override fun getAppInboxButton(context: Context): Button {
    // reuse a default button or create your own
    val appInboxButton = super.getAppInboxButton(context)
    // apply your setup
    appInboxButton.setTextColor(Color.BLACK)
    // return instance
    return appInboxButton
}
```

### Building App Inbox list View

Method `getAppInboxListView(Context, (MessageItem, Int) -> Unit)` is used to build a `android.view.View` to show an App Inbox messages list. All data handling has to be done here (fetching, showing data, onItemClicked listeners...).
Default implementation builds a simple View that shows data in `androidx.recyclerview.widget.RecyclerView` and empty or error state is shown by other `android.widget.TextView` elements.
On-item-clicked action for each item is set to open `AppInboxDetailActivity` for `MessageItem` value.
To override this behavior you are able to write own method:

```kotlin
override fun getAppInboxListView(context: Context, onItemClicked: (MessageItem, Int) -> Unit): View {
    // reuse a default view or create your own
    val appInboxListView = super.getAppInboxListView(context, onItemClicked)
    // you are able to access default class impl by casting
    val typedView = appInboxListView as AppInboxListView
    // apply your setup to any element
    typedView.statusErrorTitleView.setTextColor(Color.RED)
    // return instance
    return typedView
}
```

> Event tracking `Exponea.trackAppInboxOpened` and message read state `Exponea.markAppInboxAsRead` are called by clicking on item. Please call these methods in your customized implementation to keep a proper App Inbox behavior.

### Building App Inbox list Fragment

Method `getAppInboxListFragment(Context)` is used to build a `androidx.fragment.app.Fragment` that is showing an App Inbox list view (created by previous method).
To override this behavior you are able to write own method:

```kotlin
override fun getAppInboxListFragment(context: Context): Fragment {
    // reuse a default fragment or create your own
    val appInboxListFragment = super.getAppInboxListFragment(context)
    // you are able to access default class impl by casting
    val typedFragment = appInboxListFragment as AppInboxListFragment
    // there isn't a lot of things to do :-) but animations may be customized here
    typedFragment.enterTransition = Fade()
    typedFragment.returnTransition = Fade()
    // return instance
    return typedFragment
}
```

### Building App Inbox detail View

Method `getAppInboxDetailView(Context, String)` is used to build a `android.view.View` to show an App Inbox message detail. All data handling has to be done here (fetching, showing data, action listeners...).
Default implementation builds a simple View that shows data by multiple `android.widget.TextView`s and `android.widget.ImageView`, whole layout wrapped by `android.widget.ScrollView`.
App Inbox message actions are shown and invoked by multiple `android.widget.Button`s.
To override this behavior you are able to write own method:

```kotlin
override fun getAppInboxDetailView(context: Context, messageId: String): View {
    // reuse a default view or create your own
    val appInboxDetailView = super.getAppInboxDetailView(context, messageId)
    // you are able to access default class impl by casting
    val typedView = appInboxDetailView as AppInboxDetailView
    // apply your setup to any element
    typedView.titleView.textSize = 32f
    // return instance
    return appInboxDetailView
}
```

> Event tracking `Exponea.trackAppInboxClick` is called by clicking on action. Please call these methods in your customized implementation to keep a proper App Inbox behavior.

### Building App Inbox detail Fragment

Method `getAppInboxDetailFragment(Context, String)` is used to build a `androidx.fragment.app.Fragment` that is showing an App Inbox detail view (created by previous method).
To override this behavior you are able to write own method:

```kotlin
override fun getAppInboxDetailFragment(context: Context, messageId: String): Fragment {
    // reuse a default view or create your own
    val appInboxDetailFragment = super.getAppInboxDetailFragment(context, messageId)
    // you are able to access default class impl by casting
    val typedFragment = appInboxDetailFragment as AppInboxDetailFragment
    // some animations may be customized here
    typedFragment.enterTransition = Fade()
    typedFragment.returnTransition = Fade()
    // return instance
    return appInboxDetailFragment
}
```

## App Inbox data API

Exponea SDK provides methods to access App Inbox data directly without accessing UI layer at all.

### App Inbox load

App Inbox is assigned to existing customer account (defined by hardIds) so App Inbox is cleared in case of:

- calling any `Exponea.identifyCustomer` method
- calling any `Exponea.anonymize` method

To prevent a large data transferring on each fetch, App Inbox are stored locally and next loading is incremental. It means that first fetch contains whole App Inbox but next requests contain only new messages. You are freed by handling such a behavior, result data contains whole App Inbox but HTTP request in your logs may be empty for that call.
List of assigned App Inbox is done by

```kotlin
Exponea.fetchAppInbox { data ->
    if (data == null) {
        Logger.e(this, "Error while loading AppInbox")
        return@fetchAppInbox
    }
    if (data.isEmpty()) {
        Logger.i(this, "AppInbox loaded but is empty")
        return@fetchAppInbox
    }
    Logger.i(this, "AppInbox loaded")
}
```

Exponea SDK provides API to get single message from App Inbox. To load it you need to pass a message ID:

```kotlin
Exponea.fetchAppInboxItem(messageId, { message ->
    message?.let {
        Logger.i(this, "AppInbox message found and loaded")
    }
})
```
Fetching of single message is still requesting for fetch of all messages (including incremental loading). But message data are returned from local repository in normal case (due to previous fetch of messages).

### App Inbox message read state

To set an App Inbox message read flag you need to pass a message ID:
```kotlin
Exponea.markAppInboxAsRead(message) { marked ->
    Logger.i(this, "AppInbox message marked as read: $marked")
}
```
> Marking a message as read by `markAppInboxAsRead` method is not invoking a tracking event for opening a message. To track an opened message, you need to call `Exponea.trackAppInboxOpened` method.

## Tracking events for App Inbox

Exponea SDK default behavior is tracking the events for you automatically. In case of your custom implementation, please use tracking methods in right places.

### Tracking opened App Inbox message

To track an opening of message detail, you should use method `Exponea.trackAppInboxOpened(MessageItem)` with opened message data.
The behaviour of `trackAppInboxOpened` may be affected by the tracking consent feature, which in enabled mode considers the requirement of explicit consent for tracking. Read more in [tracking consent documentation](./TRACKING_CONSENT.md).
If you want to avoid to consider tracking, you may use `Exponea.trackAppInboxOpenedWithoutTrackingConsent` instead. This method will do track event ignoring tracking consent state.

### Tracking clicked App Inbox message action

To track an invoking of action, you should use method `Exponea.trackAppInboxClick(MessageItemAction, MessageItem)` with clicked message action and data.
The behaviour of `trackAppInboxClick` may be affected by the tracking consent feature, which in enabled mode considers the requirement of explicit consent for tracking. Read more in [tracking consent documentation](./TRACKING_CONSENT.md).
If you want to avoid to consider tracking, you may use `Exponea.trackAppInboxClickWithoutTrackingConsent` instead. This method will do track event ignoring tracking consent state.
