package com.skaxoduf.spoplus

import android.os.StrictMode
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.sql.Statement

data class TransactionItem(
    val id: String,
    val details: String
)

class DatabaseHelper {

    fun connectAndQuery(config: DbConfig, dateFrom: String, dateTo: String): List<TransactionItem> {
        // [SAFETY] Forcefully allow network on main thread just in case, to prevent "Keeps Stopping"
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        val items = mutableListOf<TransactionItem>()
        var connection: Connection? = null
        
        try {
            // Using JTDS Driver
            Class.forName("net.sourceforge.jtds.jdbc.Driver")

            val serverAddress = config.server.replace(",", ":")
            
            // JTDS Connection URL Format: jdbc:jtds:sqlserver://<server_name>[:<port>]/<database_name>
            val connectionUrl = "jdbc:jtds:sqlserver://$serverAddress/${config.database};user=${config.user};password=${config.pass};"

            connection = DriverManager.getConnection(connectionUrl)
            
            if (connection != null) {
                // Assuming trsdate is convertible to string or is a string 'YYYYMMDD' or 'YYYY-MM-DD'
                // We'll use simple string comparison which works for ISO formats and YYYYMMDD
                val query = "SELECT uid, trsno, startdate, enddate FROM t_trsinout WHERE trsdate BETWEEN ? AND ?" 
                
                val stmt = connection.prepareStatement(query)
                stmt.setString(1, dateFrom)
                stmt.setString(2, dateTo)
                
                val rs = stmt.executeQuery()
                
                while (rs.next()) {
                    val uid = rs.getString("uid") ?: "-"
                    val trsno = rs.getString("trsno") ?: "-"
                    val start = rs.getString("startdate") ?: ""
                    val end = rs.getString("enddate") ?: ""
                    
                    // Display format: 
                    // Title: [UID] TRS-NO
                    // Details: 20240101 ~ 20240131
                    items.add(TransactionItem("[$uid] $trsno", "$start ~ $end"))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            items.add(TransactionItem("Error", "${e.javaClass.simpleName}: ${e.message}"))
        } finally {
            try { connection?.close() } catch (e: Exception) {}
        }
        
        return items
    }
}
