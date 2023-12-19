package com.development.gamebookreader


import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.webkit.WebView
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageTree
import com.tom_roush.pdfbox.rendering.PDFRenderer
import java.io.ByteArrayOutputStream

class MainActivity : AppCompatActivity() {

    var scaleUp: Animation? = null
    var scaleDown:Animation? = null

    private lateinit var pageNumberTextView: TextView
    private lateinit var setPageToOneButton: Button

    private lateinit var pageNumberEditText: TextView
    private lateinit var jumpToPageButton: Button

    private lateinit var setVisible: Button
    private lateinit var firstLinear: LinearLayout
    private lateinit var secondLinear: LinearLayout

    private var currentPageNumber: Int = 1
    private var initialPageNumber: Int = 1 // Initial page number when the activity starts
    private var pagesToAdjust: Int = 0

    private lateinit var viewPager: ViewPager
    private var pdfUri: Uri? = null

    private val fadeInAnimation = AlphaAnimation(0f, 1f)
    private val fadeOutAnimation = AlphaAnimation(1f, 0f)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        PDFBoxResourceLoader.init(applicationContext)
        scaleUp = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.scale_up)
        scaleDown = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.scale_down)
        viewPager = findViewById(R.id.viewPager)
        pageNumberTextView = findViewById(R.id.pageNumberTextView)
        setPageToOneButton = findViewById(R.id.setPageToOneButton)

        setVisible= findViewById(R.id.visibilityToggleButton)
        firstLinear = findViewById(R.id.theLinear)
        secondLinear = findViewById(R.id.theLinearTwo)

        var isVisibilityOn = true

        setVisible.setOnClickListener {
            if (isVisibilityOn) {
                // Turn off visibility
                firstLinear.visibility = View.INVISIBLE
                secondLinear.visibility = View.INVISIBLE
            } else {
                // Turn on visibility
                firstLinear.visibility = View.VISIBLE
                secondLinear.visibility = View.VISIBLE
            }
            isVisibilityOn = !isVisibilityOn
        }

        setVisible.setOnTouchListener(OnTouchListener { view, motionEvent ->
            if (motionEvent.action == MotionEvent.ACTION_DOWN) {
                setVisible.startAnimation(scaleUp)
            } else if (motionEvent.action == MotionEvent.ACTION_UP) {
                setVisible.startAnimation(scaleDown)
            }
            false
        })


        pdfUri = intent?.data

        fadeInAnimation.duration = 1000 // Adjust the duration as needed
        fadeOutAnimation.duration = 1000 // Adjust the duration as needed
        fadeOutAnimation.fillAfter = true

        if (pdfUri != null) {
            val adapter = createEbookPagerAdapter()
            viewPager.adapter = adapter
        }

        pageNumberEditText = findViewById(R.id.pageNumberEditText)
        jumpToPageButton = findViewById(R.id.enterPageNumber)

        jumpToPageButton.setOnClickListener {

            val inputPageNumber = pageNumberEditText.text.toString()
            if (inputPageNumber.isNotEmpty()) {
                val pageToJump = inputPageNumber.toInt()
                if (pageToJump >= 1 && pageToJump <= viewPager.adapter?.count ?: 0) {
                    val offsetPage = pageToJump + pagesToAdjust
                    currentPageNumber = pageToJump // Update the currentPageNumber
                    viewPager.setCurrentItem(offsetPage - 1, true)

                } else {
                    Toast.makeText(
                        this,
                        "Invalid page number. Please enter a valid page number.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        // Set button click listener
        setPageToOneButton.setOnClickListener {
            if (initialPageNumber != 1) {
                pagesToAdjust = initialPageNumber - 1
                initialPageNumber = 1
                currentPageNumber = 1
                updatePageNumbers()

            }
        }

        setPageToOneButton.setOnTouchListener(OnTouchListener { view, motionEvent ->
            if (motionEvent.action == MotionEvent.ACTION_DOWN) {
                setPageToOneButton.startAnimation(scaleUp)
            } else if (motionEvent.action == MotionEvent.ACTION_UP) {
                setPageToOneButton.startAnimation(scaleDown)
            }
            false
        })

        jumpToPageButton.setOnTouchListener(OnTouchListener { view, motionEvent ->
            if (motionEvent.action == MotionEvent.ACTION_DOWN) {
                jumpToPageButton.startAnimation(scaleUp)
            } else if (motionEvent.action == MotionEvent.ACTION_UP) {
                jumpToPageButton.startAnimation(scaleDown)
            }
            false
        })



        // Add an OnPageChangeListener to update the page number
        viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
                // Not needed for this functionality
            }

            override fun onPageSelected(position: Int) {
                currentPageNumber = position - pagesToAdjust + 1
                updatePageNumbers()
            }

            override fun onPageScrollStateChanged(state: Int) {
                // Not needed for this functionality
            }
        })

        // Set the initial page number
        viewPager.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            // This will be called when the ViewPager layout is ready
            initialPageNumber = viewPager.currentItem + 1
            currentPageNumber = initialPageNumber

        }
    }

    private fun createEbookPagerAdapter(): EbookPagerAdapter? {
        val pdfInputStream = pdfUri?.let { contentResolver.openInputStream(it) }

        return pdfInputStream?.let { inputStream ->
            val document = PDDocument.load(inputStream)
            val pages = extractPdfPages(document, currentPageNumber) // Use the current page number
            val renderer = PDFRenderer(document)
            EbookPagerAdapter(this, pages, renderer)
        }
    }

    private fun extractPdfPages(document: PDDocument, currentPageNumber: Int): List<PDPage> {
        val pageList = mutableListOf<PDPage>()
        val catalog = document.documentCatalog
        val pages = catalog.pages as PDPageTree

        // Calculate the index of the first page based on the currentPageNumber
        val firstPageIndex = currentPageNumber - 1

        for (i in firstPageIndex until pages.count) {
            val page = pages[i] as PDPage
            pageList.add(page)
        }

        return pageList
    }


    private class EbookPagerAdapter(
        private val context: MainActivity,
        private val pages: List<PDPage>,
        private val renderer: PDFRenderer
    ) : PagerAdapter() {


        override fun getCount(): Int {
            return pages.size
        }

        override fun isViewFromObject(view: View, `object`: Any): Boolean {
            return view === `object`
        }

        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            val pageImage = renderer.renderImage(position)
            val screenWidth = context.resources.displayMetrics.widthPixels
            val screenHeight = context.resources.displayMetrics.heightPixels
            val layout = WebView(context)
            layout.layoutParams = ViewGroup.LayoutParams(screenWidth, screenHeight)

            // Set WebView settings for scaling
            layout.settings.useWideViewPort = true
            layout.settings.loadWithOverviewMode = true
            layout.settings.builtInZoomControls = true
            layout.settings.displayZoomControls = false

            layout.loadDataWithBaseURL(
                null,
                getImageHtml(pageImage, screenWidth, screenHeight),
                "text/html",
                "UTF-8",
                null
            )


            container.addView(layout)
            return layout
        }


        override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
            container.removeView(`object` as View)
        }

        private fun getImageHtml(pageImage: Bitmap, screenWidth: Int, screenHeight: Int): String {
            val outputStream = ByteArrayOutputStream()
            pageImage.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            val base64Image = Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)

            val imageTag =
                "<img src='data:image/png;base64,$base64Image' width='$screenWidth' height='$screenHeight'/>"

            return "<html><body style='margin:0;padding:0;'>$imageTag</body></html>"
        }

    }

    private fun updatePageNumbers() {
        val totalPageCount = viewPager.adapter?.count ?: 0
        val formattedText = "$currentPageNumber"
        pageNumberTextView.text = formattedText
    }
}
