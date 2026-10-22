package com.drnitz.aro.export

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.drnitz.aro.domain.DomainResult
import com.drnitz.aro.domain.calculation.bodyMassIndex
import com.drnitz.aro.domain.model.DoctorConsultation
import com.drnitz.aro.domain.model.UserProfile
import com.drnitz.aro.domain.model.VitalsEntry
import java.io.File
import java.io.FileOutputStream
import java.time.format.DateTimeFormatter
import java.util.Locale

data class ClinicalReportData(
    val profile: UserProfile,
    val dateRangeText: String,
    val generatedDate: String,
    val vitals: List<VitalsEntry>,
    val consultations: List<DoctorConsultation>,
    val averageCalories: Int,
    val averageProtein: Double,
    val averageCarbs: Double,
    val averageFat: Double,
    val averageFiber: Double,
    val molarNaKRatio: Double?,
    val sodiumMg: Double?,
    val potassiumMg: Double?,
    val clinicalFindings: List<String> = emptyList(),
)

object ClinicalPdfGenerator {

    private const val PAGE_WIDTH = 595 // A4 standard width in points
    private const val PAGE_HEIGHT = 842 // A4 standard height in points
    private const val MARGIN_LEFT = 40f
    private const val MARGIN_RIGHT = 555f
    private const val CONTENT_WIDTH = MARGIN_RIGHT - MARGIN_LEFT
    private const val BOTTOM_PAGE_LIMIT = 770f

    fun generate(context: Context, data: ClinicalReportData): File {
        val document = PdfDocument()

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
        var page = document.startPage(pageInfo)
        var canvas = page.canvas
        var cursorY = 40f

        val paintText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42) // Slate 900
            textSize = 10f
        }
        val paintSecondary = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(100, 116, 139) // Slate 500
            textSize = 9f
        }
        val paintHeader = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42)
            textSize = 16f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val paintSectionTitle = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(16, 185, 129) // Emerald
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val paintFill = Paint(Paint.ANTI_ALIAS_FLAG)
        val paintStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1f
            color = Color.rgb(226, 232, 240) // Slate 200
        }

        fun checkPageBreak(requiredHeight: Float) {
            if (cursorY + requiredHeight > BOTTOM_PAGE_LIMIT) {
                // Draw footer
                drawFooter(canvas, pageNumber)
                document.finishPage(page)

                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                page = document.startPage(pageInfo)
                canvas = page.canvas
                cursorY = 50f
            }
        }

        // --- Document Header ---
        paintFill.color = Color.rgb(15, 23, 42) // Dark Banner
        canvas.drawRoundRect(RectF(MARGIN_LEFT, cursorY, MARGIN_RIGHT, cursorY + 70f), 8f, 8f, paintFill)

        paintHeader.color = Color.WHITE
        canvas.drawText("ARO CLINICAL HEALTH REPORT", MARGIN_LEFT + 16f, cursorY + 28f, paintHeader)

        val paintHeaderSub = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(148, 163, 184)
            textSize = 9.5f
        }
        canvas.drawText(
            "Confidential Medical Summary • Range: ${data.dateRangeText} • Generated: ${data.generatedDate}",
            MARGIN_LEFT + 16f,
            cursorY + 46f,
            paintHeaderSub,
        )
        canvas.drawText(
            "100% Offline Clinical Engine • Aro Health Intelligence",
            MARGIN_LEFT + 16f,
            cursorY + 60f,
            paintHeaderSub,
        )

        cursorY += 85f

        // --- Patient Baseline Card ---
        val bmiResult = bodyMassIndex(data.profile.weightKg, data.profile.heightCm).valueOrNull()
        val bmiValue = bmiResult?.value ?: 0.0
        val bmiLabel = bmiResult?.category?.label ?: "—"

        paintFill.color = Color.rgb(248, 250, 252) // Slate 50
        canvas.drawRoundRect(RectF(MARGIN_LEFT, cursorY, MARGIN_RIGHT, cursorY + 65f), 6f, 6f, paintFill)
        canvas.drawRoundRect(RectF(MARGIN_LEFT, cursorY, MARGIN_RIGHT, cursorY + 65f), 6f, 6f, paintStroke)

        val col1X = MARGIN_LEFT + 14f
        val col2X = MARGIN_LEFT + 180f
        val col3X = MARGIN_LEFT + 350f

        paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Patient: ${data.profile.name}", col1X, cursorY + 20f, paintText)
        paintText.typeface = Typeface.DEFAULT
        canvas.drawText("Sex: ${data.profile.sex.name.lowercase().replaceFirstChar { it.uppercase() }}", col1X, cursorY + 36f, paintSecondary)
        canvas.drawText("Age: ${data.profile.ageYears} yrs", col1X, cursorY + 52f, paintSecondary)

        canvas.drawText("Height: ${data.profile.heightCm.toInt()} cm", col2X, cursorY + 20f, paintSecondary)
        canvas.drawText("Current Weight: %.1f kg".format(Locale.US, data.profile.weightKg), col2X, cursorY + 36f, paintSecondary)
        canvas.drawText("Target Weight: %.1f kg".format(Locale.US, data.profile.targetWeightKg), col2X, cursorY + 52f, paintSecondary)

        paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("BMI: %.1f kg/m² (%s)".format(Locale.US, bmiValue, bmiLabel), col3X, cursorY + 20f, paintText)
        paintText.typeface = Typeface.DEFAULT
        canvas.drawText("Activity: ${data.profile.activityLevel.name.lowercase().replaceFirstChar { it.uppercase() }}", col3X, cursorY + 36f, paintSecondary)
        canvas.drawText("Goal: ${data.profile.goal.name.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }}", col3X, cursorY + 52f, paintSecondary)

        cursorY += 80f

        // --- Section 1: Patient Vitals History ---
        checkPageBreak(50f)
        canvas.drawText("1. PATIENT VITALS & CARDIOVASCULAR METRICS", MARGIN_LEFT, cursorY, paintSectionTitle)
        cursorY += 14f

        if (data.vitals.isEmpty()) {
            canvas.drawText("No patient vitals recorded during this time period.", MARGIN_LEFT, cursorY + 12f, paintSecondary)
            cursorY += 28f
        } else {
            // Table Header
            paintFill.color = Color.rgb(241, 245, 249) // Slate 100
            canvas.drawRect(MARGIN_LEFT, cursorY, MARGIN_RIGHT, cursorY + 18f, paintFill)

            val vColDate = MARGIN_LEFT + 6f
            val vColBP = MARGIN_LEFT + 85f
            val vColStage = MARGIN_LEFT + 180f
            val vColPulse = MARGIN_LEFT + 280f
            val vColGlucose = MARGIN_LEFT + 340f
            val vColOther = MARGIN_LEFT + 430f

            paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paintText.textSize = 8.5f
            canvas.drawText("DATE & TIME", vColDate, cursorY + 12f, paintText)
            canvas.drawText("BP (SYS/DIA)", vColBP, cursorY + 12f, paintText)
            canvas.drawText("AHA STAGE (MAP)", vColStage, cursorY + 12f, paintText)
            canvas.drawText("PULSE", vColPulse, cursorY + 12f, paintText)
            canvas.drawText("GLUCOSE", vColGlucose, cursorY + 12f, paintText)
            canvas.drawText("SPO2 / KETONES", vColOther, cursorY + 12f, paintText)
            cursorY += 18f

            paintText.typeface = Typeface.DEFAULT
            paintText.textSize = 8f

            data.vitals.take(15).forEachIndexed { index, v ->
                checkPageBreak(20f)
                if (index % 2 == 1) {
                    paintFill.color = Color.rgb(248, 250, 252)
                    canvas.drawRect(MARGIN_LEFT, cursorY, MARGIN_RIGHT, cursorY + 16f, paintFill)
                }

                val dStr = v.date.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
                canvas.drawText(dStr, vColDate, cursorY + 11f, paintText)

                val bpStr = v.bloodPressure?.let { "${it.systolic}/${it.diastolic}" } ?: "—"
                canvas.drawText(bpStr, vColBP, cursorY + 11f, paintText)

                val stageStr = v.bloodPressure?.let {
                    val map = it.meanArterialPressure.toInt()
                    "${it.stage.label} (${map}m)"
                } ?: "—"
                canvas.drawText(stageStr, vColStage, cursorY + 11f, paintText)

                val pulseStr = v.bloodPressure?.pulse?.let { "$it bpm" } ?: "—"
                canvas.drawText(pulseStr, vColPulse, cursorY + 11f, paintText)

                val gluStr = v.bloodGlucose?.let {
                    "%.0f mg/dL (%s)".format(Locale.US, it.valueMgDl, it.context.name.take(4))
                } ?: "—"
                canvas.drawText(gluStr, vColGlucose, cursorY + 11f, paintText)

                val otherStr = buildString {
                    if (v.spo2Percent != null) append("${v.spo2Percent}% ")
                    if (v.ketonesMmolL != null) append("K:%.1f ".format(Locale.US, v.ketonesMmolL))
                    if (v.bodyTemperatureC != null) append("%.1f°C".format(Locale.US, v.bodyTemperatureC))
                    if (isEmpty()) append("—")
                }
                canvas.drawText(otherStr, vColOther, cursorY + 11f, paintText)

                canvas.drawLine(MARGIN_LEFT, cursorY + 16f, MARGIN_RIGHT, cursorY + 16f, paintStroke)
                cursorY += 16f
            }
            cursorY += 10f
        }

        // --- Section 2: Clinical Nutrition & Metabolic Balance ---
        checkPageBreak(90f)
        canvas.drawText("2. NUTRITIONAL INTAKE & DR. ARO CLINICAL AUDIT", MARGIN_LEFT, cursorY, paintSectionTitle)
        cursorY += 14f

        paintFill.color = Color.rgb(248, 250, 252)
        canvas.drawRoundRect(RectF(MARGIN_LEFT, cursorY, MARGIN_RIGHT, cursorY + 68f), 6f, 6f, paintFill)
        canvas.drawRoundRect(RectF(MARGIN_LEFT, cursorY, MARGIN_RIGHT, cursorY + 68f), 6f, 6f, paintStroke)

        val nCol1 = MARGIN_LEFT + 14f
        val nCol2 = MARGIN_LEFT + 150f
        val nCol3 = MARGIN_LEFT + 320f

        paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Daily Mean: ${data.averageCalories} kcal", nCol1, cursorY + 18f, paintText)
        paintText.typeface = Typeface.DEFAULT
        canvas.drawText("Protein: %.1f g".format(Locale.US, data.averageProtein), nCol1, cursorY + 34f, paintSecondary)
        canvas.drawText("Fiber: %.1f g".format(Locale.US, data.averageFiber), nCol1, cursorY + 50f, paintSecondary)

        canvas.drawText("Carbohydrates: %.1f g".format(Locale.US, data.averageCarbs), nCol2, cursorY + 34f, paintSecondary)
        canvas.drawText("Fats: %.1f g".format(Locale.US, data.averageFat), nCol2, cursorY + 50f, paintSecondary)

        val ratioText = data.molarNaKRatio?.let { "%.2f".format(Locale.US, it) } ?: "—"
        val naKStatus = when {
            data.molarNaKRatio == null -> ""
            data.molarNaKRatio <= 1.0 -> "(Optimal ≤ 1.0)"
            data.molarNaKRatio <= 1.5 -> "(Borderline)"
            else -> "(High Risk > 1.5)"
        }
        paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Molar Na:K Ratio: $ratioText $naKStatus", nCol3, cursorY + 18f, paintText)
        paintText.typeface = Typeface.DEFAULT
        canvas.drawText("Sodium: %.0f mg".format(Locale.US, data.sodiumMg ?: 0.0), nCol3, cursorY + 34f, paintSecondary)
        canvas.drawText("Potassium: %.0f mg".format(Locale.US, data.potassiumMg ?: 0.0), nCol3, cursorY + 50f, paintSecondary)

        cursorY += 80f

        if (data.clinicalFindings.isNotEmpty()) {
            checkPageBreak(50f)
            paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("Clinical Audit Findings:", MARGIN_LEFT, cursorY, paintText)
            paintText.typeface = Typeface.DEFAULT
            cursorY += 14f

            data.clinicalFindings.take(4).forEach { finding ->
                checkPageBreak(16f)
                canvas.drawText("• $finding", MARGIN_LEFT + 8f, cursorY, paintSecondary)
                cursorY += 13f
            }
            cursorY += 8f
        }

        // --- Section 3: Doctor's Consultation Records ---
        checkPageBreak(50f)
        canvas.drawText("3. CLINICIAN CONSULTATION & VISIT NOTES", MARGIN_LEFT, cursorY, paintSectionTitle)
        cursorY += 14f

        if (data.consultations.isEmpty()) {
            canvas.drawText("No doctor consultations logged during this period.", MARGIN_LEFT, cursorY + 12f, paintSecondary)
            cursorY += 28f
        } else {
            data.consultations.take(6).forEach { doc ->
                checkPageBreak(75f)

                paintFill.color = Color.rgb(248, 250, 252)
                canvas.drawRoundRect(RectF(MARGIN_LEFT, cursorY, MARGIN_RIGHT, cursorY + 68f), 6f, 6f, paintFill)
                canvas.drawRoundRect(RectF(MARGIN_LEFT, cursorY, MARGIN_RIGHT, cursorY + 68f), 6f, 6f, paintStroke)

                val cDateStr = doc.date.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
                paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paintText.textSize = 9.5f
                canvas.drawText("${doc.doctorName} (${doc.specialty}) • $cDateStr", MARGIN_LEFT + 10f, cursorY + 16f, paintText)
                paintText.typeface = Typeface.DEFAULT
                paintText.textSize = 8.5f

                canvas.drawText("Reason: ${doc.reasonForVisit}", MARGIN_LEFT + 10f, cursorY + 30f, paintSecondary)
                canvas.drawText("Assessment: ${doc.diagnosisNotes}", MARGIN_LEFT + 10f, cursorY + 42f, paintSecondary)

                val planStr = buildString {
                    if (doc.prescribedMedications.isNotBlank()) append("Rx: ${doc.prescribedMedications}  ")
                    if (doc.followUpPlan.isNotBlank()) append("• Plan: ${doc.followUpPlan}")
                }
                if (planStr.isNotBlank()) {
                    canvas.drawText(planStr.take(100), MARGIN_LEFT + 10f, cursorY + 56f, paintText)
                }

                cursorY += 76f
            }
        }

        // --- Section 4: Clinical Disclaimer Footer ---
        checkPageBreak(40f)
        paintFill.color = Color.rgb(241, 245, 249)
        canvas.drawRoundRect(RectF(MARGIN_LEFT, cursorY, MARGIN_RIGHT, cursorY + 32f), 4f, 4f, paintFill)
        val paintDisclaimer = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(100, 116, 139)
            textSize = 7.5f
        }
        canvas.drawText(
            "CLINICAL DISCLAIMER: Generated automatically by Aro Companion for informational health monitoring and doctor-patient review.",
            MARGIN_LEFT + 10f,
            cursorY + 13f,
            paintDisclaimer,
        )
        canvas.drawText(
            "This report is not a replacement for formal diagnosis, laboratory confirmation, or professional medical advice.",
            MARGIN_LEFT + 10f,
            cursorY + 23f,
            paintDisclaimer,
        )

        // Draw footer on final page
        drawFooter(canvas, pageNumber)
        document.finishPage(page)

        // Save to cache file
        val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
        val file = File(reportsDir, "aro_clinical_report_${System.currentTimeMillis()}.pdf")
        FileOutputStream(file).use { out ->
            document.writeTo(out)
        }
        document.close()
        return file
    }

    private fun drawFooter(canvas: Canvas, pageNumber: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(148, 163, 184)
            textSize = 8f
        }
        canvas.drawLine(MARGIN_LEFT, PAGE_HEIGHT - 35f, MARGIN_RIGHT, PAGE_HEIGHT - 35f, paint)
        canvas.drawText(
            "Aro Health Companion • 100% Offline Clinical Record",
            MARGIN_LEFT,
            PAGE_HEIGHT - 22f,
            paint,
        )
        val pageStr = "Page $pageNumber"
        val width = paint.measureText(pageStr)
        canvas.drawText(pageStr, MARGIN_RIGHT - width, PAGE_HEIGHT - 22f, paint)
    }

    fun shareReport(context: Context, pdfFile: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile,
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Aro Clinical Health Report")
            putExtra(Intent.EXTRA_TEXT, "Attached is the clinical health and vitals report generated from Aro Companion.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(shareIntent, "Share Clinical Report"))
    }
}
