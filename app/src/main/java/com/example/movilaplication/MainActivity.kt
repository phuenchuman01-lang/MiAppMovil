package com.example.movilaplication

import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() {

    // IP PÚBLICA DE LA INSTANCIA AWS EC2 "movil"
    // siempre se debe cambiar acorde a la IP publica actual del servidor.
    private val PH_baseUrl = "http://34.224.8.169:5000"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Referencias a los elementos de la interfaz (GET)
        val PH_txtNombre = findViewById<TextView>(R.id.PH_txtNombre)
        val PH_txtCorreo = findViewById<TextView>(R.id.PH_txtCorreo)
        val PH_txtRut = findViewById<TextView>(R.id.PH_txtRut)
        val PH_btnObtener = findViewById<Button>(R.id.PH_btnObtenerUsuario)

        // Referencias a los elementos de la interfaz (POST)
        val PH_inputLogin = findViewById<EditText>(R.id.PH_inputLogin)
        val PH_inputPassword = findViewById<EditText>(R.id.PH_inputPassword)
        val PH_btnLogin = findViewById<Button>(R.id.PH_btnLoginPost)
        val PH_txtResultadoPost = findViewById<TextView>(R.id.PH_txtResultadoPost)

        // 1. Evento del botón GET: Obtener detalles del usuario
        PH_btnObtener.setOnClickListener {
            PH_realizarPeticionGet(PH_txtNombre, PH_txtCorreo, PH_txtRut)
        }

        // 2. Evento del botón POST: Enviar JSON (login, password)
        PH_btnLogin.setOnClickListener {
            val PH_loginStr = PH_inputLogin.text.toString().trim()
            val PH_passStr = PH_inputPassword.text.toString().trim()

            if (PH_loginStr.isEmpty() || PH_passStr.isEmpty()) {
                Toast.makeText(this, "Completa el login y password", Toast.LENGTH_SHORT).show()
            } else {
                PH_realizarPeticionPost(PH_loginStr, PH_passStr, PH_txtResultadoPost)
            }
        }
    }

    // Función para realizar la solicitud HTTP GET al servicio Flask
    private fun PH_realizarPeticionGet(
        txtNombre: TextView,
        txtCorreo: TextView,
        txtRut: TextView
    ) {
        Thread {
            try {
                val url = URL("$PH_baseUrl/usuario")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                val inputStreamReader = InputStreamReader(connection.inputStream)
                val bufferedReader = BufferedReader(inputStreamReader)
                val stringBuilder = StringBuilder()
                var line: String?
                while (bufferedReader.readLine().also { line = it } != null) {
                    stringBuilder.append(line)
                }
                bufferedReader.close()

                // Procesar el JSON recibido
                val jsonResult = JSONObject(stringBuilder.toString())
                val PH_nombre = jsonResult.getString("nombre")
                val PH_correo = jsonResult.getString("correo")
                val PH_rut = jsonResult.getString("rut")

                // Actualizar los TextViews en el hilo principal
                runOnUiThread {
                    txtNombre.text = "Nombre: $PH_nombre"
                    txtCorreo.text = "Correo: $PH_correo"
                    txtRut.text = "RUT: $PH_rut"
                    Toast.makeText(this, "Datos obtenidos con éxito", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this, "Error de conexión GET", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    // Función para realizar la solicitud HTTP POST enviando un JSON
    private fun PH_realizarPeticionPost(
        login: String,
        pass: String,
        txtResultado: TextView
    ) {
        Thread {
            try {
                // Crear el objeto JSON con login y password
                val json = JSONObject()
                json.put("login", login)
                json.put("password", pass)

                val url = URL("$PH_baseUrl/login")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connection.doOutput = true
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                // Escribir el JSON en el cuerpo de la petición
                val outputStreamWriter = OutputStreamWriter(connection.outputStream)
                outputStreamWriter.write(json.toString())
                outputStreamWriter.flush()
                outputStreamWriter.close()

                // Leer la respuesta aunque sea código 200 o 401
                val stream = if (connection.responseCode < HttpURLConnection.HTTP_BAD_REQUEST) {
                    connection.inputStream
                } else {
                    connection.errorStream
                }

                val bufferedReader = BufferedReader(InputStreamReader(stream))
                val stringBuilder = StringBuilder()
                var line: String?
                while (bufferedReader.readLine().also { line = it } != null) {
                    stringBuilder.append(line)
                }
                bufferedReader.close()

                val jsonResult = JSONObject(stringBuilder.toString())
                val PH_valido = jsonResult.getBoolean("valido")
                val PH_mensaje = jsonResult.getString("mensaje")

                runOnUiThread {
                    txtResultado.text = "Estado: $PH_mensaje"
                    if (PH_valido) {
                        txtResultado.setTextColor(Color.parseColor("#2E7D32")) // Verde
                    } else {
                        txtResultado.setTextColor(Color.parseColor("#C62828")) // Rojo
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    txtResultado.text = "Error de conexión con el servidor Flask"
                    txtResultado.setTextColor(Color.parseColor("#C62828"))
                }
            }
        }.start()
    }
}