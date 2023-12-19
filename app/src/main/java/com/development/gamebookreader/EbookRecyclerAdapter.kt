package com.development.gamebookreader
import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder

data class EbookData(val title: String, val uri: Uri)

class EbookRecyclerAdapter(
    private val context: Context,
    private val ebookList: List<EbookData>
) : RecyclerView.Adapter<EbookRecyclerAdapter.EbookViewHolder>() {


    inner class EbookViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ebookThumbnail: ImageView = itemView.findViewById(R.id.ebookThumbnail)
        val ebookName: TextView = itemView.findViewById(R.id.ebookName)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EbookViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_ebook, parent, false)
        return EbookViewHolder(view)
    }


    override fun onBindViewHolder(holder: EbookViewHolder, position: Int) {
        val ebook = ebookList[position]

        // Set the name of the download
        holder.ebookName.text = ebook.title

        // Load and set the first page of the PDF as an image (you need to extract this page)
        val pdfUri = ebook.uri // Replace with the URI of the PDF file
        val firstPageImage = generateFirstPageImage(pdfUri)
        holder.ebookThumbnail.setImageBitmap(firstPageImage)
    }

    override fun getItemCount(): Int {
        return ebookList.size
    }

    private fun generateFirstPageImage(pdfUri: Uri): Bitmap? {
        try {
            val pdfRenderer = context.contentResolver.openFileDescriptor(pdfUri, "r")
                ?.let { PdfRenderer(it) }
            val page = pdfRenderer?.openPage(0)
            val bitmap = page?.let { Bitmap.createBitmap(it.width, page.height, Bitmap.Config.ARGB_8888) }
            if (bitmap != null) {
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            }
            if (page != null) {
                page.close()
            }
            if (pdfRenderer != null) {
                pdfRenderer.close()
            }
            return bitmap
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

}