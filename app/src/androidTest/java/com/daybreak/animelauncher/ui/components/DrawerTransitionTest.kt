package com.daybreak.animelauncher.ui.components

import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.core.Transition
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.daybreak.animelauncher.AppShortcut
import com.daybreak.animelauncher.DrawerCategory
import com.daybreak.animelauncher.ui.screens.DrawerAppContextMenu
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Compile only here; actual frame-clock, input and native-window checks need a device. */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class DrawerTransitionTest {
    private val durationScale = object : MotionDurationScale { override var scaleFactor = 1f }
    @get:Rule val rule = createComposeRule(effectContext = durationScale)
    private var visible by mutableStateOf(false)
    private var ownerPresent by mutableStateOf(true)
    private var motion: Transition<Boolean>? = null
    private var dispatcher: OnBackPressedDispatcher? = null
    private var instance: Any? = null
    private var compositions = 0
    private var measures = 0
    private var placements = 0
    private var mounts = 0
    private var disposals = 0
    private var homeBacks = 0
    private var homeClicks = 0
    private var drawerClicks = 0

    private fun mount() {
        rule.mainClock.autoAdvance = false
        rule.setContent {
            val back = LocalOnBackPressedDispatcherOwner.current!!.onBackPressedDispatcher
            SideEffect { dispatcher = back }
            BackHandler { homeBacks++ }
            Box(Modifier.size(240.dp, 400.dp)) {
                if (!visible) {
                    Box(Modifier.fillMaxSize().semantics { testTag = "home" }.clickable { homeClicks++ })
                }
                if (ownerPresent) DrawerTransition(visible) { transition, layer ->
                    val token = remember { Any() }
                    var menuOpen by remember { mutableStateOf(false) }
                    SideEffect { motion = transition; instance = token; compositions++ }
                    DisposableEffect(Unit) {
                        mounts++
                        onDispose { disposals++ }
                    }
                    BackHandler { if (menuOpen) menuOpen = false else visible = false }
                    Box(
                        layer.fillMaxSize().semantics {
                            testTag = if (transition.currentState && transition.targetState && !transition.isRunning)
                                "nova_drawer_ready" else "nova_drawer_busy"
                        }.layout { measurable, constraints ->
                            measures++
                            val child = measurable.measure(constraints)
                            layout(child.width, child.height) { child.place(0, 0); placements++ }
                        }.clickable { drawerClicks++ }
                    ) {
                        Text("Menu", Modifier.clickable { menuOpen = true })
                    }
                    if (menuOpen) DrawerAppContextMenu(
                        app = AppShortcut("fixture", "Fixture app", "com.example.fixture"),
                        categories = listOf(DrawerCategory("all", "All")),
                        isEs = true,
                        onDismissRequest = { menuOpen = false },
                        onChangeIcon = { menuOpen = false },
                        onMoveToCategory = { menuOpen = false },
                        onSystemAction = { _, _ -> menuOpen = false }
                    )
                }
            }
        }
        rule.mainClock.advanceTimeByFrame()
    }

    private fun drawer() = rule.onNode(hasTestTag("nova_drawer_ready") or hasTestTag("nova_drawer_busy"))
    private fun advance(milliseconds: Long) { rule.mainClock.advanceTimeBy(milliseconds); rule.waitForIdle() }
    private fun open(milliseconds: Long = 240) { rule.runOnIdle { visible = true }; advance(milliseconds) }
    private fun progress() = rule.runOnIdle { motion!!.animations.single().value as Float }
    private fun back() = rule.runOnIdle { dispatcher!!.onBackPressed() }

    @Test fun openCloseDisposesDrawerAndRestoresHomeWithoutInvisibleHits() {
        mount()
        drawer().assertDoesNotExist()
        rule.onNodeWithTag("home").performTouchInput { click() }
        open()
        rule.onNodeWithTag("nova_drawer_ready").assertExists()
        assertEquals(1f, progress(), 0.0001f)
        rule.onNodeWithTag("home").assertDoesNotExist()
        rule.runOnIdle { visible = false }
        advance(240)
        drawer().assertDoesNotExist()
        rule.onNodeWithTag("home").performTouchInput { click() }
        rule.runOnIdle { assertEquals(2, homeClicks); assertEquals(0, drawerClicks); assertEquals(1, mounts); assertEquals(1, disposals) }
    }

    @Test fun backDuringEntryRetainsContentUntilExitCompletes() {
        mount()
        open(64)
        assertTrue(progress() > 0f && progress() < 1f)
        back()
        rule.mainClock.advanceTimeByFrame()
        drawer().assertExists()
        rule.runOnIdle { assertFalse(motion!!.targetState); assertEquals(0, disposals); assertEquals(0, homeBacks) }
        advance(300)
        drawer().assertDoesNotExist()
        rule.runOnIdle { assertEquals(1, disposals) }
    }

    @Test fun reopeningDuringExitKeepsContentAndConsecutiveOpensDoNotRestartIt() {
        mount()
        open()
        val original = rule.runOnIdle { instance }
        rule.runOnIdle { visible = false }
        advance(48)
        assertTrue(progress() > 0f && progress() < 1f)
        rule.runOnIdle { visible = true }
        rule.mainClock.advanceTimeByFrame()
        rule.runOnIdle { assertSame(original, instance); assertEquals(0, disposals) }
        advance(300)
        rule.runOnIdle { visible = true }
        advance(32)
        rule.onNodeWithTag("nova_drawer_ready").assertExists()
        rule.runOnIdle { assertEquals(1, mounts); assertSame(original, instance) }
    }

    @Test fun repeatedBackDuringExitDoesNotReachHomeUntilDrawerIsRemoved() {
        mount()
        open()
        back()
        advance(48)
        back()
        rule.runOnIdle { assertEquals(0, homeBacks) }
        advance(240)
        drawer().assertDoesNotExist()
        back()
        rule.runOnIdle { assertEquals(1, homeBacks) }
    }

    @Test fun inputDuringEntryIsImmediateAndMenuStillOwnsItsBackPriority() {
        mount()
        open(48)
        drawer().performTouchInput { click() }
        rule.runOnIdle { assertEquals(1, drawerClicks) }
        rule.onNodeWithText("Menu").performClick()
        rule.onAllNodes(isDialog()).assertCountEquals(1)
        back()
        rule.onAllNodes(isDialog()).assertCountEquals(0)
        rule.runOnIdle { assertTrue(visible); assertEquals(0, homeBacks) }
        back()
        advance(300)
        drawer().assertDoesNotExist()
    }

    @Test fun zeroMotionScaleSettlesWithoutDurationWaitAndReleasesInput() {
        durationScale.scaleFactor = 0f
        mount()
        rule.runOnIdle { visible = true }
        repeat(3) { rule.mainClock.advanceTimeByFrame() }
        rule.onNodeWithTag("nova_drawer_ready").assertExists()
        back()
        repeat(3) { rule.mainClock.advanceTimeByFrame() }
        drawer().assertDoesNotExist()
        rule.onNodeWithTag("home").performTouchInput { click() }
        rule.runOnIdle { assertEquals(1, homeClicks); assertEquals(1, disposals) }
    }

    @Test fun ownerDisposalCancelsMotionAndRestorationDoesNotReopenDrawer() {
        mount()
        open(48)
        val oldInstance = rule.runOnIdle { instance }
        rule.runOnIdle { ownerPresent = false; visible = false }
        rule.mainClock.advanceTimeByFrame()
        drawer().assertDoesNotExist()
        advance(300)
        rule.runOnIdle { ownerPresent = true }
        rule.mainClock.advanceTimeByFrame()
        drawer().assertDoesNotExist()
        rule.runOnIdle { assertEquals(1, disposals); assertEquals(1, mounts) }
        open()
        rule.runOnIdle { assertNotSame(oldInstance, instance); assertEquals(2, mounts) }
    }

    @Test fun animatedFramesDoNotRecomposeRemeasureOrReplaceDrawerContent() {
        mount()
        open(48)
        val counts = rule.runOnIdle { Triple(compositions, measures, placements) }
        advance(48)
        assertTrue(progress() in 0f..1f)
        rule.runOnIdle { assertEquals(counts.first, compositions); assertEquals(counts.second, measures); assertEquals(counts.third, placements); assertEquals(1, mounts) }
    }
}
