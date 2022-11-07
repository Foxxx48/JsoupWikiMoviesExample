package com.fox.jsoupwikimoviesexample

import android.R
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.ListAdapter
import androidx.lifecycle.lifecycleScope
import com.fox.jsoupwikimoviesexample.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

class MainActivity : AppCompatActivity() {
    private var _binding: ActivityMainBinding? = null
    private val binding
        get() = _binding ?: throw RuntimeException("ActivityMainBinding = null")

    var movies = mutableListOf<Movie?>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val adapter = ArrayAdapter(this, R.layout.simple_list_item_1, movies)

        lifecycleScope.launch(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()
            val doc =
                Jsoup.connect("$WIKI/wiki/List_of_films_with_a_100%25_rating_on_Rotten_Tomatoes")
                    .get()
            doc.select(".wikitable:first-of-type tr td:first-of-type a")
                .map { col -> col.attr("href") }

                .parallelStream()
                .map { extractMovieData(it) }
                .filter { it != null }
                .forEach { movies.add(it) }
            println("${(System.currentTimeMillis() - startTime) / 1000} seconds")
        }


        binding.btnShow.setOnClickListener {
            lifecycleScope.launch(Dispatchers.Main) {

                binding.listViewMovies.adapter = adapter

            }

        }
    }

    fun extractMovieData(url: String): Movie? {
        val doc: Document
        try {
            doc = Jsoup.connect("$WIKI$url").get()
        } catch (e: Exception) {
            return null

        }
        val movie = Movie()
        doc.select(".infobox tr")
            .forEach { ele ->
                when {
                    ele.getElementsByTag("th")?.hasClass("summary") ?: false -> {
                        movie.title = ele.getElementsByTag("th")?.text()
                    }
                    /*ele.getElementsByTag("img").isNotEmpty() -> {
                        movie.posterURL = "https:" + ele.getElementsByTag("img").attr("src")
                    }*/
                    else -> {
                        val value: String? = if (ele.getElementsByTag("li").size > 1)
                            ele.getElementsByTag("li").map(Element::text).filter(String::isNotEmpty)
                                .joinToString(", ") else
                            ele.getElementsByTag("td")?.first()?.text()

                        when (ele.getElementsByTag("th")?.first()?.text()) {
                            "Directed by" -> movie.directedBy = value ?: ""
                            "Produced by" -> movie.producedBy = value ?: ""
                            "Written by" -> movie.writtenBy = value ?: ""
                            "Starring" -> movie.starring = value ?: ""
                            "Music by" -> movie.musicBy = value ?: ""
                            "Release date" -> movie.releaseDate = value ?: ""
                            //"poster URL" -> movie.posterURL = value ?: ""
                            "title" -> movie.title = value ?: ""
                        }
                    }
                }
            }
        return movie
    }


    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    private fun myLog(message: Any?) {
        Log.d("MyApp", "$message")
    }

    companion object {
        private const val WIKI = "https://en.wikipedia.org"
    }
}