package com.exponea.sdk.services.inappcontentblock

import com.exponea.sdk.models.InAppContentBlock

internal class InAppContentBlockCarouselComparator : Comparator<InAppContentBlock> {

    companion object {
        val INSTANCE = InAppContentBlockCarouselComparator()
    }

    private val KEEP_ORDER = 0
    private val FIRST_WINS = -1
    private val SECOND_WINS = 1
    override fun compare(msg1: InAppContentBlock?, msg2: InAppContentBlock?): Int {
        // null check
        when {
            msg1 == null && msg2 == null -> return KEEP_ORDER
            msg1 == null -> return SECOND_WINS
            msg2 == null -> return FIRST_WINS
        }
        // primary comparison - higher priority wins
        val msgPriority1 = msg1?.priority ?: 0
        val msgPriority2 = msg2?.priority ?: 0
        when {
            msgPriority1 > msgPriority2 -> return FIRST_WINS
            msgPriority2 > msgPriority1 -> return SECOND_WINS
        }
        // secondary comparison - sort by name ascending
        return String.CASE_INSENSITIVE_ORDER.compare(msg1?.name ?: "", msg2?.name ?: "")
    }
}
