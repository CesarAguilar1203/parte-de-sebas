package com.example.cps01

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.ViewGroup
import android.webkit.WebView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.URL
import android.content.Context
import java.io.File
import org.json.JSONArray
import org.json.JSONObject
import com.example.cps01.ui.theme.CpS01Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CpS01Theme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    LoginScreen()
                }
            }
        }
    }
}

@Composable
fun CameraScreen() {
    var url by remember { mutableStateOf("") }
    var showStream by remember { mutableStateOf(false) }
    var snapshot by remember { mutableStateOf<Bitmap?>(null) }
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            label = { Text("IP o URL del ESP32-CAM") },
            modifier = Modifier.fillMaxWidth()
        )

        Row(modifier = Modifier.padding(top = 8.dp)) {
            Button(onClick = { showStream = true }) { Text("Mostrar") }
            Spacer(Modifier.width(8.dp))
            Button(onClick = {
                if (url.isNotBlank()) {
                    scope.launch {
                        snapshot = loadBitmap(url.trimEnd('/') + "/capture")
                    }
                }
            }) { Text("Capturar") }
        }

        if (showStream && url.isNotBlank()) {
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        settings.javaScriptEnabled = true
                        loadUrl(url)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(top = 16.dp)
            )
        }

        snapshot?.let { bmp ->
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            )
        }
    }
}

private suspend fun loadBitmap(url: String): Bitmap? = withContext(Dispatchers.IO) {
    return@withContext try {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.inputStream.use { BitmapFactory.decodeStream(it) }
    } catch (_: Exception) {
        null
    }
}

@Preview(showBackground = true)
@Composable
fun CameraScreenPreview() {
    CpS01Theme {
        CameraScreen()
    }
}

@Composable
fun LoginScreen() {
    val context = LocalContext.current
    var user by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        OutlinedTextField(
            value = user,
            onValueChange = { user = it },
            label = { Text("Usuario") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = pass,
            onValueChange = { pass = it },
            label = { Text("Contrase\u00f1a") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation()
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                message = if (loginUser(context, user, pass))
                    "Bienvenido" else "Credenciales incorrectas"
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Iniciar sesi\u00f3n") }
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                message = if (registerUser(context, user, pass))
                    "Registrado" else "El usuario ya existe"
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Registrarse") }
        if (message.isNotEmpty()) {
            Text(message, modifier = Modifier.padding(top = 16.dp))
        }
    }
}

private fun registerUser(context: Context, user: String, pass: String): Boolean {
    if (user.isBlank() || pass.isBlank()) return false
    val file = File(context.filesDir, "users.json")
    val json = if (file.exists()) JSONArray(file.readText()) else JSONArray()
    for (i in 0 until json.length()) {
        val obj = json.getJSONObject(i)
        if (obj.getString("user") == user) return false
    }
    val obj = JSONObject()
    obj.put("user", user)
    obj.put("pass", pass)
    json.put(obj)
    file.writeText(json.toString())
    return true
}

private fun loginUser(context: Context, user: String, pass: String): Boolean {
    val file = File(context.filesDir, "users.json")
    if (!file.exists()) return false
    val json = JSONArray(file.readText())
    for (i in 0 until json.length()) {
        val obj = json.getJSONObject(i)
        if (obj.getString("user") == user && obj.getString("pass") == pass)
            return true
    }
    return false
}
