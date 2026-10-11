package com.example.amarhishab

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import org.json.JSONArray
import java.util.Calendar

class MainActivity : AppCompatActivity() {
    private lateinit var db: FinanceDb
    private lateinit var root: LinearLayout
    private lateinit var totalsView: TextView
    private lateinit var listView: LinearLayout
    private lateinit var auth: FirebaseAuth
    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val signInCode = 6041
    private var selectedDate = "2026-10-11"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = FinanceDb(this)
        auth = FirebaseAuth.getInstance()
        buildUi()
        refresh()
    }

    private fun buildUi() {
        val scroll = ScrollView(this)
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 24, 28, 32)
            setBackgroundColor(0xFFF5F7F5.toInt())
        }
        scroll.addView(root)
        setContentView(scroll)

        val heading = TextView(this).apply {
            text = "আমার হিসাব"; textSize = 28f; setTextColor(0xFF145A40.toInt())
            gravity = Gravity.START; setPadding(0, 0, 0, 8)
        }
        root.addView(heading)
        root.addView(label("মাস ১২  •  ০৬ অক্টোবর – ০৫ নভেম্বর ২০২৬", 14f))
        totalsView = label("", 17f)
        totalsView.setPadding(0, 18, 0, 16)
        root.addView(totalsView)

        val addTitle = label("নতুন লেনদেন যোগ করুন", 20f)
        addTitle.setPadding(0, 12, 0, 8)
        root.addView(addTitle)

        val type = Spinner(this)
        type.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, listOf("খরচ", "ইনকাম"))
        root.addView(type)

        val dateButton = Button(this).apply { text = "তারিখ: $selectedDate" }
        dateButton.setOnClickListener {
            val c = Calendar.getInstance()
            DatePickerDialog(this, { _, y, m, d ->
                selectedDate = "%04d-%02d-%02d".format(y, m + 1, d)
                dateButton.text = "তারিখ: $selectedDate"
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
        }
        root.addView(dateButton)

        val description = EditText(this).apply { hint = "বিবরণ (যেমন: ফুয়েল, নাস্তা)" }
        root.addView(description)
        val category = EditText(this).apply { hint = "ক্যাটাগরি" }
        root.addView(category)
        val amount = EditText(this).apply { hint = "পরিমাণ (SAR)"; inputType = 8194 }
        root.addView(amount)

        root.addView(Button(this).apply {
            text = "লেনদেন সংরক্ষণ"
            setOnClickListener {
                val amt = amount.text.toString().toDoubleOrNull()
                if (amt == null || amt <= 0 || description.text.isBlank()) {
                    Toast.makeText(this@MainActivity, "বিবরণ ও সঠিক পরিমাণ লিখুন", Toast.LENGTH_SHORT).show()
                } else {
                    db.add(selectedDate, if (type.selectedItemPosition == 1) "INCOME" else "EXPENSE",
                        description.text.toString().trim(),
                        category.text.toString().trim().ifBlank { "অন্যান্য" }, amt)
                    description.text.clear(); category.text.clear(); amount.text.clear()
                    refresh()
                }
            }
        })

        val actionRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        actionRow.addView(Button(this).apply {
            text = "Google লগইন"
            setOnClickListener { signIn() }
        }, LinearLayout.LayoutParams(0, -2, 1f))
        actionRow.addView(Button(this).apply {
            text = "ক্লাউড সিঙ্ক"
            setOnClickListener { syncCloud() }
        }, LinearLayout.LayoutParams(0, -2, 1f))
        root.addView(actionRow)

        val backupRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        backupRow.addView(Button(this).apply {
            text = "ব্যাকআপ Export"
            setOnClickListener {
                val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE); type = "application/json"
                    putExtra(Intent.EXTRA_TITLE, "amar-hishab-backup.json")
                }
                startActivityForResult(intent, 6101)
            }
        }, LinearLayout.LayoutParams(0, -2, 1f))
        backupRow.addView(Button(this).apply {
            text = "ব্যাকআপ Restore"
            setOnClickListener {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE); type = "application/json"
                }
                startActivityForResult(intent, 6102)
            }
        }, LinearLayout.LayoutParams(0, -2, 1f))
        root.addView(backupRow)

        root.addView(label("সাম্প্রতিক লেনদেন", 20f).apply { setPadding(0, 18, 0, 8) })
        listView = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(listView)
    }

    private fun label(text: String, size: Float) = TextView(this).apply {
        this.text = text; textSize = size; setTextColor(0xFF26332D.toInt())
    }

    private fun refresh() {
        val (income, expense, closing) = db.totals()
        totalsView.text = "মোট ইনকাম: %.2f SAR\nমোট খরচ: %.2f SAR\nনিট সঞ্চয়: %.2f SAR\nক্যাশ ব্যালেন্স: %.2f SAR".format(
            income, expense, income - expense, closing)
        listView.removeAllViews()
        db.all().forEach { t ->
            val sign = if (t.type == "INCOME") "+" else "−"
            val row = TextView(this).apply {
                text = "${t.date}   $sign %.2f SAR\n${t.description}  •  ${t.category}".format(t.amount)
                textSize = 15f; setTextColor(0xFF26332D.toInt())
                setPadding(12, 12, 12, 12)
                setBackgroundColor(0xFFFFFFFF.toInt())
            }
            val params = LinearLayout.LayoutParams(-1, -2)
            params.bottomMargin = 8
            listView.addView(row, params)
        }
    }

    private fun signIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail().build()
        val client = GoogleSignIn.getClient(this, gso)
        startActivityForResult(client.signInIntent, signInCode)
    }

    private fun syncCloud() {
        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(this, "আগে Google লগইন করুন", Toast.LENGTH_SHORT).show()
            return
        }
        val ref = firestore.collection("users").document(user.uid)
            .collection("finance").document("backup")
        ref.get().addOnSuccessListener { snapshot ->
            if (!snapshot.exists()) {
                uploadLocal(ref)
                return@addOnSuccessListener
            }
            val options = arrayOf("এই ফোনের হিসাব ক্লাউডে আপলোড", "ক্লাউডের হিসাব এই ফোনে Restore")
            android.app.AlertDialog.Builder(this)
                .setTitle("সিঙ্কের দিক বেছে নিন")
                .setItems(options) { _, which ->
                    if (which == 0) uploadLocal(ref) else {
                        val raw = snapshot.get("transactions") as? List<*> ?: emptyList<Any>()
                        val items = raw.mapNotNull { item ->
                            val m = item as? Map<*, *> ?: return@mapNotNull null
                            Transaction(
                                date = m["date"]?.toString() ?: return@mapNotNull null,
                                type = m["type"]?.toString() ?: "EXPENSE",
                                description = m["description"]?.toString() ?: "",
                                category = m["category"]?.toString() ?: "অন্যান্য",
                                amount = (m["amount"] as? Number)?.toDouble() ?: return@mapNotNull null
                            )
                        }
                        db.replaceFromCloud(items)
                        refresh()
                        Toast.makeText(this, "ক্লাউডের হিসাব Restore হয়েছে", Toast.LENGTH_SHORT).show()
                    }
                }.show()
        }.addOnFailureListener {
            Toast.makeText(this, "ক্লাউড পড়া যায়নি: ${it.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    private fun uploadLocal(ref: com.google.firebase.firestore.DocumentReference) {
        val tx = db.all().map { it.toMap() }
        val payload = hashMapOf<String, Any>(
            "transactions" to tx,
            "updatedAt" to System.currentTimeMillis(),
            "openingBalance" to 1362.0
        )
        ref.set(payload).addOnSuccessListener {
            Toast.makeText(this, "ক্লাউডে আপলোড সম্পন্ন", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener {
            Toast.makeText(this, "সিঙ্ক হয়নি: ${it.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK || data == null) return
        if (requestCode != signInCode && data.data == null) return
        when (requestCode) {
            signInCode -> try {
                val account = GoogleSignIn.getSignedInAccountFromIntent(data).getResult(ApiException::class.java)
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                auth.signInWithCredential(credential).addOnSuccessListener {
                    Toast.makeText(this, "Google লগইন সফল", Toast.LENGTH_SHORT).show()
                }.addOnFailureListener {
                    Toast.makeText(this, "লগইন ব্যর্থ: ${it.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Google লগইন ব্যর্থ", Toast.LENGTH_SHORT).show()
            }
            6101 -> try {
                contentResolver.openOutputStream(data.data!!)?.use { it.write(db.exportJson().toByteArray(Charsets.UTF_8)) }
                Toast.makeText(this, "ব্যাকআপ ফাইল সেভ হয়েছে", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "ব্যাকআপ ব্যর্থ", Toast.LENGTH_SHORT).show()
            }
            6102 -> try {
                val json = contentResolver.openInputStream(data.data!!)?.bufferedReader()?.use { it.readText() } ?: ""
                db.importJson(json); refresh()
                Toast.makeText(this, "ব্যাকআপ Restore হয়েছে", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "ফাইল সঠিক নয় বা Restore ব্যর্থ", Toast.LENGTH_LONG).show()
            }
        }
    }
}
