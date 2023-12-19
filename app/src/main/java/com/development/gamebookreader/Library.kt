package com.development.gamebookreader
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.MotionEvent
import android.view.View
import android.view.animation.Animation
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.tom_roush.pdfbox.pdmodel.PDDocument
import java.io.File
import java.io.FileOutputStream


// ...


class Library : AppCompatActivity() {

    var scaleUp: Animation? = null
    var scaleDown: Animation? = null

    private val ADDED_EBOOKS_KEY = "added_ebooks"
    private lateinit var addedEbookURIs: MutableSet<String>
    private val REQUEST_CODE_PICK_FILE = 101
    private lateinit var recyclerView: RecyclerView
    private lateinit var pickFileButton: Button
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var ebookList: MutableList<EbookData>
    private lateinit var ebookRecyclerAdapter: EbookRecyclerAdapter

    @SuppressLint("CutPasteId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_library_gallery)

        val sharedPreferences = getSharedPreferences("LibraryPreferences", Context.MODE_PRIVATE)
        addedEbookURIs = sharedPreferences.getStringSet(ADDED_EBOOKS_KEY, mutableSetOf()) ?: mutableSetOf()

        scaleUp = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.scale_up)
        scaleDown = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.scale_down)

        dbHelper = DatabaseHelper(this)
        ebookList = retrieveEbooksFromDatabase()


        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = GridLayoutManager(this, 3, GridLayoutManager.VERTICAL, false)
        val ebookRecyclerAdapter = EbookRecyclerAdapter(this, ebookList)
        recyclerView.adapter = ebookRecyclerAdapter

        val ebookDirectory = File(getExternalFilesDir(null), "ebooks")
        ebookDirectory.mkdirs()




        pickFileButton = findViewById(R.id.pickFileButton)
        pickFileButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = "*/*"
            startActivityForResult(intent, REQUEST_CODE_PICK_FILE)
        }

        pickFileButton.setOnTouchListener(View.OnTouchListener { view, motionEvent ->
            if (motionEvent.action == MotionEvent.ACTION_DOWN) {
                pickFileButton.startAnimation(scaleUp)
            } else if (motionEvent.action == MotionEvent.ACTION_UP) {
                pickFileButton.startAnimation(scaleDown)
            }
            false
        })

        recyclerView.addOnItemTouchListener(
            RecyclerItemClickListener(this, recyclerView, object : RecyclerItemClickListener.OnItemClickListener {
                override fun onItemClick(view: View, position: Int) {
                    val selectedEbook = ebookList[position]
                    openEbook(selectedEbook.uri) // Use 'uri' instead of 'filePath'
                }
            })
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_PICK_FILE && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                val uriString = uri.toString()
                if (!addedEbookURIs.contains(uriString)) { // Check if URI is not in the set
                    addedEbookURIs.add(uriString) // Add the URI to the set
                    val title = retrievePdfTitle(uri)

                    val ebookDirectory = File(getExternalFilesDir(null), "ebooks")
                    ebookDirectory.mkdirs()
                    val outputFile = File(ebookDirectory, "${System.currentTimeMillis()}_$title")
                    val savedUri = Uri.fromFile(outputFile).toString()

                    val inputStream = contentResolver.openInputStream(uri)
                    val outputStream = FileOutputStream(outputFile)

                    inputStream?.use { input ->
                        outputStream.use { output ->
                            input.copyTo(output)
                        }
                    }

                    val ebookData = EbookData(title, Uri.parse(savedUri))
                    ebookList.add(ebookData)
                    addEbookToDatabase(ebookData)
                    (recyclerView.adapter as EbookRecyclerAdapter).notifyDataSetChanged()
                } else {
                    showErrorMessage("File is already in the library.")
                }
            }
        }
    }




    private fun openEbook(uri: Uri) {
            val file = File(Uri.parse(uri.toString()).path.toString())
            if (file.exists()) {
                // The file exists and can be opened
                val intent = Intent(this, MainActivity::class.java)
                intent.data = Uri.fromFile(file)
                startActivity(intent)
            } else {
                // Handle the case when the file doesn't exist
                showErrorMessage("File not found")
            }
        }

        private fun retrieveEbooksFromDatabase(): MutableList<EbookData> {
        val ebooks = mutableListOf<EbookData>()
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM ${EbookTable.TABLE_NAME}", null)
        while (cursor.moveToNext()) {
            val title = cursor.getString(cursor.getColumnIndex(EbookTable.COLUMN_TITLE))
            val uriString = cursor.getString(cursor.getColumnIndex(EbookTable.COLUMN_URI))

            // Safely parse the URI
            val uri = Uri.parse(uriString)

            // Ensure the URI is not null before adding to the list
            if (uri != null) {
                ebooks.add(EbookData(title, uri))
            }
        }
        cursor.close()
        db.close()
        return ebooks
    }

    private fun addEbookToDatabase(ebookData: EbookData) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(EbookTable.COLUMN_TITLE, ebookData.title)
            put(EbookTable.COLUMN_URI, ebookData.uri.toString())
        }
        db.insert(EbookTable.TABLE_NAME, null, values)
        db.close()

        // Save the added URIs to SharedPreferences
        val sharedPreferences = getSharedPreferences("LibraryPreferences", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putStringSet(ADDED_EBOOKS_KEY, addedEbookURIs)
        editor.apply()
    }
    private fun showErrorMessage(message: String) {
        // Use a Toast message
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

        // Or use a Snackbar for more prominent and dismissible messages
        val rootView = findViewById<View>(android.R.id.content)
        Snackbar.make(rootView, message, Snackbar.LENGTH_SHORT).show()
    }
    private fun retrievePdfTitle(uri: Uri): String {
        val contentResolver = contentResolver
        val inputStream = contentResolver.openInputStream(uri)

        try {
            if (inputStream != null) {
                val pdfDocument = PDDocument.load(inputStream)
                val info = pdfDocument.documentInformation
                val title = info.title
                pdfDocument.close()
                return if (title.isNullOrEmpty()) {
                    "Untitled"
                } else {
                    title
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return "Untitled"
    }






}