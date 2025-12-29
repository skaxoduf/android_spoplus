package com.skaxoduf.spoplus

import android.content.Context
import java.io.File
import java.util.Properties

data class DbConfig(
    val server: String,
    val database: String,
    val user: String,
    val pass: String
)

class ConfigManager(private val context: Context) {

    private val fileName = "config.ini"

    fun saveConfig(server: String, database: String, user: String, pass: String) {
        val file = File(context.filesDir, fileName)
        val content = """
            [MSSQL]
            server = $server
            database = $database
            user = $user
            password = $pass
        """.trimIndent()
        file.writeText(content)
    }

    fun loadConfig(): DbConfig {
        val file = File(context.filesDir, fileName)
        if (!file.exists()) {
            // Return defaults or empty
            return DbConfig("10.0.2.2:1433", "SpoPlus", "sa", "5895")
        }

        val content = file.readText()
        val props = parseIni(content)
        
        return DbConfig(
            server = props.getProperty("server", "10.0.2.2:1433"),
            database = props.getProperty("database", "SpoPlus"),
            user = props.getProperty("user", "sa"),
            pass = props.getProperty("password", "5895")
        )
    }

    private fun parseIni(content: String): Properties {
        val props = Properties()
        val lines = content.lines()
        for (line in lines) {
            if (line.trim().startsWith("[") || line.trim().isEmpty()) continue
            val parts = line.split("=", limit = 2)
            if (parts.size == 2) {
                props.setProperty(parts[0].trim(), parts[1].trim())
            }
        }
        return props
    }
}
