package com.pandorika.irbruteforcer.remote

import androidx.annotation.StringRes

/** Icon identifiers; the UI layer maps each to a 9x9 pixel bitmap. */
enum class ButtonIcon {
    Power, Snow, Heat, Drop, Fan, Plus, Minus, Up, Down, Mute, Play, Pause, Next, Prev,
    FastForward, Rewind, Bulb, Eject, Timer, Rotate, Mode, Ok, Subtitle, Source, Menu, Exit,
    Ac, Tv, Projector, Speaker, Monitor, Disc,
}

/**
 * One remote button. [signalNames] are the `name:` values in the category's universal library
 * that this button brute-forces, in priority order (exact, case-sensitive match).
 * [labelRes] null = icon-only button.
 */
data class RemoteButton(
    val id: String,
    @StringRes val labelRes: Int?,
    val icon: ButtonIcon,
    val signalNames: List<String>,
    /** Name shown on the LCD while scanning an icon-only button ([labelRes] == null). */
    @StringRes val nameRes: Int? = null,
)
