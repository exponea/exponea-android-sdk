package com.exponea.sdk.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout.LayoutParams
import androidx.fragment.app.Fragment
import com.exponea.sdk.Exponea
import com.exponea.sdk.databinding.MessageInboxListFragmentBinding
import com.exponea.sdk.models.MessageItem
import com.exponea.sdk.util.Logger

class AppInboxListFragment : Fragment() {

    private var onItemClickListener: (MessageItem, Int) -> Unit = { item: MessageItem, position: Int ->
        Logger.i(this, "AppInbox message ${item.id} is opening")
        onMessageItemClicked(item, position)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val layout = MessageInboxListFragmentBinding.inflate(inflater, container, false)
        layout.container.addView(
            Exponea.getAppInboxListView(requireContext(), onItemClickListener),
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        )
        return layout.root
    }

    private fun onMessageItemClicked(item: MessageItem, index: Int) {
        requireContext().startActivity(AppInboxDetailActivity.buildIntent(requireContext(), item))
    }
}
