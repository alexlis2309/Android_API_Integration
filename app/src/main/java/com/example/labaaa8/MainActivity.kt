package com.example.labaaa8

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.URL
import okhttp3.*


class MainActivity : AppCompatActivity() {

    private lateinit var amountEditText: EditText
    private lateinit var fromCurrencySpinner: Spinner
    private lateinit var toCurrencySpinner: Spinner
    private lateinit var convertButton: Button
    private lateinit var resultTextView: TextView
    private lateinit var weatherTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        amountEditText = findViewById(R.id.amountEditText)
        fromCurrencySpinner = findViewById(R.id.fromCurrencySpinner)
        toCurrencySpinner = findViewById(R.id.toCurrencySpinner)
        convertButton = findViewById(R.id.convertButton)
        resultTextView = findViewById(R.id.resultTextView)
        weatherTextView = findViewById(R.id.weatherTextView)

        // Установка значений валют в спиннеры
        val currencies = arrayOf("USD", "EUR", "GBP", "JPY") // Замените на нужные валюты
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, currencies)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        fromCurrencySpinner.adapter = adapter
        toCurrencySpinner.adapter = adapter
        fetchWeather()
        convertButton.setOnClickListener {
            val amount = amountEditText.text.toString().toDoubleOrNull()
            if (amount != null) {
                val fromCurrency = fromCurrencySpinner.selectedItem as String
                val toCurrency = toCurrencySpinner.selectedItem as String
                fetchExchangeRate(fromCurrency, toCurrency, amount)
            } else {
                resultTextView.text = "Введите корректное значение суммы"
            }
        }

        fetchWeather()
    }

    private fun fetchWeather() {
        GlobalScope.launch {
            try {
                val weatherData = fetchWeatherFromAPI()
                withContext(Dispatchers.Main) {
                    val weatherText = "Погода в Минске:\n$weatherData"
                    weatherTextView.text = weatherText
                }
            } catch (e: IOException) {
                withContext(Dispatchers.Main) {
                    val errorText = "Ошибка при выполнении запроса: ${e.message}"
                    weatherTextView.text = errorText
                }
            }
        }
    }

    private suspend fun fetchWeatherFromAPI(): String = withContext(Dispatchers.IO) {
        val apiKey = "0c6be58f0bc3ccbd363698a48ad1c710" // Замените на ваш API ключ OpenWeatherMap
        val url = "https://api.openweathermap.org/data/2.5/weather?q=Minsk&appid=$apiKey&units=metric"
        val client = OkHttpClient()
        val request = Request.Builder()
            .url(url)
            .build()

        val response = client.newCall(request).execute()
        val responseString = response.body?.string()
        val jsonObject = JSONObject(responseString)

        if (jsonObject.has("main")) {
            val mainData = jsonObject.getJSONObject("main")
            val temperature = mainData.getDouble("temp")

            val weatherArray = jsonObject.getJSONArray("weather")
            val weatherObject = weatherArray.getJSONObject(0)
            val weatherDescription = weatherObject.getString("description")

            val weatherData = "Температура: $temperature °C\nОписание: $weatherDescription"
            weatherData
        } else {
            throw IOException("Invalid response format")
        }
    }



    private fun fetchExchangeRate(fromCurrency: String, toCurrency: String, amount: Double) {
        GlobalScope.launch {
            try {
                val exchangeRate = fetchExchangeRateFromAPI(fromCurrency, toCurrency)
                withContext(Dispatchers.Main) {
                    val convertedAmount = amount * exchangeRate
                    val resultText = "$amount $fromCurrency = $convertedAmount $toCurrency"
                    resultTextView.text = resultText
                }
            } catch (e: IOException) {
                withContext(Dispatchers.Main) {
                    val errorText = "Ошибка при выполнении запроса: ${e.message}"
                    resultTextView.text = errorText
                }
            }
        }
    }

    private suspend fun fetchExchangeRateFromAPI(fromCurrency: String, toCurrency: String): Double = withContext(Dispatchers.IO) {
        val apiKey = "Ваш apiKey"
        val url = "https://api.apilayer.com/exchangerates_data/convert?from=$fromCurrency&to=$toCurrency&amount=1"
        val connection = URL(url).openConnection()
        connection.setRequestProperty("apikey", apiKey)
        connection.connect()

        val response = connection.getInputStream().bufferedReader().use { it.readText() }
        val jsonObject = JSONObject(response)
        val exchangeRate = jsonObject.getDouble("result")
        exchangeRate
    }

    private fun fetchCurrencyRates() {
        GlobalScope.launch {
            try {
                val currencyRates = fetchCurrencyRatesFromAPI()
                withContext(Dispatchers.Main) {
                    val ratesText = "Курсы валют:\n$currencyRates"
                    resultTextView.text = ratesText
                }
            } catch (e: IOException) {
                withContext(Dispatchers.Main) {
                    val errorText = "Ошибка при выполнении запроса: ${e.message}"
                    resultTextView.text = errorText
                }
            }
        }
    }

    private suspend fun fetchCurrencyRatesFromAPI(): String = withContext(Dispatchers.IO) {
        val apiKey = "Ваш apiKey"
        val url = "https://api.apilayer.com/marketplace/exchangerates_data-api/live"
        val connection = URL(url).openConnection()
        connection.setRequestProperty("apikey", apiKey)
        connection.connect()

        val response = connection.getInputStream().bufferedReader().use { it.readText() }
        val jsonObject = JSONObject(response)
        val ratesObject = jsonObject.getJSONObject("quotes")
        val currencyRates = ratesObject.toString()
        currencyRates
    }
}
