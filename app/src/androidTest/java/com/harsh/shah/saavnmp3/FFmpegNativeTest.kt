package com.harsh.shah.saavnmp3

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Smoke test for the FFmpeg native libraries.
 *
 * The maintained FFmpegKit 8.1.9 AAR only ships 64-bit libraries. The 32-bit
 * libraries for older devices are checked in under src/main/jniLibs (see the
 * README there), so this test makes sure the mixed setup still loads and runs
 * on both 32-bit and 64-bit devices/emulators.
 */
@RunWith(AndroidJUnit4::class)
class FFmpegNativeTest {
    @Test
    fun ffmpegLoadsAndRuns() {
        val session = FFmpegKit.execute("-version")
        val output = session.output ?: ""
        assertTrue(
            "FFmpeg failed: ${session.failStackTrace}\n$output",
            ReturnCode.isSuccess(session.returnCode)
        )
        assertTrue("Unexpected FFmpeg output:\n$output", output.contains("ffmpeg version"))
    }
}
