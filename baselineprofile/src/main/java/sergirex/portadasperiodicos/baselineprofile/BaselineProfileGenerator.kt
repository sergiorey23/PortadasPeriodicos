package sergirex.portadasperiodicos.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Records the code paths of a typical session — cold start, scrolling the covers grid, swiping tabs and
 * opening a cover — so they ship precompiled instead of starting out interpreted.
 *
 * Run with the emulator up: `./gradlew :app:generateBaselineProfile`. The result is written to
 * app/src/release/generated/baselineProfiles and should be committed.
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() = rule.collect(packageName = PACKAGE, includeInStartupProfile = true) {
        // The daily-reminder permission dialog would otherwise cover the first launch.
        device.executeShellCommand("pm grant $PACKAGE android.permission.POST_NOTIFICATIONS")
        pressHome()
        startActivityAndWait()

        val grid = device.wait(Until.findObject(By.res(PACKAGE, "recyclerView")), TIMEOUT_MS) ?: return@collect
        device.wait(Until.hasObject(By.res(PACKAGE, "portadaImageView")), TIMEOUT_MS)
        grid.setGestureMargin(device.displayWidth / 5)
        grid.fling(Direction.DOWN)
        device.waitForIdle()
        grid.fling(Direction.UP)
        device.waitForIdle()

        device.findObject(By.res(PACKAGE, "viewpager"))?.let { pager ->
            pager.setGestureMargin(device.displayWidth / 5)
            pager.swipe(Direction.LEFT, 1f)
            device.waitForIdle()
            pager.swipe(Direction.RIGHT, 1f)
            device.waitForIdle()
        }

        // Open a cover (container transform + detail screen) and come back.
        device.wait(Until.findObject(By.res(PACKAGE, "portadaImageView")), TIMEOUT_MS)?.click() ?: return@collect
        device.wait(Until.hasObject(By.res(PACKAGE, "imagen_extendida")), TIMEOUT_MS)
        device.waitForIdle()
        device.pressBack()
        device.waitForIdle()
    }

    private companion object {
        const val PACKAGE = "sergirex.portadasperiodicos"
        const val TIMEOUT_MS = 15_000L
    }
}
