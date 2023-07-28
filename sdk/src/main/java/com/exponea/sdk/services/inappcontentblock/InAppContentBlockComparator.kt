package com.exponea.sdk.services.inappcontentblock

import com.exponea.sdk.models.InAppContentBlock

internal open class InAppContentBlockComparator : Comparator<InAppContentBlock> {

    object INSTANCE : InAppContentBlockComparator()

    private val KEEP_ORDER = 0
    private val FIRST_WINS = -1
    private val SECOND_WINS = 1
    private val CMP_RESULTS = listOf(FIRST_WINS, KEEP_ORDER, SECOND_WINS)
    override fun compare(msg1: InAppContentBlock?, msg2: InAppContentBlock?): Int {
        // null check
        when {
            msg1 == null && msg2 == null -> return KEEP_ORDER
            msg1 == null -> SECOND_WINS
            msg2 == null -> FIRST_WINS
        }
        // priority comparison - higher priority wins
        val msgPriority1 = msg1?.priority ?: 0
        val msgPriority2 = msg2?.priority ?: 0
        when {
            msgPriority1 > msgPriority2 -> FIRST_WINS
            msgPriority2 > msgPriority1 -> SECOND_WINS
        }
        // for same priorities, we want to randomize it
        return CMP_RESULTS.random()
    }
}
