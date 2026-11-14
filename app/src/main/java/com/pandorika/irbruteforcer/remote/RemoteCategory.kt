package com.pandorika.irbruteforcer.remote

import androidx.annotation.StringRes
import com.pandorika.irbruteforcer.R

/**
 * Device categories in dropdown order. AC and TV are pinned first ([pinned]); the dropdown draws
 * a divider after the last pinned entry. Libraries are Momentum universal `.ir` files in assets.
 */
enum class RemoteCategory(
    @StringRes val titleRes: Int,
    @StringRes val shortTitleRes: Int,
    val icon: ButtonIcon,
    val assetPath: String,
    val pinned: Boolean,
    val buttons: List<RemoteButton>,
) {
    Ac(
        titleRes = R.string.category_ac,
        shortTitleRes = R.string.category_ac_short,
        icon = ButtonIcon.Ac,
        assetPath = "infrared/ac.ir",
        pinned = true,
        buttons = listOf(
            RemoteButton("off", R.string.label_off, ButtonIcon.Power, listOf("Off")),
            RemoteButton("cool_hi", R.string.label_cool_max, ButtonIcon.Snow, listOf("Cool_hi")),
            RemoteButton("heat_hi", R.string.label_heat_max, ButtonIcon.Heat, listOf("Heat_hi")),
            RemoteButton("cool_lo", R.string.label_cool_23, ButtonIcon.Snow, listOf("Cool_lo")),
            RemoteButton("heat_lo", R.string.label_heat_23, ButtonIcon.Heat, listOf("Heat_lo")),
            RemoteButton("dry", R.string.label_dry, ButtonIcon.Drop, listOf("Dh")),
        ),
    ),
    Tv(
        titleRes = R.string.category_tv,
        shortTitleRes = R.string.category_tv_short,
        icon = ButtonIcon.Tv,
        assetPath = "infrared/tv.ir",
        pinned = true,
        buttons = listOf(
            RemoteButton("power", R.string.label_power, ButtonIcon.Power, listOf("Power")),
            RemoteButton("vol_up", null, ButtonIcon.Plus, listOf("Vol_up"), nameRes = R.string.name_vol_up),
            RemoteButton("vol_dn", null, ButtonIcon.Minus, listOf("Vol_dn"), nameRes = R.string.name_vol_dn),
            RemoteButton("mute", R.string.label_mute, ButtonIcon.Mute, listOf("Mute")),
            RemoteButton("ch_next", null, ButtonIcon.Up, listOf("Ch_next"), nameRes = R.string.name_ch_next),
            RemoteButton("ch_prev", null, ButtonIcon.Down, listOf("Ch_prev"), nameRes = R.string.name_ch_prev),
        ),
    ),
    Projector(
        titleRes = R.string.category_projector,
        shortTitleRes = R.string.category_projector_short,
        icon = ButtonIcon.Projector,
        assetPath = "infrared/projectors.ir",
        pinned = false,
        buttons = listOf(
            RemoteButton("power", R.string.label_power, ButtonIcon.Power, listOf("Power")),
            RemoteButton("vol_up", null, ButtonIcon.Plus, listOf("Vol_up"), nameRes = R.string.name_vol_up),
            RemoteButton("vol_dn", null, ButtonIcon.Minus, listOf("Vol_dn"), nameRes = R.string.name_vol_dn),
            RemoteButton("mute", R.string.label_mute, ButtonIcon.Mute, listOf("Mute")),
            RemoteButton("play", R.string.label_play, ButtonIcon.Play, listOf("Play")),
            RemoteButton("pause", R.string.label_pause, ButtonIcon.Pause, listOf("Pause")),
        ),
    ),
    Audio(
        titleRes = R.string.category_audio,
        shortTitleRes = R.string.category_audio_short,
        icon = ButtonIcon.Speaker,
        assetPath = "infrared/audio.ir",
        pinned = false,
        buttons = listOf(
            RemoteButton("power", R.string.label_power, ButtonIcon.Power, listOf("Power")),
            RemoteButton("mute", R.string.label_mute, ButtonIcon.Mute, listOf("Mute")),
            RemoteButton("vol_up", null, ButtonIcon.Plus, listOf("Vol_up"), nameRes = R.string.name_vol_up),
            RemoteButton("vol_dn", null, ButtonIcon.Minus, listOf("Vol_dn"), nameRes = R.string.name_vol_dn),
            RemoteButton("play", R.string.label_play, ButtonIcon.Play, listOf("Play")),
            RemoteButton("pause", R.string.label_pause, ButtonIcon.Pause, listOf("Pause")),
            RemoteButton("prev", null, ButtonIcon.Prev, listOf("Prev"), nameRes = R.string.name_prev),
            RemoteButton("next", null, ButtonIcon.Next, listOf("Next"), nameRes = R.string.name_next),
        ),
    ),
    Fan(
        titleRes = R.string.category_fan,
        shortTitleRes = R.string.category_fan_short,
        icon = ButtonIcon.Fan,
        assetPath = "infrared/fans.ir",
        pinned = false,
        buttons = listOf(
            RemoteButton("power", R.string.label_power, ButtonIcon.Power, listOf("Power")),
            RemoteButton("speed_up", null, ButtonIcon.Plus, listOf("Speed_up"), nameRes = R.string.name_speed_up),
            RemoteButton("speed_dn", null, ButtonIcon.Minus, listOf("Speed_dn"), nameRes = R.string.name_speed_dn),
            RemoteButton("rotate", R.string.label_rotate, ButtonIcon.Rotate, listOf("Rotate")),
            RemoteButton("mode", R.string.label_mode, ButtonIcon.Mode, listOf("Mode")),
            RemoteButton("timer", R.string.label_timer, ButtonIcon.Timer, listOf("Timer")),
        ),
    ),
    Led(
        titleRes = R.string.category_led,
        shortTitleRes = R.string.category_led_short,
        icon = ButtonIcon.Bulb,
        assetPath = "infrared/leds.ir",
        pinned = false,
        buttons = listOf(
            RemoteButton("power_on", R.string.label_on, ButtonIcon.Bulb, listOf("Power_on")),
            RemoteButton("power_off", R.string.label_off, ButtonIcon.Power, listOf("Power_off")),
            RemoteButton("bright_up", null, ButtonIcon.Plus, listOf("Brightness_up"), nameRes = R.string.name_bright_up),
            RemoteButton("bright_dn", null, ButtonIcon.Minus, listOf("Brightness_dn"), nameRes = R.string.name_bright_dn),
            RemoteButton("red", null, ButtonIcon.Bulb, listOf("Red"), nameRes = R.string.name_red),
            RemoteButton("green", null, ButtonIcon.Bulb, listOf("Green"), nameRes = R.string.name_green),
            RemoteButton("blue", null, ButtonIcon.Bulb, listOf("Blue"), nameRes = R.string.name_blue),
            RemoteButton("white", null, ButtonIcon.Bulb, listOf("White"), nameRes = R.string.name_white),
        ),
    ),
    Disc(
        titleRes = R.string.category_disc,
        shortTitleRes = R.string.category_disc_short,
        icon = ButtonIcon.Disc,
        assetPath = "infrared/bluray_dvd.ir",
        pinned = false,
        buttons = listOf(
            RemoteButton("power", R.string.label_power, ButtonIcon.Power, listOf("Power")),
            RemoteButton("eject", R.string.label_eject, ButtonIcon.Eject, listOf("Eject")),
            RemoteButton("rewind", null, ButtonIcon.Rewind, listOf("Fast_ba"), nameRes = R.string.name_rewind),
            RemoteButton("play", null, ButtonIcon.Play, listOf("Play"), nameRes = R.string.label_play),
            RemoteButton("pause", null, ButtonIcon.Pause, listOf("Pause"), nameRes = R.string.label_pause),
            RemoteButton("forward", null, ButtonIcon.FastForward, listOf("Fast_fo"), nameRes = R.string.name_forward),
            RemoteButton("ok", R.string.label_ok, ButtonIcon.Ok, listOf("Ok")),
            RemoteButton("subtitle", R.string.label_subtitles, ButtonIcon.Subtitle, listOf("Subtitle")),
        ),
    ),
    Monitor(
        titleRes = R.string.category_monitor,
        shortTitleRes = R.string.category_monitor_short,
        icon = ButtonIcon.Monitor,
        assetPath = "infrared/monitor.ir",
        pinned = false,
        buttons = listOf(
            RemoteButton("power", R.string.label_power, ButtonIcon.Power, listOf("POWER")),
            RemoteButton("source", R.string.label_source, ButtonIcon.Source, listOf("SOURCE")),
            RemoteButton("menu", R.string.label_menu, ButtonIcon.Menu, listOf("MENU")),
            RemoteButton("exit", R.string.label_exit, ButtonIcon.Exit, listOf("EXIT")),
        ),
    ),
    ;

    fun button(id: String): RemoteButton? = buttons.firstOrNull { it.id == id }
}
