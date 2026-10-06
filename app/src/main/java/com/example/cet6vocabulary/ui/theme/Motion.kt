package com.example.cet6vocabulary.ui.theme

import android.content.Context
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.KeyframesSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * Single source of truth for motion. Every animated surface reads its timing, easing, spring
 * and displacement values from here so the whole app moves the same way.
 *
 * Values are deliberately conservative: this is a study tool, so animation signals state and
 * confirms input instead of decorating the screen. Nothing here loops forever.
 */
object MaoDanMotion {
    // Timing tiers.
    const val FastMillis = 160
    const val NormalMillis = 260
    const val SlowMillis = 400
    const val CardEnterDelayMillis = 80
    const val EnterStaggerMillis = 60
    const val ProgressMillis = 300

    // Spotlight: a soft light that follows the pointer across a card. Deliberately faint, so it
    // reads as a surface catching the light instead of an object glowing at the user.
    const val SpotlightPeakAlpha = 0.14f
    const val SpotlightRadiusRatio = 0.75f

    // Shiny text: one soft band of light crossing the glyphs of a label, once, as a page arrives.
    // Width is a fraction of the label so a short caption and a long title sweep at the same
    // proportion, and the peak alpha stays low enough that the text never stops being text.
    const val ShineWidthRatio = 0.22f
    const val ShinePeakAlpha = 0.24f
    const val ShineDarkPeakAlpha = 0.18f
    const val ShineDelayMillis = 180

    /** Tangent of the band's lean away from vertical: ~8 degrees, so it is not a hard scanline. */
    const val ShineTilt = 0.14f

    // Tilt card: a subtle 3D tilt that responds to pointer position. Deliberately minimal,
    // just enough to add spatial depth without feeling like the card is flipping.
    const val TiltMaxRotation = 3f

    /**
     * Perspective for the tilt, in Compose camera units. Measured on a 420dpi device this leaves
     * roughly 880px between the camera and the card, so a full 3 degree tip changes the near and
     * far edge heights by about 1.5 percent each: visible depth, no distortion of the text.
     */
    const val TiltCameraDistance = 12f

    // Aurora: a persistent, continuously flowing ambient light sitting behind the whole app.
    // Four large washes drift in an infinite loop. They are meant to be seen: the light gives the
    // background real colour and depth, while content stays on opaque surfaces above it.
    const val AuroraLoopMillis = 18000

    /** Peak alpha of the strongest blob. Dark mode sits lower: a glow on near-black reads hotter. */
    const val AuroraLightPeakAlpha = 0.28f
    const val AuroraDarkPeakAlpha = 0.20f

    /** Blob radius as a fraction of the canvas' short edge. Large enough that neighbours overlap
     *  into one continuous wash instead of reading as separate circles. */
    const val AuroraBlobRadiusRatio = 0.85f

    /** How far each blob drifts, as a fraction of the canvas' short edge. */
    const val AuroraTravelRatio = 0.09f

    /** Period of the alpha breathing effect (half-cycle), in millis. One full breath = 2x this. */
    const val AuroraBreathMillis = 7000

    /** Minimum alpha multiplier during the breath cycle. 0.80 = dims to 80% of its peak alpha. */
    const val AuroraBreathFactor = 0.80f

    // Particles: a thin layer of dust drifting just above the aurora. Small, faint and slow, so
    // the background gains a little grain and depth without competing with the light underneath
    // it or with the content above it. One canvas, one clock, plain circles: no gradients, no
    // glow, because the aurora already carries the large-scale colour.
    const val ParticleCount = 28

    /** Dust radius bounds. Under 2dp so a mote reads as grain in the light, never as a shape. */
    val ParticleMinRadius: Dp = 0.7.dp
    val ParticleMaxRadius: Dp = 1.8.dp

    /** Peak alpha of a mote. Dark sits higher: the same alpha loses contrast on a near-black
     *  background, and the dust has to survive the aurora glowing underneath it. */
    const val ParticleLightAlpha = 0.10f
    const val ParticleDarkAlpha = 0.14f

    /** Radius of a mote's drift orbit, as a fraction of the canvas' short edge. */
    const val ParticleTravelRatio = 0.025f

    /** One full orbit of the drift. Shorter than the aurora loop so the two layers never beat. */
    const val ParticleLoopMillis = 12000

    /** Minimum alpha multiplier during the breath. 0.75 = dims to 75%, never down to nothing. */
    const val ParticleBreathFactor = 0.75f

    // Liquid glass bottom bar: one translucent surface floating over the aurora, plus a single
    // glass droplet that slides between the tabs. The surface is static and the droplet only moves
    // while the selection moves, so an idle bar schedules no frames of its own. Every alpha is a
    // fraction of a theme colour rather than a fixed RGB, so light and dark mode stay in step.
    const val LiquidGlassSelectionMillis = 320

    /** Glass body of the bar, top to bottom. Low enough that the aurora still reads through it. */
    const val LiquidGlassSurfaceTopAlpha = 0.55f
    const val LiquidGlassLightSurfaceBottomAlpha = 0.80f
    const val LiquidGlassDarkSurfaceBottomAlpha = 0.72f

    /** Hairline of light along the top edge and how far down it fades. Dark stays fainter. */
    const val LiquidGlassLightHighlightAlpha = 0.30f
    const val LiquidGlassDarkHighlightAlpha = 0.12f
    const val LiquidGlassHighlightHeightRatio = 0.30f

    /** Edge stroke at its strongest point; it fades out before it can read as an outline. */
    const val LiquidGlassLightEdgeAlpha = 0.18f
    const val LiquidGlassDarkEdgeAlpha = 0.12f
    const val LiquidGlassEdgeFadeStop = 0.55f

    /** The droplet: a tinted glass bead, a little denser than the bar but never opaque. */
    const val LiquidGlassLightCapsuleAlpha = 0.88f
    const val LiquidGlassDarkCapsuleAlpha = 0.78f
    const val LiquidGlassCapsuleBottomFactor = 0.86f
    const val LiquidGlassLightCapsuleSheenAlpha = 0.38f
    const val LiquidGlassDarkCapsuleSheenAlpha = 0.16f
    const val LiquidGlassCapsuleSheenStop = 0.5f
    const val LiquidGlassLightCapsuleEdgeAlpha = 0.50f
    const val LiquidGlassDarkCapsuleEdgeAlpha = 0.22f

    val LiquidGlassCapsuleCorner: Dp = 20.dp
    val LiquidGlassCapsuleInset: Dp = 3.dp

    /** Icon scale under its droplet. Deliberately below NavigationSelectedIconScale: the droplet
     *  already carries the selection, and a larger icon crowds a five tab bar. */
    const val LiquidGlassSelectedIconScale = 1.04f

    /** Liquid stretch. The droplet lengthens while it travels, narrows back as it settles, and the
     *  height factor keeps its area close to constant so it reads as fluid instead of scaling. */
    const val LiquidGlassMaxStretch = 0.045f
    const val LiquidGlassStretchVelocityDivisor = 55f
    const val LiquidGlassStretchHeightFactor = 0.35f

    val StandardEasing = FastOutSlowInEasing

    fun <T> fast(): TweenSpec<T> = tween(durationMillis = FastMillis, easing = StandardEasing)
    fun <T> normal(): TweenSpec<T> = tween(durationMillis = NormalMillis, easing = StandardEasing)
    fun <T> slow(): TweenSpec<T> = tween(durationMillis = SlowMillis, easing = StandardEasing)

    /** A whole page needs a beat more than a component; a single card settles inside the page. */
    fun <T> pageEnter(): TweenSpec<T> = slow()
    fun <T> cardEnter(): TweenSpec<T> = normal()

    /** A filling bar should read as growth, so it runs a beat longer than a state change. */
    fun <T> progress(): TweenSpec<T> = tween(durationMillis = ProgressMillis, easing = StandardEasing)

    /** A light crossing a label needs the long tier; the short one reads as a flicker. */
    fun <T> shine(): TweenSpec<T> = slow()

    /** The droplet's slide. Softer than [stateSpring]: a wide bead overshooting reads as wobble,
     *  so this keeps a hint of give and settles inside [LiquidGlassSelectionMillis]. */
    fun <T> liquidGlassSelection(): SpringSpec<T> = spring(dampingRatio = 0.82f, stiffness = 380f)

    /** Icon and label colour settle over the same window as the droplet's slide. */
    fun <T> liquidGlassColor(): TweenSpec<T> =
        tween(durationMillis = LiquidGlassSelectionMillis, easing = StandardEasing)



    /** Selected/unselected state changes: quick and slightly damped, no bounce. */
    fun <T> stateSpring(): SpringSpec<T> = spring(dampingRatio = 0.8f, stiffness = 420f)

    // Press feedback: press down fast, then release through a small overshoot back to rest.
    const val PressDownMillis = 140
    const val PressReleaseMillis = 240
    private const val PressOvershootAtMillis = 110

    fun pressDown(): TweenSpec<Float> = tween(durationMillis = PressDownMillis, easing = StandardEasing)

    fun pressRelease(): KeyframesSpec<Float> = keyframes {
        durationMillis = PressReleaseMillis
        PressOvershootScale at PressOvershootAtMillis using StandardEasing
    }

    // Scale and alpha.
    const val ButtonPressedScale = 0.96f
    const val CardPressedScale = 0.98f
    const val NavigationPressedScale = 0.94f
    const val PressOvershootScale = 1.03f
    const val SuccessPulseScale = 1.03f
    const val NavigationSelectedIconScale = 1.08f
    const val NavigationUnselectedIconScale = 0.94f
    const val NavigationUnselectedIconAlpha = 0.75f
    const val NavigationUnselectedLabelAlpha = 0.85f

    // Displacement. All of it is applied through graphicsLayer, so none of it affects layout.
    val PageEnterOffset: Dp = 12.dp
    val CardEnterOffset: Dp = 10.dp
    val WordSwitchOffset: Dp = 16.dp
    val NavigationSelectedLabelRise: Dp = 2.dp
    val ShakeAmplitude: Dp = 4.dp
    val SpotlightMinRadius: Dp = 90.dp
    val SpotlightMaxRadius: Dp = 140.dp

    // Magnetic button: a subtle pull towards the pointer, like the button is attracted to the touch.
    // Small enough to feel like a micro-interaction, not like the button is flying around.
    val MagneticMaxTranslation: Dp = 3.dp

    /** One step of the wrong-answer shake; fractions of [ShakeAmplitude] live with the component. */
    fun shakeStep(durationMillis: Int): TweenSpec<Float> =
        tween(durationMillis = durationMillis, easing = LinearEasing)
}

/**
 * True when the system asks for less motion (animator/transition scale turned off, or touch
 * exploration running). Animated surfaces degrade to a cut or a plain fade, keeping colour and
 * alpha state feedback so nothing becomes ambiguous.
 */
val LocalReduceMotion = staticCompositionLocalOf { false }

@Composable
fun rememberSystemReduceMotion(): Boolean {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var reduceMotion by remember(context) { mutableStateOf(systemRequestsReduceMotion(context)) }
    DisposableEffect(context, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            // System settings can change while the app is backgrounded; re-read on resume
            // instead of registering a permanent listener.
            if (event == Lifecycle.Event.ON_RESUME) reduceMotion = systemRequestsReduceMotion(context)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    return reduceMotion
}

private fun systemRequestsReduceMotion(context: Context): Boolean {
    val resolver = context.contentResolver
    val animatorScale = Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
    val transitionScale = Settings.Global.getFloat(resolver, Settings.Global.TRANSITION_ANIMATION_SCALE, 1f)
    if (animatorScale == 0f || transitionScale == 0f) return true
    val accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
    return accessibilityManager?.isTouchExplorationEnabled == true
}
