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

import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import androidx.core.view.GravityCompat
import android.widget.ImageButton
import android.content.Intent

class MainActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private lateinit var btnMenu: ImageButton
    
    private lateinit var btnDateFrom: Button
    private lateinit var btnDateTo: Button
    private lateinit var btnConnect: Button
    private lateinit var tvStatus: TextView
    private lateinit var recyclerView: RecyclerView

    private lateinit var configManager: ConfigManager
    
    private lateinit var transactionAdapter: TransactionAdapter
    private val transactionList = mutableListOf<TransactionItem>()

    // Pagination
    private var currentPage = 1
    private var isLoading = false
    private var isLastPage = false
    private val pageSize = 10

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

        setupNavigation()

        btnDateFrom.setOnClickListener { showDatePicker(true) }
        btnDateTo.setOnClickListener { showDatePicker(false) }

        btnConnect.setOnClickListener {
            startNewSearch()
        }
    }

    private fun initViews() {
        drawerLayout = findViewById(R.id.drawerLayout)
        navView = findViewById(R.id.navView)
        btnMenu = findViewById(R.id.btnMenu)
        
        btnDateFrom = findViewById(R.id.btnDateFrom)
        btnDateTo = findViewById(R.id.btnDateTo)
        btnConnect = findViewById(R.id.btnConnect)
        tvStatus = findViewById(R.id.tvStatus)
        recyclerView = findViewById(R.id.recyclerView)
        
        val layoutManager = LinearLayoutManager(this)
        recyclerView.layoutManager = layoutManager
        
        transactionAdapter = TransactionAdapter(transactionList)
        recyclerView.adapter = transactionAdapter
        
        // 리스트 스크롤 감지 리스너 등록
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                
                // 현재 화면에 몇 개가 보이는지, 전체는 몇 개인지 확인
                val visibleItemCount = layoutManager.childCount
                val totalItemCount = layoutManager.itemCount
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                // [중요 조건] 로딩 중이 아니고(isLoading), 마지막 페이지가 아닐 때(!isLastPage)
                if (!isLoading && !isLastPage) {
                    // 현재 보이는 위치가 전체 아이템 수에 근접했다면 (스크롤이 바닥에 옴)
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                        && firstVisibleItemPosition >= 0
                        && totalItemCount >= pageSize) {
                        loadTransactions(false)
                    }
                }
            }
        })
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
                    startActivity(Intent(this, SettingsActivity::class.java))
                }
                R.id.nav_transactions -> {
                    // Already here
                    drawerLayout.closeDrawer(GravityCompat.START)
                }
                R.id.nav_exit -> {
                    finishAffinity() // Close App
                }
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    private fun initDates() {
        val calendar = Calendar.getInstance()
        updateDateButtons(calendar, true)
        updateDateButtons(calendar, false)
    }
    
    private fun showDatePicker(isFrom: Boolean) {
        val calendar = Calendar.getInstance()
        val dateStr = if (isFrom) fromDateStr else toDateStr

        if (dateStr.isNotEmpty()) {
            try {
                // Determine which format to parse based on length or trial
                // Currently fromDateStr is formatted as "yyyyMMdd" by dateFormat
                calendar.time = dateFormat.parse(dateStr)!!
            } catch (e: Exception) {
                // Fallback to current date if parsing fails
                e.printStackTrace()
            }
        }

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

    private fun startNewSearch() {
        currentPage = 1
        isLastPage = false
        transactionList.clear()
        transactionAdapter.notifyDataSetChanged()
        loadTransactions(true)
    }

    private fun loadTransactions(isNew: Boolean) {
        if (isLoading) return
        isLoading = true
        
        if (isNew) {
            tvStatus.text = getString(R.string.status_searching)
            btnConnect.isEnabled = false
        }
        
        // Load config directly from manager, not UI
        val config = configManager.loadConfig()
        
        CoroutineScope(Dispatchers.IO).launch {
            val dbHelper = DatabaseHelper()
            val results = dbHelper.connectAndQuery(config, fromDateStr, toDateStr, currentPage, pageSize)
            
            withContext(Dispatchers.Main) {
                isLoading = false
                btnConnect.isEnabled = true
                
                if (results.isNotEmpty() && results[0].id == "Error") {
                    if (isNew) {
                        tvStatus.text = getString(R.string.status_failed)
                        Toast.makeText(this@MainActivity, results[0].details, Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this@MainActivity, "Load Error: " + results[0].details, Toast.LENGTH_SHORT).show()
                    }
                } else {
                    if (results.size < pageSize) {
                        isLastPage = true
                    }
                    
                    if (results.isNotEmpty()) {
                        transactionList.addAll(results)
                        transactionAdapter.notifyDataSetChanged()
                        currentPage++
                    } else if (isNew) {
                        tvStatus.text = getString(R.string.status_found_format, 0)
                    }
                    
                    if (transactionList.isNotEmpty()) {
                        tvStatus.text = getString(R.string.status_found_format, transactionList.size)
                    }
                }
            }
        }
    }
}
