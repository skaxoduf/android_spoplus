package com.skaxoduf.spoplus

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import androidx.core.view.GravityCompat
import android.widget.ImageButton
import android.content.Intent

import android.widget.RadioGroup
import android.widget.RadioButton
import androidx.appcompat.app.AppCompatDelegate
import android.content.Context
import android.content.SharedPreferences

class SettingsActivity : AppCompatActivity() {

    private lateinit var etServer: EditText
    private lateinit var etDatabase: EditText
    private lateinit var etUser: EditText
    private lateinit var etPass: EditText
    private lateinit var btnSave: Button
    
    private lateinit var rgTheme: RadioGroup
    private lateinit var rbLight: RadioButton
    private lateinit var rbDark: RadioButton
    private lateinit var rbSystem: RadioButton
    
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private lateinit var btnMenu: ImageButton
    
    private lateinit var configManager: ConfigManager
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        configManager = ConfigManager(this)
        prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

        initViews()
        setupNavigation()
        loadConfig()
        loadTheme()

        btnSave.setOnClickListener {
            saveConfig()
        }
        
        rgTheme.setOnCheckedChangeListener { _, checkedId ->
            val mode = when (checkedId) {
                R.id.rbLight -> AppCompatDelegate.MODE_NIGHT_NO
                R.id.rbDark -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
            AppCompatDelegate.setDefaultNightMode(mode)
            prefs.edit().putInt("theme_mode", mode).apply()
        }
    }

    private fun initViews() {
        etServer = findViewById(R.id.etServer)
        etDatabase = findViewById(R.id.etDatabase)
        etUser = findViewById(R.id.etUser)
        etPass = findViewById(R.id.etPassword)
        btnSave = findViewById(R.id.btnSave)
        
        rgTheme = findViewById(R.id.rgTheme)
        rbLight = findViewById(R.id.rbLight)
        rbDark = findViewById(R.id.rbDark)
        rbSystem = findViewById(R.id.rbSystem)
        
        drawerLayout = findViewById(R.id.drawerLayout)
        navView = findViewById(R.id.navView)
        btnMenu = findViewById(R.id.btnMenu)
    }

    private fun loadTheme() {
        val currentMode = prefs.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        when (currentMode) {
            AppCompatDelegate.MODE_NIGHT_NO -> rbLight.isChecked = true
            AppCompatDelegate.MODE_NIGHT_YES -> rbDark.isChecked = true
            else -> rbSystem.isChecked = true
        }
    }

    private fun setupNavigation() {
        btnMenu.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    finish() // Return to Home
                }
                R.id.nav_settings -> {
                     // Already here
                     drawerLayout.closeDrawer(GravityCompat.START)
                }
                R.id.nav_transactions -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish() // Close this to return to main flow
                }
                R.id.nav_exit -> {
                    finishAffinity()
                }
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    private fun loadConfig() {
        val config = configManager.loadConfig()
        etServer.setText(config.server)
        etDatabase.setText(config.database)
        etUser.setText(config.user)
        etPass.setText(config.pass)
    }

    private fun saveConfig() {
        configManager.saveConfig(
            etServer.text.toString(),
            etDatabase.text.toString(),
            etUser.text.toString(),
            etPass.text.toString()
        )
        Toast.makeText(this, getString(R.string.msg_config_saved), Toast.LENGTH_SHORT).show()
        finish()
    }
}
