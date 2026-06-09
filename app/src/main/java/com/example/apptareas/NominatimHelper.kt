package com.example.apptareas

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object NominatimHelper {

    suspend fun buscarCoordenadas(direccion: String): Pair<Double, Double>? {
        return withContext(Dispatchers.IO) {
            try {
                // 1. Limpiar el texto para la URL (convierte espacios en %20, etc.)
                val direccionLimpia = URLEncoder.encode(direccion, "UTF-8")

                // 2. Armar la URL (Agregamos limit=1 para que sea más rápida la descarga)
                val urlString = "https://nominatim.openstreetmap.org/search?q=$direccionLimpia&format=json&limit=1"
                val url = URL(urlString)
                val conexion = url.openConnection() as HttpURLConnection

                // ⚠️ REGLA DE ORO DE NOMINATIM ⚠️
                // Tienes que poner un nombre real que identifique a tu app para que no te bloqueen
                conexion.setRequestProperty("User-Agent", "AppTareas (ruizvalentin2002@gmail.com)")
                conexion.requestMethod = "GET"

                // 3. Verificar si la respuesta fue exitosa (Código 200 OK)
                if (conexion.responseCode == HttpURLConnection.HTTP_OK) {
                    val respuestaJson = conexion.inputStream.bufferedReader().use { it.readText() }
                    val jsonArray = JSONArray(respuestaJson)

                    // Si encontró resultados
                    if (jsonArray.length() > 0) {
                        val primerResultado = jsonArray.getJSONObject(0)

                        // Extraemos latitud y longitud
                        val lat = primerResultado.getString("lat").toDouble()
                        val lon = primerResultado.getString("lon").toDouble()

                        return@withContext Pair(lat, lon)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace() // Si se corta el internet o falla algo, esto nos avisa
            }

            // Si llegamos hasta aquí, algo falló o la dirección no existe
            return@withContext null
        }
    }
    // Creamos un molde para guardar los datos de la lista
    data class Lugar(val nombre: String, val latitud: Double, val longitud: Double)

    //Función que devuelve una lista de opciones
    suspend fun buscarOpciones(query: String): List<Lugar> {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val listaResultados = mutableListOf<Lugar>()
            try {
                // Le pedimos a OpenStreetMap hasta 5 resultados (limit=5)
                val url = java.net.URL("https://nominatim.openstreetmap.org/search?q=${java.net.URLEncoder.encode(query, "UTF-8")}&format=json&limit=5")
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.setRequestProperty("User-Agent", "AppTareas/1.0")

                if (connection.responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().readText()
                    val jsonArray = org.json.JSONArray(response)

                    // Recorremos los resultados y los guardamos en nuestra lista
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val nombre = obj.getString("display_name")
                        val lat = obj.getString("lat").toDouble()
                        val lon = obj.getString("lon").toDouble()
                        listaResultados.add(Lugar(nombre, lat, lon))
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            listaResultados // Devolvemos la lista llena (o vacía si hubo error)
        }
    }
}