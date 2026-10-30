package com.bibletriviaquiz.playandwin.util

import android.content.Context
import android.net.Uri
import android.util.Xml
import com.bibletriviaquiz.playandwin.data.repository.DefaultBibleQuestions
import com.bibletriviaquiz.playandwin.model.Question
import org.xmlpull.v1.XmlPullParser
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

data class ParseResult(
    val questions: List<Question>,
    val errors: List<String>,
    val totalRowsProcessed: Int,
    val categoryCounts: Map<String, Int>
)

object ExcelQuestionParser {

    /**
     * Parses an Excel (.xlsx) or CSV file from a Content Uri.
     */
    fun parseFromUri(context: Context, uri: Uri, defaultCategory: String? = null): ParseResult {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: return ParseResult(emptyList(), listOf("Could not open file from storage."), 0, emptyMap())

        return inputStream.use { stream ->
            val bytes = stream.readBytes()
            parseBytes(bytes, defaultCategory)
        }
    }

    /**
     * Parses Excel (.xlsx) or CSV/TSV from raw byte array.
     */
    fun parseBytes(bytes: ByteArray, defaultCategory: String? = null): ParseResult {
        if (bytes.isEmpty()) {
            return ParseResult(emptyList(), listOf("The selected file is empty."), 0, emptyMap())
        }

        // Check for ZIP magic bytes (PK\x03\x04 = 0x50, 0x4B, 0x03, 0x04)
        val isZipXlsx = bytes.size >= 4 &&
                bytes[0] == 0x50.toByte() &&
                bytes[1] == 0x4B.toByte() &&
                bytes[2] == 0x03.toByte() &&
                bytes[3] == 0x04.toByte()

        return if (isZipXlsx) {
            parseXlsxBytes(bytes, defaultCategory)
        } else {
            // Treat as CSV / TSV text
            val text = String(bytes, Charsets.UTF_8)
            parseDelimitedText(text, defaultCategory)
        }
    }

    /**
     * Parses standard Office Open XML (.xlsx) workbook.
     */
    private fun parseXlsxBytes(bytes: ByteArray, defaultCategory: String?): ParseResult {
        val sharedStrings = mutableListOf<String>()
        var sheetBytes: ByteArray? = null

        try {
            // First pass: extract shared strings and find first worksheet
            ZipInputStream(ByteArrayInputStream(bytes)).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                while (entry != null) {
                    val name = entry.name.lowercase()
                    if (name.contains("sharedstrings.xml")) {
                        val ssBytes = zis.readBytes()
                        sharedStrings.addAll(parseSharedStrings(ssBytes))
                    } else if (sheetBytes == null && (name.contains("worksheets/sheet1.xml") || name.contains("worksheets/sheet"))) {
                        sheetBytes = zis.readBytes()
                    }
                    entry = zis.nextEntry
                }
            }

            if (sheetBytes == null) {
                return ParseResult(emptyList(), listOf("Could not find worksheet in Excel file."), 0, emptyMap())
            }

            // Second pass: parse the sheet into rows of cell values
            val rows = parseSheetXml(sheetBytes!!, sharedStrings)
            return mapTableToQuestions(rows, defaultCategory)
        } catch (e: Exception) {
            return ParseResult(emptyList(), listOf("Failed to parse Excel file: ${e.message}"), 0, emptyMap())
        }
    }

    private fun parseSharedStrings(bytes: ByteArray): List<String> {
        val strings = mutableListOf<String>()
        val parser: XmlPullParser = Xml.newPullParser()
        parser.setInput(ByteArrayInputStream(bytes), "UTF-8")

        var eventType = parser.eventType
        var currentText = StringBuilder()
        var insideT = false

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    if (parser.name.equals("t", ignoreCase = true)) {
                        insideT = true
                        currentText.clear()
                    } else if (parser.name.equals("si", ignoreCase = true)) {
                        currentText.clear()
                    }
                }
                XmlPullParser.TEXT -> {
                    if (insideT) {
                        currentText.append(parser.text)
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name.equals("t", ignoreCase = true)) {
                        insideT = false
                    } else if (parser.name.equals("si", ignoreCase = true)) {
                        strings.add(currentText.toString())
                    }
                }
            }
            eventType = parser.next()
        }
        return strings
    }

    private fun parseSheetXml(bytes: ByteArray, sharedStrings: List<String>): List<List<String>> {
        val rows = mutableListOf<MutableList<String>>()
        var currentRow: MutableList<String>? = null
        var currentCellCol = 0
        var currentCellType = ""
        var insideV = false
        var cellValue = StringBuilder()

        val parser: XmlPullParser = Xml.newPullParser()
        parser.setInput(ByteArrayInputStream(bytes), "UTF-8")

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    val name = parser.name
                    if (name.equals("row", ignoreCase = true)) {
                        currentRow = mutableListOf()
                        currentCellCol = 0
                    } else if (name.equals("c", ignoreCase = true)) {
                        currentCellType = parser.getAttributeValue(null, "t") ?: ""
                        val ref = parser.getAttributeValue(null, "r") ?: ""
                        val colIdx = columnRefToIndex(ref)
                        if (currentRow != null) {
                            while (currentRow.size < colIdx) {
                                currentRow.add("")
                            }
                        }
                        cellValue.clear()
                    } else if (name.equals("v", ignoreCase = true) || name.equals("t", ignoreCase = true)) {
                        insideV = true
                    }
                }
                XmlPullParser.TEXT -> {
                    if (insideV) {
                        cellValue.append(parser.text)
                    }
                }
                XmlPullParser.END_TAG -> {
                    val name = parser.name
                    if (name.equals("v", ignoreCase = true) || name.equals("t", ignoreCase = true)) {
                        insideV = false
                    } else if (name.equals("c", ignoreCase = true)) {
                        val raw = cellValue.toString().trim()
                        val finalValue = if (currentCellType == "s") {
                            val idx = raw.toIntOrNull()
                            if (idx != null && idx in sharedStrings.indices) sharedStrings[idx] else raw
                        } else {
                            raw
                        }
                        currentRow?.add(finalValue)
                    } else if (name.equals("row", ignoreCase = true)) {
                        if (currentRow != null && currentRow.any { it.isNotBlank() }) {
                            rows.add(currentRow)
                        }
                    }
                }
            }
            eventType = parser.next()
        }
        return rows
    }

    private fun columnRefToIndex(cellRef: String): Int {
        val colLetters = cellRef.takeWhile { it.isLetter() }.uppercase()
        if (colLetters.isEmpty()) return 0
        var col = 0
        for (char in colLetters) {
            col = col * 26 + (char - 'A' + 1)
        }
        return (col - 1).coerceAtLeast(0)
    }

    /**
     * Parses standard CSV or TSV text.
     */
    fun parseDelimitedText(text: String, defaultCategory: String? = null): ParseResult {
        val lines = text.lines()
        val delimiter = if (lines.firstOrNull()?.contains("\t") == true) '\t' else ','
        val rows = mutableListOf<List<String>>()

        for (line in lines) {
            if (line.isBlank()) continue
            val row = parseCsvLine(line, delimiter)
            if (row.any { it.isNotBlank() }) {
                rows.add(row)
            }
        }

        return mapTableToQuestions(rows, defaultCategory)
    }

    private fun parseCsvLine(line: String, delimiter: Char): List<String> {
        val values = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false

        for (i in line.indices) {
            val c = line[i]
            if (c == '"') {
                inQuotes = !inQuotes
            } else if (c == delimiter && !inQuotes) {
                values.add(sb.toString().trim())
                sb.clear()
            } else {
                sb.append(c)
            }
        }
        values.add(sb.toString().trim())
        return values.map { it.removePrefix("\"").removeSuffix("\"").trim() }
    }

    /**
     * Maps extracted 2D spreadsheet rows into verified Question models.
     */
    private fun mapTableToQuestions(rows: List<List<String>>, defaultCategory: String?): ParseResult {
        if (rows.isEmpty()) {
            return ParseResult(emptyList(), listOf("No data rows found in spreadsheet."), 0, emptyMap())
        }

        val headerRow = rows.first()
        val headers = headerRow.map { it.trim().lowercase() }

        // Find column indices
        var catCol = headers.indexOfFirst { it.contains("category") || it.contains("cat") }
        var qCol = headers.indexOfFirst { it.contains("question") || it.contains("prompt") || it == "q" }
        var optACol = headers.indexOfFirst { it.contains("option a") || it == "a" || it.contains("opt a") || it == "option 1" || it == "choice a" }
        var optBCol = headers.indexOfFirst { it.contains("option b") || it == "b" || it.contains("opt b") || it == "option 2" || it == "choice b" }
        var optCCol = headers.indexOfFirst { it.contains("option c") || it == "c" || it.contains("opt c") || it == "option 3" || it == "choice c" }
        var optDCol = headers.indexOfFirst { it.contains("option d") || it == "d" || it.contains("opt d") || it == "option 4" || it == "choice d" }
        var correctCol = headers.indexOfFirst { it.contains("correct") || it.contains("answer") || it == "ans" }
        var refCol = headers.indexOfFirst { it.contains("reference") || it.contains("ref") || it.contains("bible") || it.contains("verse") }
        var expCol = headers.indexOfFirst { it.contains("explanation") || it.contains("explain") || it.contains("note") }
        var diffCol = headers.indexOfFirst { it.contains("difficulty") || it.contains("level") }

        // Positional fallback if no clear header keywords match
        val hasHeaders = qCol >= 0 || optACol >= 0 || correctCol >= 0
        val dataRows = if (hasHeaders) rows.drop(1) else rows

        if (!hasHeaders) {
            // Assume default standard column order:
            // Col 0: Category, Col 1: Question, Col 2: Opt A, Col 3: Opt B, Col 4: Opt C, Col 5: Opt D, Col 6: Correct, Col 7: Ref
            catCol = 0
            qCol = 1
            optACol = 2
            optBCol = 3
            optCCol = 4
            optDCol = 5
            correctCol = 6
            refCol = 7
        } else {
            // If question column wasn't explicitly named, pick first column with substantial text
            if (qCol < 0) qCol = 0
            if (optACol < 0) optACol = 1
            if (optBCol < 0) optBCol = 2
            if (optCCol < 0) optCCol = 3
            if (optDCol < 0) optDCol = 4
            if (correctCol < 0) correctCol = 5
        }

        val questions = mutableListOf<Question>()
        val errors = mutableListOf<String>()
        val categoryCounts = mutableMapOf<String, Int>()

        dataRows.forEachIndexed { rowIdx, row ->
            val rowNumber = if (hasHeaders) rowIdx + 2 else rowIdx + 1

            val qText = row.getOrNull(qCol)?.trim() ?: ""
            if (qText.isBlank() || qText.length < 5) {
                errors.add("Row $rowNumber: Question text is missing or too short (min 5 characters).")
                return@forEachIndexed
            }

            val optA = row.getOrNull(optACol)?.trim() ?: ""
            val optB = row.getOrNull(optBCol)?.trim() ?: ""
            val optC = row.getOrNull(optCCol)?.trim() ?: ""
            val optD = row.getOrNull(optDCol)?.trim() ?: ""

            if (optA.isBlank() || optB.isBlank()) {
                errors.add("Row $rowNumber: Option A and Option B must not be empty.")
                return@forEachIndexed
            }

            val finalOptC = if (optC.isNotBlank()) optC else "None of the above"
            val finalOptD = if (optD.isNotBlank()) optD else "All of the above"
            val optionsList = listOf(optA, optB, finalOptC, finalOptD)

            // Determine correct option index (0..3)
            val correctRaw = row.getOrNull(correctCol)?.trim() ?: "A"
            val correctIdx = parseCorrectIndex(correctRaw, optionsList)

            // Determine category
            val rawCategory = if (catCol >= 0) row.getOrNull(catCol)?.trim() else null
            val category = normalizeCategory(rawCategory, defaultCategory)

            val reference = if (refCol >= 0) row.getOrNull(refCol)?.trim()?.ifBlank { null } else null
            val explanation = if (expCol >= 0) row.getOrNull(expCol)?.trim()?.ifBlank { null } else null
            val difficulty = if (diffCol >= 0) row.getOrNull(diffCol)?.trim()?.ifBlank { "Medium" } ?: "Medium" else "Medium"

            val qId = "q_${UUID.randomUUID().toString().replace("-", "").take(14)}"
            val question = Question(
                id = qId,
                questionId = qId,
                questionText = qText,
                options = optionsList,
                correctOptionIndex = correctIdx.toLong(),
                category = category,
                difficulty = difficulty,
                bibleReference = reference,
                explanation = explanation,
                isPublished = true
            )
            questions.add(question)
            categoryCounts[category] = (categoryCounts[category] ?: 0) + 1
        }

        return ParseResult(
            questions = questions,
            errors = errors,
            totalRowsProcessed = dataRows.size,
            categoryCounts = categoryCounts
        )
    }

    private fun parseCorrectIndex(raw: String, options: List<String>): Int {
        val trimmed = raw.trim().uppercase()
        when (trimmed) {
            "A", "1", "OPTION A", "FIRST" -> return 0
            "B", "2", "OPTION B", "SECOND" -> return 1
            "C", "3", "OPTION C", "THIRD" -> return 2
            "D", "4", "OPTION D", "FOURTH" -> return 3
        }

        val idx = trimmed.toIntOrNull()
        if (idx != null && idx in 1..4) return idx - 1
        if (idx != null && idx in 0..3) return idx

        // Check if raw matches any of the option texts exactly
        val matchIdx = options.indexOfFirst { it.equals(raw.trim(), ignoreCase = true) }
        if (matchIdx >= 0) return matchIdx

        return 0 // default to A
    }

    private fun normalizeCategory(categoryStr: String?, defaultCategory: String?): String {
        if (!categoryStr.isNullOrBlank()) {
            val matched = DefaultBibleQuestions.categories.firstOrNull {
                it.equals(categoryStr.trim(), ignoreCase = true) ||
                        it.contains(categoryStr.trim(), ignoreCase = true)
            }
            if (matched != null) return matched
            return categoryStr.trim()
        }

        if (!defaultCategory.isNullOrBlank()) {
            return defaultCategory.trim()
        }

        return "Old Testament"
    }

    /**
     * Generates a sample CSV template string that admins can copy or download.
     */
    fun getSampleCsvTemplate(): String {
        return "Category,Question Text,Option A,Option B,Option C,Option D,Correct Option,Bible Reference,Explanation,Difficulty\n" +
                "Old Testament,\"Who led the Israelites out of Egypt?\",Moses,Aaron,Joshua,Gideon,A,Exodus 3:10,\"God called Moses at the burning bush.\",Easy\n" +
                "New Testament,\"In what city was Jesus Christ born?\",Nazareth,Bethlehem,Jerusalem,Jericho,B,Luke 2:4-7,\"Jesus was born in Bethlehem of Judea.\",Easy\n" +
                "Miracles of Jesus,\"How many loaves and fish fed the 5,000?\",5 loaves and 2 fish,7 loaves and 3 fish,2 loaves and 5 fish,10 loaves and 2 fish,A,Matthew 14:17-21,\"A young boy had five loaves and two fish.\",Easy\n" +
                "Bible Characters,\"Who was swallowed by a great fish?\",Daniel,Jonah,Elijah,Ezekiel,B,Jonah 1:17,\"The Lord provided a huge fish to swallow Jonah.\",Easy\n"
    }
}
