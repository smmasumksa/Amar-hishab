package com.example.amarhishab

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONArray
import org.json.JSONObject

class FinanceDb(context: Context) : SQLiteOpenHelper(context, "amar_hishab.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE transactions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                date TEXT NOT NULL,
                type TEXT NOT NULL,
                description TEXT NOT NULL,
                category TEXT NOT NULL,
                amount REAL NOT NULL
            )
        """.trimIndent())
        db.execSQL("CREATE TABLE settings (key TEXT PRIMARY KEY, value TEXT NOT NULL)")
        db.execSQL("INSERT INTO settings(key,value) VALUES('opening_balance','1362.0')")
        seed(db)
    }

    private fun seed(db: SQLiteDatabase) {
        val rows = listOf(
            arrayOf("2026-10-06","INCOME","১১.৫ × ১০ ইনকাম","সাধারণ ইনকাম","115.0"),
            arrayOf("2026-10-06","EXPENSE","ফুয়েল","জ্বালানি / গাড়ি","18.0"),
            arrayOf("2026-10-06","EXPENSE","নাস্তা","খাবার","6.0"),
            arrayOf("2026-10-07","INCOME","১০ × ১০ ইনকাম","সাধারণ ইনকাম","100.0"),
            arrayOf("2026-10-07","EXPENSE","বাড়িতে পাঠালাম","রেমিট্যান্স / পারিবারিক সাহায্য","100.0"),
            arrayOf("2026-10-08","INCOME","১৪.৫ × ১০ ইনকাম","সাধারণ ইনকাম","145.0"),
            arrayOf("2026-10-08","EXPENSE","নাস্তা","খাবার","10.0"),
            arrayOf("2026-10-09","INCOME","৭.৫ × ১০ ইনকাম","সাধারণ ইনকাম","75.0"),
            arrayOf("2026-10-09","EXPENSE","বাশার বাজার - আদা, রসুন","বাজার / খাদ্যসামগ্রী","22.0"),
            arrayOf("2026-10-09","EXPENSE","নাস্তা","খাবার","6.0"),
            arrayOf("2026-10-10","INCOME","৮.৫ × ১০ ইনকাম","সাধারণ ইনকাম","85.0"),
            arrayOf("2026-10-10","EXPENSE","ফুয়েল","জ্বালানি / গাড়ি","20.0")
        )
        rows.forEach { r ->
            val v = ContentValues().apply {
                put("date", r[0]); put("type", r[1]); put("description", r[2])
                put("category", r[3]); put("amount", r[4].toDouble())
            }
            db.insert("transactions", null, v)
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}

    fun all(): List<Transaction> {
        val result = mutableListOf<Transaction>()
        readableDatabase.rawQuery(
            "SELECT id,date,type,description,category,amount FROM transactions ORDER BY date DESC,id DESC", null
        ).use { c ->
            while (c.moveToNext()) result.add(Transaction(c.getLong(0), c.getString(1), c.getString(2),
                c.getString(3), c.getString(4), c.getDouble(5)))
        }
        return result
    }

    fun add(date: String, type: String, description: String, category: String, amount: Double) {
        val v = ContentValues().apply {
            put("date", date); put("type", type); put("description", description)
            put("category", category); put("amount", amount)
        }
        writableDatabase.insert("transactions", null, v)
    }

    fun totals(): Triple<Double, Double, Double> {
        var income = 0.0; var expense = 0.0
        all().forEach { if (it.type == "INCOME") income += it.amount else expense += it.amount }
        val opening = readableDatabase.rawQuery("SELECT value FROM settings WHERE key='opening_balance'", null).use {
            if (it.moveToFirst()) it.getString(0).toDoubleOrNull() ?: 0.0 else 0.0
        }
        return Triple(income, expense, opening + income - expense)
    }

    fun exportJson(): String {
        val arr = JSONArray()
        all().forEach { t ->
            arr.put(JSONObject().put("date", t.date).put("type", t.type).put("description", t.description)
                .put("category", t.category).put("amount", t.amount))
        }
        return JSONObject().put("app", "আমার হিসাব").put("version", 1)
            .put("openingBalance", 1362.0).put("transactions", arr).toString(2)
    }

    fun importJson(json: String) {
        val root = JSONObject(json)
        val arr = root.getJSONArray("transactions")
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete("transactions", null, null)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val v = ContentValues().apply {
                    put("date", o.getString("date")); put("type", o.getString("type"))
                    put("description", o.optString("description", "")); put("category", o.optString("category", "অন্যান্য"))
                    put("amount", o.getDouble("amount"))
                }
                db.insert("transactions", null, v)
            }
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }

    fun replaceFromCloud(items: List<Transaction>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete("transactions", null, null)
            items.forEach { t ->
                val v = ContentValues().apply {
                    put("date", t.date); put("type", t.type); put("description", t.description)
                    put("category", t.category); put("amount", t.amount)
                }
                db.insert("transactions", null, v)
            }
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }
}
