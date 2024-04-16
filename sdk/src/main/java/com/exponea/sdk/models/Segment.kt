package com.exponea.sdk.models

public class Segment : HashMap<String, String> {

    constructor() : super()

    constructor(source: Map<String, String>) : super(source)

    internal fun deepClone(): Segment {
        return Segment(this)
    }
}
