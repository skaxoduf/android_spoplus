package com.skaxoduf.spoplus

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import android.app.DatePickerDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var etServer: EditText
    private lateinit var etDatabase: EditText
    private lateinit var etUser: EditText
    private lateinit var etPass: EditText
    private lateinit var btnDateFrom: Button
    private lateinit var btnDateTo: Button
    private lateinit var btnConnect: Button
    private lateinit var tvStatus: TextView
    private lateinit var recyclerView: RecyclerView

    private lateinit var configManager: ConfigManager

    // Format: YYYYMMDD (Common in DBs) or YYYY-MM-DD. Let's start with YYYYMMDD based on typical legacy systems
    // If it fails, user can tell us format.
    private val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
    private val displayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    
    private var fromDateStr = ""
    private var toDateStr = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        configManager = ConfigManager(this)

        initViews()
        initDates()
        loadConfigToViews()

        btnDateFrom.setOnClickListener { showDatePicker(true) }
        btnDateTo.setOnClickListener { showDatePicker(false) }

        btnConnect.setOnClickListener {
            saveConfigFromViews()
            performSearch()
        }
    }

    private fun initViews() {
        etServer = findViewById(R.id.etServer)
        etDatabase = findViewById(R.id.etDatabase)
        etUser = findViewById(R.id.etUser)
        etPass = findViewById(R.id.etPassword)
        btnDateFrom = findViewById(R.id.btnDateFrom)
        btnDateTo = findViewById(R.id.btnDateTo)
        btnConnect = findViewById(R.id.btnConnect)
        tvStatus = findViewById(R.id.tvStatus)
        recyclerView = findViewById(R.id.recyclerView)
        
        recyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun initDates() {
        val calendar = Calendar.getInstance()
        updateDateButtons(calendar, true)
        updateDateButtons(calendar, false)
    }
    
    private fun showDatePicker(isFrom: Boolean) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(this, { _, y, m, d ->
            val newDate = Calendar.getInstance()
            newDate.set(y, m, d)
            updateDateButtons(newDate, isFrom)
        }, year, month, day).show()
    }
    
    private fun updateDateButtons(cal: Calendar, isFrom: Boolean) {
        val strForDb = dateFormat.format(cal.time)
        val strForDisplay = displayFormat.format(cal.time)
        
        if (isFrom) {
            fromDateStr = strForDb
            btnDateFrom.text = strForDisplay
        } else {
            toDateStr = strForDb
            btnDateTo.text = strForDisplay
        }
    }

    private fun loadConfigToViews() {
        val config = configManager.loadConfig()
        etServer.setText(config.server)
        etDatabase.setText(config.database)
        etUser.setText(config.user)
        etPass.setText(config.pass)
    }

    private fun saveConfigFromViews() {
        configManager.saveConfig(
            etServer.text.toString(),
            etDatabase.text.toString(),
            etUser.text.toString(),
            etPass.text.toString()
        )
    }

    private fun performSearch() {
        tvStatus.text = "Status: Searching..."
        btnConnect.isEnabled = false
        
        val config = configManager.loadConfig()
        
        CoroutineScope(Dispatchers.IO).launch {
            val dbHelper = DatabaseHelper()
            val results = dbHelper.connectAndQuery(config, fromDateStr, toDateStr)
            
            withContext(Dispatchers.Main) {
                btnConnect.isEnabled = true
                if (results.isNotEmpty() && results[0].id == "Error") {
                    tvStatus.text = "Status: Search Failed"
                    Toast.makeText(this@MainActivity, results[0].details, Toast.LENGTH_LONG).show()
                } else {
                    tvStatus.text = "Status: Found ${results.size} rows."
                    recyclerView.adapter = TransactionAdapter(results)
                }
            }
        }
    }
}
