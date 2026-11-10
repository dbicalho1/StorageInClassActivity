package com.example.networkapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.squareup.picasso.Picasso
import org.json.JSONObject
import org.json.JSONException

class MainActivity : AppCompatActivity() {

    private lateinit var requestQueue: RequestQueue
    private val comicPreferences by lazy { getSharedPreferences("comics", MODE_PRIVATE) }
    lateinit var titleTextView: TextView
    lateinit var descriptionTextView: TextView
    lateinit var numberEditText: EditText
    lateinit var showButton: Button
    lateinit var comicImageView: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        requestQueue = Volley.newRequestQueue(this)

        titleTextView = findViewById<TextView>(R.id.comicTitleTextView)
        descriptionTextView = findViewById<TextView>(R.id.comicDescriptionTextView)
        numberEditText = findViewById<EditText>(R.id.comicNumberEditText)
        showButton = findViewById<Button>(R.id.showComicButton)
        comicImageView = findViewById<ImageView>(R.id.comicImageView)

        showButton.setOnClickListener {
            downloadComic(numberEditText.text.toString())
        }

        loadSavedComic()
    }

    // Fetches comic from web as JSONObject
    private fun downloadComic (comicId: String) {
        val number = comicId.trim().toIntOrNull()
        if (number == null || number <= 0) {
            numberEditText.error = "Enter a positive comic number"
            return
        }
        val url = "https://xkcd.com/$number/info.0.json"
        // Only the most recently requested comic should update the screen and cache.
        requestQueue.cancelAll("comic")
        requestQueue.add (
            JsonObjectRequest(url
                , {
                    showComic(it)
                    saveComic(it)
                }
                , { error ->
                    Log.e("MainActivity", "Failed to download comic $comicId", error)
                    Toast.makeText(
                        this,
                        "Couldn't load comic. Check your connection and comic number.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            ).setTag("comic")
        )
    }

    // Display a comic for a given comic JSON object
    private fun showComic (comicObject: JSONObject) {
        titleTextView.text = comicObject.getString("title")
        descriptionTextView.text = comicObject.getString("alt")
        Picasso.get().load(comicObject.getString("img")).into(comicImageView)
    }

    // Persist the JSON; Picasso manages the comic image's memory and disk cache.
    private fun saveComic(comicObject: JSONObject) {
        comicPreferences.edit().putString("last_comic", comicObject.toString()).apply()
    }

    private fun loadSavedComic() {
        val savedComic = comicPreferences.getString("last_comic", null) ?: return
        try {
            val comicObject = JSONObject(savedComic)
            // Validate the cached fields before updating the screen.
            comicObject.getString("title")
            comicObject.getString("alt")
            comicObject.getString("img")
            val number = comicObject.getInt("num")
            showComic(comicObject)
            numberEditText.setText(number.toString())
        } catch (error: JSONException) {
            Log.w("MainActivity", "Ignoring invalid saved comic", error)
            comicPreferences.edit().remove("last_comic").apply()
        }
    }

    override fun onDestroy() {
        requestQueue.cancelAll("comic")
        requestQueue.stop()
        Picasso.get().cancelRequest(comicImageView)
        super.onDestroy()
    }

}
