package com.example.dam_front.utils

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfUtils {

    fun generateSummaryPdf(context: Context, childName: String, summaryText: String): File? {
        val pdfDocument = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint()

        val sportyBlue = android.graphics.Color.parseColor("#1A5276")
        val sportyGreen = android.graphics.Color.parseColor("#27AE60")
        val textGray = android.graphics.Color.parseColor("#34495E")

        // Draw Header background
        paint.color = sportyBlue
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), 120f, paint)

        // Header Title
        paint.color = android.graphics.Color.WHITE
        paint.textSize = 24f
        paint.isFakeBoldText = true
        canvas.drawText("SPORTYKIDS", 40f, 55f, paint)
        
        paint.textSize = 16f
        paint.isFakeBoldText = false
        canvas.drawText("Bilan de Progression : $childName", 40f, 90f, paint)

        // Date on right
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val dateStr = dateFormat.format(Date())
        paint.textSize = 12f
        canvas.drawText("Date: $dateStr", 450f, 90f, paint)

        // Content Area
        var y = 160f
        val x = 40f
        val maxWidth = 515f
        val lineHeight = 20f

        // Process sections
        val sections = summaryText.split("[SECTION_START:")
        
        for (section in sections) {
            if (section.isBlank()) continue
            
            // Extract title and content
            val parts = section.split("]", limit = 2)
            if (parts.size < 2) continue
            
            val sectionType = parts[0]
            val sectionContent = parts[1].trim()

            // Draw Section Header
            paint.color = sportyGreen
            paint.textSize = 14f
            paint.isFakeBoldText = true
            
            // Determine header icon/title
            val headerTitle = when(sectionType) {
                "RÉSUMÉ" -> "🌟 RÉSUMÉ GLOBAL"
                "FORTS" -> "✅ POINTS FORTS"
                "AXES" -> "🚀 AXES D'AMÉLIORATION"
                "CONSEILS" -> "💡 CONSEILS AUX PARENTS"
                "OBJECTIF" -> "🎯 PROCHAIN DÉFI"
                else -> sectionType
            }
            
            canvas.drawText(headerTitle, x, y, paint)
            y += lineHeight + 5

            // Draw Section Body
            paint.color = textGray
            paint.textSize = 12f
            paint.isFakeBoldText = false
            
            val bodyLines = sectionContent.split("\n")
            for (line in bodyLines) {
                if (line.isBlank() && bodyLines.indexOf(line) != bodyLines.size -1) {
                    y += 10f
                    continue
                }
                
                // Wrap text
                var currentText = line.trim()
                if (currentText.startsWith("•") || currentText.startsWith("-")) {
                    currentText = "  " + currentText
                }

                while (currentText.isNotEmpty()) {
                    val count = paint.breakText(currentText, true, maxWidth, null)
                    val subLine = currentText.substring(0, count)
                    canvas.drawText(subLine, x, y, paint)
                    y += lineHeight
                    currentText = currentText.substring(count).trim()
                    
                    if (y > pageHeight - 60) break
                }
            }
            y += 15f // Space between sections
            if (y > pageHeight - 60) break
        }

        // Footer
        paint.color = android.graphics.Color.LTGRAY
        paint.textSize = 10f
        paint.isFakeBoldText = false
        canvas.drawRect(40f, 800f, 555f, 801f, paint) // horizontal line
        canvas.drawText("Document confidentiel généré par SportyKids AI", 40f, 820f, paint)

        pdfDocument.finishPage(page)


        // Save to Downloads folder (visible in Files app)
        val dateStrForFilename = dateStr.replace("/", "-")
        val fileName = "SportyKids_Resume_${childName.replace(" ", "_")}_$dateStrForFilename.pdf"
        
        // Use public Downloads directory
        val directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!directory.exists()) {
            directory.mkdirs()
        }
        
        val file = File(directory, fileName)

        return try {
            pdfDocument.writeTo(FileOutputStream(file))
            pdfDocument.close()
            Log.d("PdfUtils", "PDF saved to: ${file.absolutePath}")
            file
        } catch (e: IOException) {
            Log.e("PdfUtils", "Error writing PDF", e)
            pdfDocument.close()
            null
        }
    }
}
