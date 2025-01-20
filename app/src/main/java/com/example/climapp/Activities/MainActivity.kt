package com.example.climapp.Activities

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.example.climapp.Models.WeatherModel
import com.example.climapp.R
import com.example.climapp.Utilites.ApiUtilities
import com.example.climapp.databinding.ActivityMainBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Date
import kotlin.math.roundToInt
import com.newrelic.agent.android.NewRelic

class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding

    private lateinit var currentLocation: Location

    private lateinit var fusedLocationProvider: FusedLocationProviderClient

    private val LOCATION_REQUEST_CODE = 101

    private val apiKey = "1101ab007281cc591261574d6ae549d4"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        NewRelic.withApplicationToken(
            "AAd15e79e223b38710c2dbbd72a8a9f1c969b789c8-NRMA"
        ).start(this.applicationContext)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        fusedLocationProvider = LocationServices.getFusedLocationProviderClient(this)

        getCurrentLocation()
        // Detectar Botao de pesquisa
        binding.citySearch.setOnEditorActionListener { textView, i, keyEvent ->
            if (i == EditorInfo.IME_ACTION_SEARCH) {
                getCityWeather(binding.citySearch.text.toString())

                val view = this.currentFocus
                if (view != null) {
                    val imm: InputMethodManager =
                        getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(view.windowToken, 0)
                    binding.citySearch.clearFocus()
                }
                return@setOnEditorActionListener true
            } else {
                return@setOnEditorActionListener false
            }
        }

        binding.currentLocation.setOnClickListener {
            getCurrentLocation()
        }
    }
    // Pesquisa cidade
    private fun getCityWeather(city: String) {
        binding.progressBar.visibility = View.VISIBLE
        //Chama api
        ApiUtilities.getApiInterface()?.getCityWeatherData(city, apiKey)?.enqueue(
            object : Callback<WeatherModel> {
                @RequiresApi(Build.VERSION_CODES.O)
                override fun onResponse(
                    call: Call<WeatherModel>,
                    response: Response<WeatherModel>
                ) {
                    if (response.isSuccessful) {
                        binding.progressBar.visibility = View.GONE
                        response.body()?.let {
                            setData(it)     // Att UI
                        }
                    } else {
                        Toast.makeText(this@MainActivity, "Cidade não encontrada", Toast.LENGTH_SHORT).show()
                        binding.progressBar.visibility = View.GONE
                    }
                }

                override fun onFailure(call: Call<WeatherModel>, t: Throwable) {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this@MainActivity, "Falha ao buscar dados", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun fetchCurrentLocationWeather(latitude: String, longitude: String) {
        ApiUtilities.getApiInterface()?.getCurrentWeatherData(latitude, longitude, apiKey)?.enqueue(
            object : Callback<WeatherModel> {
                @RequiresApi(Build.VERSION_CODES.O)
                //Tratar resposta API
                override fun onResponse(
                    call: Call<WeatherModel>,
                    response: Response<WeatherModel>
                ) {
                    if (response.isSuccessful) {
                        binding.progressBar.visibility = View.GONE
                        response.body()?.let {
                            setData(it)
                        }
                    }
                }

                override fun onFailure(call: Call<WeatherModel>, t: Throwable) {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this@MainActivity, "Falha ao buscar dados", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
    // Permissão pra loc
    private fun getCurrentLocation() {
        if (checkPermissions()) {
            if (isLocationEnabled()) {
                // Verificar permissões explicitamente
                if (ActivityCompat.checkSelfPermission(
                        this,
                        android.Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(
                        this,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    // Solicitar permissões se não estiverem disponíveis
                    requestPermission()
                    return
                }

                fusedLocationProvider.lastLocation
                    .addOnSuccessListener { location ->
                        if (location != null) {
                            currentLocation = location
                            binding.progressBar.visibility = View.VISIBLE
                            fetchCurrentLocationWeather(
                                location.latitude.toString(),
                                location.longitude.toString()
                            )
                        } else {
                            Toast.makeText(this, "Não foi possível obter a localização", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Erro ao acessar localização: ${it.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(this, "Ative a localização no dispositivo", Toast.LENGTH_SHORT).show()
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            }
        } else {
            requestPermission()
        }
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                android.Manifest.permission.ACCESS_COARSE_LOCATION,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ),
            LOCATION_REQUEST_CODE
        )
    }
    // Verif GPS
    private fun isLocationEnabled(): Boolean {
        val locationManager: LocationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }
    // Verif se a loc foi permitido
    private fun checkPermissions(): Boolean {
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            return true
        }
        return false
    }
    // Permissão negada
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation()
            } else {
                Toast.makeText(this, "Permissão negada", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setData(body: WeatherModel) {
        binding.apply {
            val currentDate = SimpleDateFormat("dd/MM/yyyy HH:mm").format(Date())
            dateTime.text = currentDate

            maxTemp.text = "Max: ${k2c(body.main.temp_max)}°"
            minTemp.text = "Min: ${k2c(body.main.temp_min)}°"
            temp.text = "${k2c(body.main.temp)}°"
            weatherTitle.text = body.weather[0].main

            val timezoneOffset = body.timezone
            sunriseValue.text = ts2td(body.sys.sunrise.toLong(), timezoneOffset)
            sunsetValue.text = ts2td(body.sys.sunset.toLong(), timezoneOffset)

            pressureValue.text = body.main.pressure.toString()
            humidityValue.text = "${body.main.humidity}%"
            tempFValue.text = "${(k2c(body.main.temp) * 1.8 + 32).roundToInt()}°F"
            citySearch.setText(body.name)
            feelsLike.text = "Sensação Térmica: ${k2c(body.main.feels_like)}°"
            windValue.text = "${body.wind.speed} m/s"
            groundValue.text = body.main.grnd_level.toString()
            seaValue.text = body.main.sea_level.toString()
            countryValue.text = body.sys.country
        }

        updateUI(body.weather[0].id)
    }
    // Mudar background
    private fun updateUI(id: Int) {
        binding.apply {
            when (id) {
                in 200..232 -> {
                    weatherImg.setImageResource(R.drawable.ic_storm_weather)
                    mainLayout.background = ContextCompat.getDrawable(
                        this@MainActivity, R.drawable.thunderstrom_bg)
                    optionsLayout.background = ContextCompat.getDrawable(
                        this@MainActivity, R.drawable.thunderstrom_bg)
                }
                in 300..321 -> {
                    weatherImg.setImageResource(R.drawable.ic_few_clouds)
                    mainLayout.background = ContextCompat.getDrawable(
                        this@MainActivity, R.drawable.drizzle_bg)
                    optionsLayout.background = ContextCompat.getDrawable(
                        this@MainActivity, R.drawable.drizzle_bg)
                }
                in 500..531 -> {
                    weatherImg.setImageResource(R.drawable.ic_rainy_weather)
                    mainLayout.background = ContextCompat.getDrawable(
                        this@MainActivity, R.drawable.rain_bg)
                    optionsLayout.background = ContextCompat.getDrawable(
                        this@MainActivity, R.drawable.rain_bg)
                }
                in 600..622 -> {
                    weatherImg.setImageResource(R.drawable.ic_snow_weather)
                    mainLayout.background = ContextCompat.getDrawable(
                        this@MainActivity, R.drawable.snow_bg)
                    optionsLayout.background = ContextCompat.getDrawable(
                        this@MainActivity, R.drawable.snow_bg)
                }
                in 700..781 -> {
                    weatherImg.setImageResource(R.drawable.ic_broken_clouds)
                    mainLayout.background = ContextCompat.getDrawable(
                        this@MainActivity, R.drawable.atmosphere_bg)
                    optionsLayout.background = ContextCompat.getDrawable(
                        this@MainActivity, R.drawable.atmosphere_bg)
                }
                800 -> {
                    weatherImg.setImageResource(R.drawable.ic_clear_day)
                    mainLayout.background = ContextCompat.getDrawable(
                        this@MainActivity, R.drawable.clear_bg)
                    optionsLayout.background = ContextCompat.getDrawable(
                        this@MainActivity, R.drawable.clear_bg)
                }
                in 801..804 -> {
                    weatherImg.setImageResource(R.drawable.ic_cloudy_weather)
                    mainLayout.background = ContextCompat.getDrawable(
                        this@MainActivity, R.drawable.clouds_bg)
                    optionsLayout.background = ContextCompat.getDrawable(
                        this@MainActivity, R.drawable.clouds_bg)
                }
                else -> {
                    weatherImg.setImageResource(R.drawable.ic_unknown)
                    mainLayout.background = ContextCompat.getDrawable(
                        this@MainActivity, R.drawable.unknown_bg)
                    optionsLayout.background = ContextCompat.getDrawable(
                        this@MainActivity, R.drawable.unknown_bg)
                }
            }
        }
    }
    // Formatar horas e fuso horario
    @RequiresApi(Build.VERSION_CODES.O)
    private fun ts2td(ts: Long, timezoneOffset: Int): String {
        val zonedDateTime = Instant.ofEpochSecond(ts)
            .atZone(ZoneId.of("UTC"))
            .withZoneSameInstant(ZoneId.ofOffset("UTC", ZoneOffset.ofTotalSeconds(timezoneOffset)))
        return zonedDateTime.format(DateTimeFormatter.ofPattern("HH:mm"))
    }
    // K pra C
    private fun k2c(t: Double): Double {
        var intTemp = t
        intTemp -= 273.15
        return intTemp.toBigDecimal().setScale(1, RoundingMode.UP).toDouble()
    }
}