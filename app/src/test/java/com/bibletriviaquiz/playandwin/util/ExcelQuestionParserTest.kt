package com.bibletriviaquiz.playandwin.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExcelQuestionParserTest {

    @Test
    fun parseDelimitedText_standardCsv_parsesQuestionsAccurately() {
        val csvData = """
Category,Question Text,Option A,Option B,Option C,Option D,Correct Option,Bible Reference,Explanation,Difficulty
Old Testament,"Who led the Israelites out of Egypt?",Moses,Aaron,Joshua,Caleb,A,Exodus 3:10,"God called Moses to deliver His people.",Easy
Miracles of Jesus,"What was the first miracle performed by Jesus?",Healing a blind man,Turning water into wine,Walking on water,Feeding 5000,B,John 2:1-11,"Performed at the wedding in Cana.",Easy
Women in the Bible,"Who was the queen that saved her people from destruction?",Esther,Ruth,Deborah,Sarah,Esther,Esther 4:14,"For such a time as this.",Medium
        """.trimIndent()

        val result = ExcelQuestionParser.parseDelimitedText(csvData)

        assertEquals(3, result.questions.size)
        assertEquals(0, result.errors.size)

        // Question 1
        val q1 = result.questions[0]
        assertEquals("Old Testament", q1.category)
        assertEquals("Who led the Israelites out of Egypt?", q1.questionText)
        assertEquals(4, q1.options.size)
        assertEquals(0L, q1.correctOptionIndex) // A -> 0
        assertEquals("Exodus 3:10", q1.bibleReference)

        // Question 2
        val q2 = result.questions[1]
        assertEquals("Miracles of Jesus", q2.category)
        assertEquals(1L, q2.correctOptionIndex) // B -> 1
        assertEquals("John 2:1-11", q2.bibleReference)

        // Question 3 (Correct option given as full text "Esther")
        val q3 = result.questions[2]
        assertEquals("Women in the Bible", q3.category)
        assertEquals(0L, q3.correctOptionIndex) // "Esther" matches Option A
        assertEquals("Esther 4:14", q3.bibleReference)
        assertEquals("Medium", q3.difficulty)
    }

    @Test
    fun parseDelimitedText_withCategoryOverride_routesAllQuestionsToTargetCategory() {
        val tsvData = """
Question Text	Option A	Option B	Option C	Option D	Correct Option	Bible Reference
Who was the first king of Israel?	David	Saul	Solomon	Rehoboam	B	1 Samuel 9
Who defeated Goliath?	David	Saul	Jonathan	Abner	1	1 Samuel 17
        """.trimIndent()

        val targetCategory = "Kings and Prophets"
        val result = ExcelQuestionParser.parseDelimitedText(tsvData, defaultCategory = targetCategory)

        assertEquals(2, result.questions.size)
        assertTrue(result.questions.all { it.category == targetCategory })
        assertEquals(1L, result.questions[0].correctOptionIndex) // B -> 1
        assertEquals(0L, result.questions[1].correctOptionIndex) // 1 -> 0
    }

    @Test
    fun parseDelimitedText_skipsInvalidRowsAndReportsErrors() {
        val corruptedData = """
Category,Question Text,Option A,Option B,Option C,Option D,Correct Option
Old Testament,Who?,A,B,C,D,A
Old Testament,Valid Question Text Here?,,Opt 2,Opt 3,Opt 4,B
Old Testament,Another Valid Question Here?,One,Two,Three,Four,A
        """.trimIndent()

        val result = ExcelQuestionParser.parseDelimitedText(corruptedData)

        // Only 1 fully valid question (at least 5 chars, Option A and B present)
        assertEquals(1, result.questions.size)
        assertEquals(2, result.errors.size)
    }

    @Test
    fun getSampleCsvTemplate_providesValidCsvHeaderAndRows() {
        val template = ExcelQuestionParser.getSampleCsvTemplate()
        assertNotNull(template)
        assertTrue(template.contains("Category"))
        assertTrue(template.contains("Question Text"))
        assertTrue(template.contains("Option A"))
        assertTrue(template.contains("Option B"))
        assertTrue(template.contains("Correct Option"))

        // Parsing the template should yield the example questions without errors
        val result = ExcelQuestionParser.parseDelimitedText(template)
        assertTrue(result.questions.isNotEmpty())
        assertEquals(0, result.errors.size)
    }
}
