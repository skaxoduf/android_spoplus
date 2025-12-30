package com.skaxoduf.spoplus

import android.os.StrictMode
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.sql.Statement

data class TransactionItem(
    val id: String,
    val details: String,
    val sukangban: String
)

class DatabaseHelper {

    fun connectAndQuery(config: DbConfig, dateFrom: String, dateTo: String, page: Int = 1, pageSize: Int = 10): List<TransactionItem> {
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
                // Pagination Logic: OFFSET (page-1)*pageSize ROWS FETCH NEXT pageSize ROWS ONLY
                // Requires SQL Server 2012+
                val offset = (page - 1) * pageSize
                val query = """
                    SELECT uid, trsno, startdate, enddate, sukangban 
                    FROM t_trsinout 
                    WHERE trsdate BETWEEN ? AND ?
                    ORDER BY uid DESC
                    OFFSET ? ROWS FETCH NEXT ? ROWS ONLY
                """.trimIndent()
                
                val stmt = connection.prepareStatement(query)
                stmt.setString(1, dateFrom)
                stmt.setString(2, dateTo)
                stmt.setInt(3, offset)
                stmt.setInt(4, pageSize)
                
                val rs = stmt.executeQuery()
                
                while (rs.next()) {
                    val uid = rs.getString("uid") ?: "-"
                    val trsno = rs.getString("trsno") ?: "-"
                    val start = rs.getString("startdate") ?: ""
                    val end = rs.getString("enddate") ?: ""
                    val sukang = rs.getString("sukangban") ?: ""
                    
                    // Display format: 
                    // Title: [UID] TRS-NO
                    // Details: 20240101 ~ 20240131
                    items.add(TransactionItem("[$uid] $trsno", "$start ~ $end", sukang))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            items.add(TransactionItem("Error", "${e.javaClass.simpleName}: ${e.message}", ""))
        } finally {
            try { connection?.close() } catch (e: Exception) {}
        }
        
        return items
    }
}
