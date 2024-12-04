package com.exponea.sdk.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout.LayoutParams
import androidx.fragment.app.Fragment
import com.exponea.sdk.Exponea
import com.exponea.sdk.databinding.MessageInboxDetailFragmentBinding

class AppInboxDetailFragment : Fragment() {

    companion object {
        public val MESSAGE_ID = "MessageID"
        fun buildInstance(messageId: String): AppInboxDetailFragment {
            return AppInboxDetailFragment().apply {
                arguments = Bundle().apply {
                    putString(MESSAGE_ID, messageId)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val layout = MessageInboxDetailFragmentBinding.inflate(inflater, container, false)
        val messageId = arguments?.getString(MESSAGE_ID)
        if (messageId == null) {
            activity?.finish()
            return null
        }
        layout.container.addView(
            Exponea.getAppInboxDetailView(requireContext(), messageId),
            LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        )
        return layout.root
    }
}
