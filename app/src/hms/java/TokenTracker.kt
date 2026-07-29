import android.content.Context
import android.text.TextUtils
import android.util.Log
import com.exponea.sdk.Exponea
import com.huawei.hms.aaid.HmsInstanceId
import com.huawei.hms.common.ApiException

class TokenTracker {

    private val tag = this::class.simpleName

    fun trackToken(context: Context?) {
        object : Thread() {
            override fun run() {
                try {
                    // Obtain the app ID from the agconnect-service.json file.
                    val appId = "115368361"

                    // Set tokenScope to HCM.
                    val tokenScope = "HCM"
                    val token = HmsInstanceId.getInstance(context).getToken(appId, tokenScope)

                    // Check whether the token is empty.
                    if (!TextUtils.isEmpty(token)) {
                        Exponea.trackHmsPushToken(token)
                    }
                } catch (e: ApiException) {
                    Log.e(tag, "get hms token failed, $e")
                }
            }
        }.start()
    }
}
