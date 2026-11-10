package com.example.networkapp

import android.graphics.Bitmap
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class ComicCacheTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val preferences = context.getSharedPreferences("comics", 0)
    private var originalComic: String? = null
    private val imageFile by lazy { File(context.cacheDir, "rotation-test.png") }

    @Before
    fun setUp() {
        originalComic = preferences.getString("last_comic", null)
        preferences.edit().remove("last_comic").commit()
    }

    @After
    fun tearDown() {
        preferences.edit().putString("last_comic", originalComic).commit()
        imageFile.delete()
    }

    @Test
    fun savedComicRestoresOnLaunchAndActivityRecreation() {
        val bitmap = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)
        imageFile.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        val comic = JSONObject()
            .put("num", 100)
            .put("title", "Cached comic")
            .put("alt", "Cached description")
            .put("img", imageFile.toURI().toString())
        preferences.edit().putString("last_comic", comic.toString()).commit()

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            assertComicDisplayed(scenario)
            // Rotation destroys and recreates the activity through this same lifecycle.
            scenario.recreate()
            assertComicDisplayed(scenario)
        }
    }

    @Test
    fun corruptCacheDoesNotCrashOnLaunch() {
        preferences.edit().putString("last_comic", "invalid json").commit()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity {
                assertFalse(preferences.contains("last_comic"))
            }
        }
    }

    private fun assertComicDisplayed(scenario: ActivityScenario<MainActivity>) {
        var imageLoaded = false
        repeat(50) {
            scenario.onActivity { activity ->
                assertEquals("Cached comic", activity.titleTextView.text.toString())
                assertEquals("Cached description", activity.descriptionTextView.text.toString())
                assertEquals("100", activity.numberEditText.text.toString())
                imageLoaded = activity.comicImageView.drawable != null
            }
            if (imageLoaded) return
            Thread.sleep(100)
        }
        fail("Cached comic image did not load")
    }
}
