package com.example.dam_front.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.dam_front.ui.screens.AiSummaryResult
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.List as PdfList
import com.itextpdf.layout.element.ListItem
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import java.io.File
import java.io.FileOutputStream

object PdfService {

    fun generateSummaryPdf(context: Context, childName: String, summary: AiSummaryResult): File? {
        return try {
            val fileName = "Resume_IA_${childName.replace(" ", "_")}_${System.currentTimeMillis()}.pdf"
            val file = File(context.cacheDir, fileName)
            val writer = PdfWriter(FileOutputStream(file))
            val pdf = PdfDocument(writer)
            val document = Document(pdf)

            // Header
            document.add(Paragraph("Rapport d'Analyse Sportive IA")
                .setBold()
                .setFontSize(24f)
                .setTextAlignment(TextAlignment.CENTER)
                .setFontColor(ColorConstants.BLUE))

            document.add(Paragraph("Élève : $childName")
                .setBold()
                .setFontSize(18f)
                .setMarginTop(10f))

            document.add(Paragraph("Date du rapport : ${java.text.SimpleDateFormat("dd/MM/yyyy").format(java.util.Date())}")
                .setFontSize(12f)
                .setItalic())

            // Divider
            document.add(Paragraph("______________________________________________________________________________")
                .setMarginBottom(20f))

            // Summary
            document.add(Paragraph("Résumé Global")
                .setBold()
                .setFontSize(16f)
                .setFontColor(ColorConstants.DARK_GRAY))
            document.add(Paragraph(summary.shortSummary)
                .setMarginBottom(15f))

            // Strengths
            document.add(Paragraph("Points Forts")
                .setBold()
                .setFontSize(16f)
                .setFontColor(ColorConstants.GREEN))
            val strengthsList: PdfList = PdfList()
            summary.strengths.forEach { strengthsList.add(ListItem(it)) }
            document.add(strengthsList.setMarginBottom(15f))

            // Improvements
            document.add(Paragraph("Axes d'Amélioration")
                .setBold()
                .setFontSize(16f)
                .setFontColor(ColorConstants.ORANGE))
            val improvementsList: PdfList = PdfList()
            summary.improvements.forEach { improvementsList.add(ListItem(it)) }
            document.add(improvementsList.setMarginBottom(15f))

            // Counsel
            document.add(Paragraph("Conseil aux Parents")
                .setBold()
                .setFontSize(16f)
                .setFontColor(ColorConstants.CYAN))
            document.add(Paragraph(summary.counsel)
                .setMarginBottom(15f))

            // Goals
            document.add(Paragraph("Objectifs Futurs")
                .setBold()
                .setFontSize(16f)
                .setFontColor(ColorConstants.RED))
            val goalsList: PdfList = PdfList()
            summary.goals.forEach { goalsList.add(ListItem(it)) }
            document.add(goalsList)

            // Footer
            document.add(Paragraph("\nGénéré par SportyKids IA")
                .setFontSize(10f)
                .setItalic()
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(30f))

            document.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun sharePdf(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Partager le rapport PDF"))
    }

    fun sendEmailWithPdf(context: Context, file: File, childName: String, parentEmail: String? = null) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "message/rfc822"
            if (parentEmail != null) putExtra(Intent.EXTRA_EMAIL, arrayOf(parentEmail))
            putExtra(Intent.EXTRA_SUBJECT, "[SportyKids] Rapport d'Analyse IA - $childName")
            putExtra(Intent.EXTRA_TEXT, "Bonjour,\n\nVous trouverez en pièce jointe le rapport d'analyse sportive généré par l'IA SportyKids pour votre enfant $childName.\n\nSportivement,\nL'équipe SportyKids")
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Envoyer via SportyKids"))
    }
}
