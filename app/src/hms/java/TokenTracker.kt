import android.content.Context
import android.text.TextUtils
import com.exponea.sdk.Exponea
import com.exponea.sdk.util.Logger
import com.huawei.hms.aaid.HmsInstanceId
import com.huawei.hms.common.ApiException

class TokenTracker {
    fun trackToken(context: Context?) {
        object : Thread() {
            override fun run() {
                try {
                    // Obtain the app ID from the agconnect-service.json file.
                    val appId = "104661225"

                    // Set tokenScope to HCM.
                    val tokenScope = "HCM"
                    val token = HmsInstanceId.getInstance(context).getToken(appId, tokenScope)

                    // Check whether the token is empty.
                    if (!TextUtils.isEmpty(token)) {
                        Exponea.trackPushToken(token)
                    }
                } catch (e: ApiException) {
                    Logger.e(this, "get hms token failed, $e")
                }
            }
        }.start()
    }
}
