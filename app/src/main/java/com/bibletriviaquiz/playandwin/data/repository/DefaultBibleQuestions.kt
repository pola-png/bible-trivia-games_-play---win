package com.bibletriviaquiz.playandwin.data.repository

import com.bibletriviaquiz.playandwin.model.Question

object DefaultBibleQuestions {
    val categories = listOf(
        "Old Testament",
        "New Testament",
        "Bible Characters",
        "Miracles of Jesus",
        "Life and Teachings of Jesus",
        "Books of the Bible",
        "Bible History",
        "Women in the Bible",
        "Kings and Prophets",
        "Bible Verses and References",
        "Parables",
        "Apostles and Early Church",
        "Easy Bible Questions",
        "Intermediate Bible Questions",
        "Advanced Bible Questions"
    )

    val questions: List<Question> = emptyList()
}
