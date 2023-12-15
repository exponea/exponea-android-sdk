## In-app content blocks
Exponea SDK allows you to display native In-app content blocks based on definitions set up on the Exponea web application. You can find information on creating your messages in [Exponea documentation](https://documentation.bloomreach.com/engagement/docs/in-app-content-blocks)

In-app content block will be shown exactly where you'll place a placeholder UI view. You can get a placeholder view from API:

```kotlin
val placeholderView = Exponea.getInAppContentBlocksPlaceholder("placeholder_1", activityContext)
yourLayout.addView(placeholderView)
```

No more developer work is required; they work automatically after the SDK is initialized.
In-app content blocks are shown within placeholder view by its ID automatically based on conditions setup on the Exponea backend. Once a message passes those filters, the SDK will try to present the message.

### If displaying In-app content blocks has delay

Message is able to be shown only if it is fully loaded and also its images are loaded too. In case that message is not yet fully loaded (including its images) then you may experience delayed showing.

If you need to show In-app content block as soon as possible (ideally instantly) you may set a auto-prefetch of placeholders. In-app content blocks for these placeholders are loaded immediately after SDK initialization.

```kotlin
val configuration = ExponeaConfiguration(
// your configuration goes here
)
configuration.inAppContentBlockPlaceholdersAutoLoad = listOf("placeholder_1", "placeholder_2", ...)
Exponea.init(App.instance, configuration)
```

### If displaying In-app content blocks needs delay

Message content starts to be loaded while creating a placeholder view (from `Exponea.getInAppContentBlocksPlaceholder`). This may cause an issue for adding it into RecyclerView where fast scrolling leads to multiple false-positive loads. That may have a negative impact on app performance. You are able to post-pone message loading for such cases.

Example shows how to load message in `onBindViewHolder` method, but you are able to call it after a 'shimmer' animation, etc.
```kotlin
override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    ...
    var placeholderView = Exponea.getInAppContentBlocksPlaceholder(
        PLACEHOLDER_ID_1,
        parent.context,
        InAppContentBlockPlaceholderConfiguration(
            defferedLoad = true
        )
    )
    return ViewHolder(placeholderView)
}
override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    // do nothing, content will load itself OR refresh content:
    (holder.itemView as InAppContentBlockPlaceholderView).refreshContent()
}
```

### In-app content block images caching
To reduce the number of API calls, SDK is caching the images displayed in messages. Therefore, once the SDK downloads the image, an image with the same URL may not be downloaded again, and will not change, since it was already cached. For this reason, we recommend always using different URLs for different images.

### In-app content blocks tracking

In-app content blocks are tracked automatically by SDK. You may see these `action` values in customers tracked events:

- 'show' - event is tracked if message has been shown to user
- 'action' - event is tracked if user clicked on action button inside message. Event contains 'text' and 'link' properties that you might be interested in
- 'close' - event is tracked if user clicked on close button inside message
- 'error' - event is tracked if showing of message has failed. Event contains 'error' property with meaningfull description

> The behaviour of In-app content block tracking may be affected by the tracking consent feature, which in enabled mode considers the requirement of explicit consent for tracking. Read more in [tracking consent documentation](./TRACKING_CONSENT.md).

### Show In-app content block view after content is loaded

You may prefer UX design "finding and operating" principle so you want to show to user only available things. Or you may have a static layout where you need to set exact frame dimension to In-app content block view but it is blank until content is ready. For that case we recommend to use callback that will be notified if content has been successfully loaded or no content was found.

```kotlin
val placeholderView = Exponea.getInAppContentBlocksPlaceholder("placeholder_1", activityContext)
placeholderView?.let {
    it.setOnContentReadyListener { contentLoaded ->
        if (contentLoaded) {
            // you have exact dimensions for loaded content
            Logger.i(this, "InApp CB has dimens width ${it.width}px height ${it.height}px")
        } else {
            // you can hide this view because no In-app content block is available now
            it.visibility = View.GONE
        }
    }
}
```

### Custom In-app content block actions

If you want to override default SDK behavior, when in-app content block action is performed (button is clicked), or you want to add your code to be performed along with code executed by the SDK, you can set up `behaviourCallback` on View instance.
Default SDK behaviour is mainly tracking of 'show', 'click', 'close' and 'error' events and opening action URL.

```kotlin
val placeholderView = Exponea.getInAppContentBlocksPlaceholder(
        "placeholder_1",
        activityContext,
        // it is recommended to postpone message load if `onMessageShown` usage is crucial for you
        // due to cached messages so message could be shown before you set `behaviourCallback`
        InAppContentBlockPlaceholderConfiguration(true)
)
// you can access original callback and invokes it anytime
val origBehaviour = placeholderView.behaviourCallback
placeholderView.behaviourCallback = object : InAppContentBlockCallback {
    override fun onMessageShown(placeholderId: String, contentBlock: InAppContentBlock) {
        origBehaviour.onMessageShown(placeholderId, contentBlock)   // tracks 'show'
        Logger.i(this, "Content block with HTML: ${contentBlock.htmlContent}")
        // you may set this placeholder visible
    }
    override fun onNoMessageFound(placeholderId: String) {
        origBehaviour.onNoMessageFound(placeholderId)   // just log
        // you may set this placeholder hidden
    }
    override fun onError(placeholderId: String, contentBlock: InAppContentBlock?, errorMessage: String) {
        if (contentBlock == null) {
            return
        }
        // !!! invoke origBehaviour.onError to track 'error' or call it yourself
        Exponea.trackInAppContentBlockError(
                placeholderId, contentBlock, errorMessage
        )
        // you may set this placeholder hidden and do any fallback
    }
    override fun onCloseClicked(placeholderId: String, contentBlock: InAppContentBlock) {
        // !!! invoke origBehaviour.onCloseClicked to track 'close' or call it yourself
        Exponea.trackInAppContentBlockClose(placeholderId, contentBlock)
        // placeholder may show another content block if is assigned to placeholder ID
    }
    override fun onActionClicked(placeholderId: String, contentBlock: InAppContentBlock, action: InAppContentBlockAction) {
        // content block action has to be tracked for 'click' event
        Exponea.trackInAppContentBlockAction(
                placeholderId, action, contentBlock
        )
        // content block action has to be handled for given `action.url`
        handleUrlByYourApp(action.url)
    }
}
```