import android.content.Context
import com.exponea.sdk.Exponea
import com.google.firebase.messaging.FirebaseMessaging

class TokenTracker {
    fun trackToken(context: Context?) {
        FirebaseMessaging.getInstance().token.addOnSuccessListener {
            Exponea.trackPushToken(it)
        }
    }
}
