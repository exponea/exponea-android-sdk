package com.exponea.sdk.models

internal class SegmentationCategories : HashMap<String, ArrayList<Segment>> {

    constructor() : super()

    constructor(source: Map<String, ArrayList<Segment>>) : super(source)

    fun deepClone(): SegmentationCategories {
        return SegmentationCategories(this.mapValues { segmentationsEntry ->
            ArrayList(segmentationsEntry.value.map { segment -> segment.deepClone() })
        })
    }
}
