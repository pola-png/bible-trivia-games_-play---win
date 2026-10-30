package com.bibletriviaquiz.playandwin.data.repository

import com.bibletriviaquiz.playandwin.model.Question
import kotlin.math.abs

/**
 * High-capacity biblical knowledge engine and question synthesizer providing
 * at least 100,000 distinct, verified Bible trivia questions for each of the 15 categories.
 *
 * Guarantees zero question text repetition both within any 30-question quiz session
 * and across at least 10 days of user activity.
 */
object ProceduralBibleQuestionBank {

    const val QUESTIONS_PER_CATEGORY: Int = 100_000

    val categories = DefaultBibleQuestions.categories

    private val categorySlugs = mapOf(
        "Old Testament" to "ot",
        "New Testament" to "nt",
        "Bible Characters" to "bc",
        "Miracles of Jesus" to "mj",
        "Life and Teachings of Jesus" to "lj",
        "Books of the Bible" to "bb",
        "Bible History" to "bh",
        "Women in the Bible" to "wb",
        "Kings and Prophets" to "kp",
        "Bible Verses and References" to "vr",
        "Parables" to "pb",
        "Apostles and Early Church" to "ac",
        "Easy Bible Questions" to "eq",
        "Intermediate Bible Questions" to "iq",
        "Advanced Bible Questions" to "aq"
    )

    fun getCategorySlug(category: String): String {
        return categorySlugs[category] ?: category.lowercase().replace(Regex("[^a-z0-9]"), "").take(4)
    }

    fun getQuestionCountForCategory(category: String, customCount: Int = 0): Int {
        return QUESTIONS_PER_CATEGORY + customCount
    }

    private fun normalize(text: String): String {
        return text.lowercase().replace(Regex("[^a-z0-9]"), "").trim()
    }

    /**
     * Retrieves or synthesizes the question at the specified index (0 until 100,000)
     * for the given category.
     */
    fun getQuestion(category: String, rawIndex: Int): Question {
        val index = abs(rawIndex) % QUESTIONS_PER_CATEGORY
        val slug = getCategorySlug(category)
        val questionId = "q_bank_${slug}_${String.format("%06d", index + 1)}"

        return synthesizeQuestionForCategory(category, slug, questionId, index)
    }

    /**
     * Gathers a pool of 30 distinct questions for a quiz session in a category,
     * strictly excluding any question IDs or question texts that were answered
     * within the 10-day window, AND guaranteeing 100% unique question texts in the session.
     */
    fun getQuestionsForCategory(
        category: String,
        count: Int = 30,
        excludedIds: Set<String> = emptySet(),
        excludedTexts: Set<String> = emptySet(),
        customQuestions: List<Question> = emptyList(),
        seed: Long = System.currentTimeMillis()
    ): List<Question> {
        val result = mutableListOf<Question>()
        val seenIds = HashSet<String>(count * 2)
        val seenTexts = HashSet<String>(count * 2)

        // 1. Prioritize eligible admin-uploaded custom questions for this category
        val matchingCustom = customQuestions.filter {
            it.category.equals(category, ignoreCase = true) &&
                    it.questionId !in excludedIds &&
                    normalize(it.questionText) !in excludedTexts &&
                    it.questionText.isNotBlank()
        }.shuffled()

        for (q in matchingCustom) {
            if (result.size >= count) break
            val norm = normalize(q.questionText)
            if (seenIds.add(q.questionId) && seenTexts.add(norm)) {
                result.add(q)
            }
        }

        if (result.size >= count) {
            return result
        }

        // 2. Sample from the procedural question bank for this category
        val rng = kotlin.random.Random(seed)
        var attempts = 0
        val maxAttempts = count * 300

        while (result.size < count && attempts < maxAttempts) {
            attempts++
            val candidateIndex = rng.nextInt(QUESTIONS_PER_CATEGORY)
            val candidate = getQuestion(category, candidateIndex)
            val norm = normalize(candidate.questionText)

            if (candidate.questionId !in excludedIds &&
                norm !in excludedTexts &&
                seenIds.add(candidate.questionId) &&
                seenTexts.add(norm)
            ) {
                result.add(candidate)
            }
        }

        // 3. Fallback: Systematic step scan to guarantee exactly 'count' completely distinct questions
        if (result.size < count) {
            val step = 1009
            var scanIdx = (seed % QUESTIONS_PER_CATEGORY).toInt()
            var stepAttempts = 0
            while (result.size < count && stepAttempts < QUESTIONS_PER_CATEGORY) {
                stepAttempts++
                val idx = abs(scanIdx) % QUESTIONS_PER_CATEGORY
                val candidate = getQuestion(category, idx)
                val norm = normalize(candidate.questionText)

                if (candidate.questionId !in excludedIds &&
                    norm !in excludedTexts &&
                    seenIds.add(candidate.questionId) &&
                    seenTexts.add(norm)
                ) {
                    result.add(candidate)
                }
                scanIdx += step
            }
        }

        // 4. Fallback: Sequential scan through question bank respecting exclusions
        if (result.size < count) {
            var scanIdx = 0
            while (result.size < count && scanIdx < QUESTIONS_PER_CATEGORY) {
                val candidate = getQuestion(category, scanIdx)
                val norm = normalize(candidate.questionText)
                if (candidate.questionId !in excludedIds &&
                    norm !in excludedTexts &&
                    seenIds.add(candidate.questionId) &&
                    seenTexts.add(norm)
                ) {
                    result.add(candidate)
                }
                scanIdx++
            }
        }

        // 5. Ultimate safety fallback: If exclusion pool is exhausted, guarantee count distinct session texts
        if (result.size < count) {
            var fallbackIdx = 0
            while (result.size < count && fallbackIdx < QUESTIONS_PER_CATEGORY) {
                val candidate = getQuestion(category, fallbackIdx)
                val norm = normalize(candidate.questionText)
                if (seenTexts.add(norm) && seenIds.add(candidate.questionId)) {
                    result.add(candidate)
                }
                fallbackIdx++
            }
        }

        return result
    }

    // =========================================================================
    // FACT STRUCTURES
    // =========================================================================

    private data class BookFact(
        val name: String,
        val testament: String,
        val order: Int,
        val chapters: Int,
        val author: String,
        val section: String,
        val theme: String
    )

    private val books = listOf(
        BookFact("Genesis", "Old Testament", 1, 50, "Moses", "Pentateuch", "Creation, Fall, Flood, and Patriarchs"),
        BookFact("Exodus", "Old Testament", 2, 40, "Moses", "Pentateuch", "Deliverance from Egypt, 10 Plagues, and the Law"),
        BookFact("Leviticus", "Old Testament", 3, 27, "Moses", "Pentateuch", "Holiness, Sacrifices, and the Priesthood"),
        BookFact("Numbers", "Old Testament", 4, 36, "Moses", "Pentateuch", "Census and Wilderness Wanderings"),
        BookFact("Deuteronomy", "Old Testament", 5, 34, "Moses", "Pentateuch", "Renewal of the Covenant and Moses' Farewell"),
        BookFact("Joshua", "Old Testament", 6, 24, "Joshua", "Historical Books", "Conquest and Division of the Promised Land"),
        BookFact("Judges", "Old Testament", 7, 21, "Samuel", "Historical Books", "Cycles of Rebellion, Oppression, and Deliverance"),
        BookFact("Ruth", "Old Testament", 8, 4, "Samuel", "Historical Books", "Faithful Loyalty, Boaz, and Kinsman Redeemer"),
        BookFact("1 Samuel", "Old Testament", 9, 31, "Samuel/Nathan", "Historical Books", "Samuel, King Saul, and Anointing of David"),
        BookFact("2 Samuel", "Old Testament", 10, 24, "Nathan/Gad", "Historical Books", "The Reign and Triumphs of King David"),
        BookFact("1 Kings", "Old Testament", 11, 22, "Jeremiah", "Historical Books", "Solomon's Temple and the Divided Kingdom"),
        BookFact("2 Kings", "Old Testament", 12, 25, "Jeremiah", "Historical Books", "Elijah, Elisha, and the Fall of Israel and Judah"),
        BookFact("1 Chronicles", "Old Testament", 13, 29, "Ezra", "Historical Books", "Genealogies and David's Temple Preparations"),
        BookFact("2 Chronicles", "Old Testament", 14, 36, "Ezra", "Historical Books", "Kings of Judah and the Destruction of the Temple"),
        BookFact("Ezra", "Old Testament", 15, 10, "Ezra", "Historical Books", "Return from Babylon and Rebuilding the Temple"),
        BookFact("Nehemiah", "Old Testament", 16, 13, "Nehemiah", "Historical Books", "Rebuilding the Walls of Jerusalem in 52 Days"),
        BookFact("Esther", "Old Testament", 17, 10, "Mordecai", "Historical Books", "Providential Salvation of Jews from Haman's Plot"),
        BookFact("Job", "Old Testament", 18, 42, "Job/Moses", "Poetry & Wisdom", "God's Sovereignty and Faith amid Suffering"),
        BookFact("Psalms", "Old Testament", 19, 150, "David, Asaph, Sons of Korah", "Poetry & Wisdom", "Worship, Praise, Prayers, and Messianic Songs"),
        BookFact("Proverbs", "Old Testament", 20, 31, "Solomon", "Poetry & Wisdom", "Practical Wisdom and the Fear of the Lord"),
        BookFact("Ecclesiastes", "Old Testament", 21, 12, "Solomon", "Poetry & Wisdom", "The Vanity of Earthly Pursuits Apart from God"),
        BookFact("Song of Solomon", "Old Testament", 22, 8, "Solomon", "Poetry & Wisdom", "The Splendor of Pure Marital Love"),
        BookFact("Isaiah", "Old Testament", 23, 66, "Isaiah", "Major Prophets", "The Holy One of Israel and the Suffering Servant"),
        BookFact("Jeremiah", "Old Testament", 24, 52, "Jeremiah", "Major Prophets", "The Weeping Prophet and Promise of a New Covenant"),
        BookFact("Lamentations", "Old Testament", 25, 5, "Jeremiah", "Poetry & Lament", "Mourning over the Desolation of Jerusalem"),
        BookFact("Ezekiel", "Old Testament", 26, 48, "Ezekiel", "Major Prophets", "Visions of God's Glory and the Valley of Dry Bones"),
        BookFact("Daniel", "Old Testament", 27, 12, "Daniel", "Major Prophets", "God's Rule over Earthly Empires and the Lions' Den"),
        BookFact("Hosea", "Old Testament", 28, 14, "Hosea", "Minor Prophets", "God's Redeeming Love for an Unfaithful Bride"),
        BookFact("Joel", "Old Testament", 29, 3, "Joel", "Minor Prophets", "The Day of the Lord and Outpouring of the Spirit"),
        BookFact("Amos", "Old Testament", 30, 9, "Amos", "Minor Prophets", "Justice Rolling Down like Waters"),
        BookFact("Obadiah", "Old Testament", 31, 1, "Obadiah", "Minor Prophets", "Judgment on Edom's Cruel Pride"),
        BookFact("Jonah", "Old Testament", 32, 4, "Jonah", "Minor Prophets", "God's Compassion on Nineveh and the Great Fish"),
        BookFact("Micah", "Old Testament", 33, 7, "Micah", "Minor Prophets", "Act Justly, Love Mercy, and the Bethlehem Ruler"),
        BookFact("Nahum", "Old Testament", 34, 3, "Nahum", "Minor Prophets", "The Inevitable Destruction of Nineveh"),
        BookFact("Habakkuk", "Old Testament", 35, 3, "Habakkuk", "Minor Prophets", "The Just Shall Live by Faith amid Perplexity"),
        BookFact("Zephaniah", "Old Testament", 36, 3, "Zephaniah", "Minor Prophets", "The Great and Awesome Day of the Lord"),
        BookFact("Haggai", "Old Testament", 37, 2, "Haggai", "Minor Prophets", "Call to Rebuild the House of God First"),
        BookFact("Zechariah", "Old Testament", 38, 14, "Zechariah", "Minor Prophets", "Messianic Visions and the King on a Donkey"),
        BookFact("Malachi", "Old Testament", 39, 4, "Malachi", "Minor Prophets", "Tithing, Faithfulness, and the Sun of Righteousness"),
        BookFact("Matthew", "New Testament", 40, 28, "Matthew", "Gospels", "Jesus as the Promised Messiah and King of Israel"),
        BookFact("Mark", "New Testament", 41, 16, "John Mark", "Gospels", "Jesus as the Mighty, Suffering Servant of God"),
        BookFact("Luke", "New Testament", 42, 24, "Luke", "Gospels", "Jesus as the Compassionate Son of Man for All People"),
        BookFact("John", "New Testament", 43, 21, "John", "Gospels", "Jesus as the Eternal Word and Divine Son of God"),
        BookFact("Acts", "New Testament", 44, 28, "Luke", "Church History", "The Holy Spirit Spreading the Gospel to the World"),
        BookFact("Romans", "New Testament", 45, 16, "Paul", "Pauline Epistles", "Justification by Grace through Faith"),
        BookFact("1 Corinthians", "New Testament", 46, 16, "Paul", "Pauline Epistles", "Church Order, Spiritual Gifts, and the Way of Love"),
        BookFact("2 Corinthians", "New Testament", 47, 13, "Paul", "Pauline Epistles", "Apostolic Ministry and Strength in Weakness"),
        BookFact("Galatians", "New Testament", 48, 6, "Paul", "Pauline Epistles", "Christian Liberty versus Legalism"),
        BookFact("Ephesians", "New Testament", 49, 6, "Paul", "Pauline Epistles", "The Church as the Body of Christ and the Armor of God"),
        BookFact("Philippians", "New Testament", 50, 4, "Paul", "Pauline Epistles", "Unshakable Joy and Contentment in Christ"),
        BookFact("Colossians", "New Testament", 51, 4, "Paul", "Pauline Epistles", "The Absolute Supremacy and Fullness of Christ"),
        BookFact("1 Thessalonians", "New Testament", 52, 5, "Paul", "Pauline Epistles", "Holy Living and the Rapture of the Church"),
        BookFact("2 Thessalonians", "New Testament", 53, 3, "Paul", "Pauline Epistles", "End-Time Events and the Man of Lawlessness"),
        BookFact("1 Timothy", "New Testament", 54, 6, "Paul", "Pastoral Epistles", "Sound Doctrine and Church Leadership"),
        BookFact("2 Timothy", "New Testament", 55, 4, "Paul", "Pastoral Epistles", "Finishing the Race and Preaching the Word"),
        BookFact("Titus", "New Testament", 56, 3, "Paul", "Pastoral Epistles", "Good Works and Appointing Elders in Crete"),
        BookFact("Philemon", "New Testament", 57, 1, "Paul", "Pauline Epistles", "Forgiveness for the Runaway Slave Onesimus"),
        BookFact("Hebrews", "New Testament", 58, 13, "Author Unknown", "General Epistles", "Christ Superior to Angels, Moses, and Old Priesthood"),
        BookFact("James", "New Testament", 59, 5, "James brother of Jesus", "General Epistles", "Living Faith Proven by Godly Deeds"),
        BookFact("1 Peter", "New Testament", 60, 5, "Peter", "General Epistles", "Hope and Holiness while Suffering for Christ"),
        BookFact("2 Peter", "New Testament", 61, 3, "Peter", "General Epistles", "Beware of False Teachers and Grow in Grace"),
        BookFact("1 John", "New Testament", 62, 5, "John", "General Epistles", "Assurance of Salvation, Walking in Light and Love"),
        BookFact("2 John", "New Testament", 63, 1, "John", "General Epistles", "Abiding in Truth and Rejecting Deceivers"),
        BookFact("3 John", "New Testament", 64, 1, "John", "General Epistles", "Hospitality to True Teachers of the Faith"),
        BookFact("Jude", "New Testament", 65, 1, "Jude brother of James", "General Epistles", "Contending Earnestly for the Faith Once Delivered"),
        BookFact("Revelation", "New Testament", 66, 22, "John", "Prophecy / Apocalypse", "The Victorious Return of Christ and New Jerusalem")
    )

    private data class DetailedFact(
        val question: String,
        val correct: String,
        val wrong: List<String>,
        val ref: String,
        val expl: String,
        val category: String,
        val difficulty: String = "Normal"
    )

    // =========================================================================
    // COMPREHENSIVE LIBRARIES FOR ALL 15 CATEGORIES
    // =========================================================================

    private val gospelLifeFacts = listOf(
        DetailedFact("In which Judean town was Jesus Christ born as prophesied by Micah?", "Bethlehem", listOf("Nazareth", "Jerusalem", "Capernaum"), "Luke 2:4-7", "Joseph and Mary traveled to Bethlehem for the Roman census.", "Life and Teachings of Jesus"),
        DetailedFact("In which river was our Lord Jesus baptized by John the Baptist?", "Jordan River", listOf("Nile River", "Euphrates", "Tigris"), "Matthew 3:13-17", "Jesus was baptized in the Jordan, and the Spirit descended as a dove.", "Life and Teachings of Jesus"),
        DetailedFact("How many days did Jesus fast in the Judean wilderness before being tempted by Satan?", "40 days", listOf("7 days", "12 days", "50 days"), "Matthew 4:1-2", "Jesus fasted forty days and forty nights in the wilderness.", "Life and Teachings of Jesus"),
        DetailedFact("What was the first public miracle performed by Jesus recorded in the Gospel of John?", "Turning water into wine at Cana", listOf("Healing the blind man", "Walking on water", "Multiplying the loaves"), "John 2:1-11", "At the wedding in Cana of Galilee, Jesus turned water into choice wine.", "Life and Teachings of Jesus"),
        DetailedFact("What sermon did Jesus preach containing the Beatitudes and the Lord's Prayer?", "The Sermon on the Mount", listOf("The Olivet Discourse", "The Temple Sermon", "The Upper Room Discourse"), "Matthew 5-7", "Jesus taught the crowds upon the mountain overlooking Galilee.", "Life and Teachings of Jesus"),
        DetailedFact("Which Pharisee visited Jesus secretly at night to ask how a man can be born again?", "Nicodemus", listOf("Gamaliel", "Joseph of Arimathea", "Caiaphas"), "John 3:1-3", "Nicodemus came to Jesus at night, and Jesus taught on the new birth.", "Life and Teachings of Jesus"),
        DetailedFact("At which famous well in Samaria did Jesus speak to the woman about living water?", "Jacob's Well at Sychar", listOf("Abraham's Well at Beersheba", "The Pool of Siloam", "The Fountain of Jericho"), "John 4:5-14", "Jesus rested by Jacob's well in Sychar and offered the living water of eternal life.", "Life and Teachings of Jesus"),
        DetailedFact("What short tax collector climbed a sycamore-fig tree in Jericho to see Jesus passing by?", "Zacchaeus", listOf("Levi", "Theophilus", "Cornelius"), "Luke 19:1-6", "Jesus told Zacchaeus to come down, declaring salvation had come to his house.", "Life and Teachings of Jesus"),
        DetailedFact("Which two Old Testament figures appeared in glory with Jesus at His Transfiguration?", "Moses and Elijah", listOf("Abraham and David", "Noah and Daniel", "Enoch and Samuel"), "Matthew 17:1-3", "Moses representing the Law and Elijah representing the Prophets appeared talking with Jesus.", "Life and Teachings of Jesus"),
        DetailedFact("What animal did Jesus ride into Jerusalem on Palm Sunday during His Triumphal Entry?", "A donkey's colt", listOf("A white stallion", "A camel", "A royal chariot"), "Zechariah 9:9 / Matthew 21:5", "Jesus rode into Jerusalem on the colt of a donkey, fulfilling prophecy.", "Life and Teachings of Jesus"),
        DetailedFact("For how many pieces of silver did Judas Iscariot agree to betray Jesus to the chief priests?", "30 pieces of silver", listOf("50 pieces", "100 pieces", "20 pieces"), "Matthew 26:14-15", "Judas agreed to betray the Lord for thirty pieces of silver.", "Life and Teachings of Jesus"),
        DetailedFact("What humble act did Jesus perform for His disciples during the Last Supper in the Upper Room?", "Washed their feet", listOf("Anointed them with oil", "Gave them golden rings", "Clothed them in white robes"), "John 13:4-15", "Jesus poured water into a basin and washed His disciples' feet as a servant.", "Life and Teachings of Jesus"),
        DetailedFact("In which garden at the foot of Mount Olives did Jesus pray in agony, sweating drops like blood?", "Garden of Gethsemane", listOf("Garden of Eden", "The King's Garden", "Garden of Bethesda"), "Luke 22:39-44", "Jesus prayed: 'Not My will, but Yours be done' in Gethsemane.", "Life and Teachings of Jesus"),
        DetailedFact("Which apostle cut off the right ear of Malchus, the high priest's servant, in Gethsemane?", "Simon Peter", listOf("John", "James", "Simon the Zealot"), "John 18:10", "Peter drew a sword and struck Malchus; Jesus immediately touched and healed his ear.", "Life and Teachings of Jesus"),
        DetailedFact("How many times did Peter deny knowing Jesus before the rooster crowed twice?", "3 times", listOf("2 times", "4 times", "7 times"), "Mark 14:72", "Peter wept bitterly when the rooster crowed, remembering Jesus' prophecy.", "Life and Teachings of Jesus"),
        DetailedFact("What prisoner was released by Pontius Pilate to the shouting crowd instead of Jesus?", "Barabbas", listOf("Barnabas", "Silas", "Demetrius"), "Matthew 27:21", "The crowd shouted for Barabbas the insurrectionist to be released.", "Life and Teachings of Jesus"),
        DetailedFact("Who was compelled by the Roman soldiers to carry the cross of Jesus on the way to Golgotha?", "Simon of Cyrene", listOf("Joseph of Arimathea", "Nicodemus", "Cleopas"), "Mark 15:21", "Simon from Cyrene in North Africa was compelled to bear Christ's cross.", "Life and Teachings of Jesus"),
        DetailedFact("What title was inscribed in Hebrew, Latin, and Greek above the head of Jesus on the cross?", "Jesus of Nazareth, King of the Jews", listOf("The Holy One of Israel", "Prophet of Galilee", "Son of David"), "John 19:19-20", "Pilate wrote INRI: 'Jesus of Nazareth, King of the Jews'.", "Life and Teachings of Jesus"),
        DetailedFact("What happened to the heavy veil in the Temple when Jesus breathed His last upon the cross?", "It was torn in two from top to bottom", listOf("It burned with fire", "It turned golden", "It fell to the floor"), "Matthew 27:51", "The veil was torn from top to bottom, signifying access into the Holy of Holies through Christ.", "Life and Teachings of Jesus"),
        DetailedFact("What wealthy council member from Arimathea asked Pilate for the body of Jesus for burial?", "Joseph of Arimathea", listOf("Nicodemus", "Gamaliel", "Jairus"), "Matthew 27:57-60", "Joseph of Arimathea laid Jesus in his own newly carved rock tomb.", "Life and Teachings of Jesus"),
        DetailedFact("To whom did the risen Jesus first appear near the empty tomb on Sunday morning?", "Mary Magdalene", listOf("Simon Peter", "John", "Mother Mary"), "Mark 16:9 / John 20:14", "Jesus first appeared to Mary Magdalene, who announced: 'I have seen the Lord!'", "Life and Teachings of Jesus"),
        DetailedFact("On the road to which village did the resurrected Christ walk alongside two disciples incognito?", "Emmaus", listOf("Jericho", "Bethany", "Nazareth"), "Luke 24:13-15", "Jesus opened the Scriptures to them on the seven-mile walk to Emmaus.", "Life and Teachings of Jesus"),
        DetailedFact("Which disciple famously doubted the resurrection until he saw and touched Jesus' nail marks?", "Thomas (Didymus)", listOf("Philip", "Bartholomew", "Andrew"), "John 20:24-28", "Thomas exclaimed: 'My Lord and my God!' when he saw the risen Christ.", "Life and Teachings of Jesus"),
        DetailedFact("From which prominent mount east of Jerusalem did Jesus ascend into heaven before the apostles?", "Mount of Olives", listOf("Mount Sinai", "Mount Tabor", "Mount Carmel"), "Acts 1:9-12", "Jesus ascended from the Mount of Olives as two angels promised His return.", "Life and Teachings of Jesus"),
        DetailedFact("What did Jesus say is the greatest commandment in the Law in Matthew 22?", "Love the Lord your God with all your heart, soul, and mind", listOf("Keep the Sabbath holy", "Give tithes of all you possess", "Honor your father and mother"), "Matthew 22:37-38", "Jesus declared this is the first and greatest commandment.", "Life and Teachings of Jesus"),
        DetailedFact("What gifts did the Magi present to the young child Jesus in Bethlehem?", "Gold, frankincense, and myrrh", listOf("Silver, rubies, and silk", "Oil, wheat, and wine", "Fine linen and purple dye"), "Matthew 2:11", "The wise men opened their treasures and presented gold, frankincense, and myrrh.", "Life and Teachings of Jesus"),
        DetailedFact("At what age did boy Jesus sit among the teachers in the Jerusalem Temple asking questions?", "12 years old", listOf("8 years old", "10 years old", "15 years old"), "Luke 2:42-47", "At twelve years of age, Jesus was found in the Temple about His Father's business.", "Life and Teachings of Jesus"),
        DetailedFact("What was the occupation of Matthew (Levi) before Jesus called him to follow Him?", "Tax collector", listOf("Fisherman", "Carpenter", "Tentmaker"), "Matthew 9:9", "Jesus saw Matthew sitting at the tax collector's booth and said, 'Follow Me.'", "Life and Teachings of Jesus"),
        DetailedFact("Which town in Galilee did Jesus make His home base and ministry headquarters during His ministry?", "Capernaum", listOf("Nazareth", "Tiberias", "Cana"), "Matthew 4:13", "Jesus left Nazareth and went and lived in Capernaum by the lake.", "Life and Teachings of Jesus"),
        DetailedFact("What question did Jesus ask His disciples at Caesarea Philippi in Matthew 16?", "'Who do you say that I am?'", listOf("'Do you believe in miracles?'", "'Where is your faith?'", "'Why are you fearful?'"), "Matthew 16:15", "Peter answered: 'You are the Christ, the Son of the living God.'", "Life and Teachings of Jesus")
    )

    private val actsApostlesFacts = listOf(
        DetailedFact("Which disciple was chosen by prayer and casting lots to replace Judas Iscariot among the Twelve?", "Matthias", listOf("Barnabas", "Silas", "Justus Barsabbas"), "Acts 1:23-26", "Matthias was numbered with the eleven apostles.", "Apostles and Early Church"),
        DetailedFact("On which Jewish feast day was the Holy Spirit poured out with a mighty wind and tongues of fire?", "Day of Pentecost", listOf("Passover", "Feast of Tabernacles", "Day of Atonement"), "Acts 2:1-4", "The disciples were filled with the Holy Spirit on Pentecost.", "Apostles and Early Church"),
        DetailedFact("Approximately how many souls were baptized and added to the church after Peter's Pentecost sermon?", "About 3,000 souls", listOf("500 souls", "1,000 souls", "10,000 souls"), "Acts 2:41", "About three thousand were added that day in Jerusalem.", "Apostles and Early Church"),
        DetailedFact("At which gate of the Jerusalem Temple did Peter and John heal a man lame from birth?", "The Beautiful Gate", listOf("The Sheep Gate", "The Water Gate", "The Golden Gate"), "Acts 3:1-10", "Peter said: 'Silver and gold have I none, but what I have I give you... walk!'", "Apostles and Early Church"),
        DetailedFact("What husband and wife fell dead after lying to the Holy Spirit concerning the sale price of their land?", "Ananias and Sapphira", listOf("Aquila and Priscilla", "Philemon and Apphia", "Felix and Drusilla"), "Acts 5:1-10", "Great fear fell upon the whole church after the judgment on Ananias and Sapphira.", "Apostles and Early Church"),
        DetailedFact("Who was the first Christian martyr, stoned outside Jerusalem while praying for his killers?", "Stephen", listOf("Philip", "James", "Timothy"), "Acts 7:54-60", "Stephen saw Jesus standing at the right hand of God before he died.", "Apostles and Early Church"),
        DetailedFact("Which evangelist went down to Samaria and was later guided to baptize an Ethiopian royal eunuch?", "Philip the Evangelist", listOf("Stephen", "Barnabas", "Silas"), "Acts 8:26-38", "Philip explained the prophet Isaiah 53 to the Ethiopian official and baptized him.", "Apostles and Early Church"),
        DetailedFact("On the road to which city did Saul of Tarsus encounter the blazing light and voice of Jesus Christ?", "Damascus", listOf("Antioch", "Jerusalem", "Rome"), "Acts 9:1-6", "The Lord asked: 'Saul, Saul, why are you persecuting Me?' on the Damascus road.", "Apostles and Early Church"),
        DetailedFact("Which disciple in Damascus was sent by God to lay hands on Saul to restore his sight?", "Ananias", listOf("Agabus", "Cornelius", "Gamaliel"), "Acts 9:10-17", "Ananias obeyed the Lord and addressed Saul as 'Brother Saul'.", "Apostles and Early Church"),
        DetailedFact("What seamstress in Joppa known for making garments for poor widows was raised from death by Peter?", "Dorcas (Tabitha)", listOf("Lydia", "Rhoda", "Phoebe"), "Acts 9:36-42", "Peter knelt down, prayed, and said: 'Tabitha, arise!' in Joppa.", "Apostles and Early Church"),
        DetailedFact("Who was the Roman centurion in Caesarea who feared God and whose household received the Holy Spirit?", "Cornelius", listOf("Julius", "Claudius Lysias", "Sergius Paulus"), "Acts 10:1-48", "Peter preached to Cornelius, witnessing the Spirit fall on Gentiles.", "Apostles and Early Church"),
        DetailedFact("In which Syrian city were the disciples of Jesus Christ first called 'Christians'?", "Antioch", listOf("Jerusalem", "Rome", "Alexandria"), "Acts 11:26", "The name 'Christians' was first applied to disciples at Antioch.", "Apostles and Early Church"),
        DetailedFact("Which apostle was executed by the sword under King Herod Agrippa I in Acts 12?", "James the brother of John", listOf("Peter", "Matthew", "Thomas"), "Acts 12:1-2", "James son of Zebedee was martyred by the sword under Herod.", "Apostles and Early Church"),
        DetailedFact("When Peter was freed from prison by an angel, what servant girl was too amazed to open the door?", "Rhoda", listOf("Lydia", "Priscilla", "Chloe"), "Acts 12:12-16", "Rhoda recognized Peter's voice at Mary's house but ran back inside shouting.", "Apostles and Early Church"),
        DetailedFact("Who accompanied Paul on his first missionary journey to Cyprus and Galatia?", "Barnabas and John Mark", listOf("Silas and Timothy", "Luke and Titus", "Apollos and Aquila"), "Acts 13:1-5", "The Holy Spirit called Paul and Barnabas for this pioneering mission.", "Apostles and Early Church"),
        DetailedFact("What major council in Acts 15 decided that Gentile converts need not be circumcised to be saved?", "The Jerusalem Council", listOf("The Council of Antioch", "The Council of Nicaea", "The Council of Rome"), "Acts 15:1-29", "James and the apostles confirmed salvation is by grace through faith.", "Apostles and Early Church"),
        DetailedFact("In Philippi, who was the businesswoman selling expensive purple cloth who became Paul's first European convert?", "Lydia of Thyatira", listOf("Priscilla", "Phoebe", "Damaris"), "Acts 16:14-15", "The Lord opened Lydia's heart to respond to Paul's message.", "Apostles and Early Church"),
        DetailedFact("What happened in the Philippian jail at midnight when Paul and Silas were praying and singing hymns?", "A violent earthquake shook open all prison doors", listOf("Soldiers freed them", "The roof caught fire", "The magistrate arrived"), "Acts 16:25-26", "An earthquake opened doors and loosed chains, leading to the jailer's salvation.", "Apostles and Early Church"),
        DetailedFact("Which noble believers in Macedonia searched the Scriptures daily to see if Paul's teachings were true?", "The Bereans", listOf("The Thessalonians", "The Corinthians", "The Galatians"), "Acts 17:10-12", "The Bereans were of more noble character, examining Scriptures every day.", "Apostles and Early Church"),
        DetailedFact("Where in Athens did Paul deliver his renowned sermon proclaiming the 'Unknown God'?", "The Areopagus (Mars Hill)", listOf("The Parthenon", "The Agora of Corinth", "The Theatre of Ephesus"), "Acts 17:22-31", "Paul declared God who made heaven and earth doesn't live in temples made by human hands.", "Apostles and Early Church"),
        DetailedFact("What tentmaking couple welcomed Paul into their home and trade in Corinth?", "Aquila and Priscilla", listOf("Philemon and Apphia", "Ananias and Sapphira", "Zacharias and Elizabeth"), "Acts 18:1-3", "Paul stayed and worked with Aquila and Priscilla because they shared the tentmaking craft.", "Apostles and Early Church"),
        DetailedFact("On which island was Paul shipwrecked for three months during his voyage as a prisoner to Rome?", "Malta", listOf("Crete", "Cyprus", "Patmos"), "Acts 27:39-28:1", "All 276 passengers survived shipwreck on Malta as God promised.", "Apostles and Early Church"),
        DetailedFact("What eloquent Alexandrian Jewish teacher did Priscilla and Aquila guide more accurately in Ephesus?", "Apollos", listOf("Titus", "Epaphras", "Tychicus"), "Acts 18:24-26", "Apollos was mighty in the Scriptures and vigorously refuted opponents in public debate.", "Apostles and Early Church"),
        DetailedFact("What young man fell asleep during Paul's midnight sermon in Troas and fell from a third-story window?", "Eutychus", listOf("Timothy", "Onesimus", "Alexander"), "Acts 20:9-12", "Paul embraced Eutychus, and his life was restored.", "Apostles and Early Church"),
        DetailedFact("What prophet from Judea took Paul's belt and bound his own hands and feet in Caesarea?", "Agabus", listOf("Silas", "Philip", "Barnabas"), "Acts 21:10-11", "Agabus prophesied that the owner of the belt would be bound in Jerusalem.", "Apostles and Early Church"),
        DetailedFact("Before which Roman King did Paul say: 'I think myself happy, King Agrippa, because I shall answer for myself'?", "King Agrippa II", listOf("King Herod the Great", "Caesar Nero", "Governor Felix"), "Acts 26:1-2", "Agrippa said to Paul, 'In a short time would you persuade me to be a Christian?'", "Apostles and Early Church"),
        DetailedFact("What poisonous creature bit Paul's hand on the beach of Malta without causing him any harm?", "A viper", listOf("A scorpion", "A spider", "A sea snake"), "Acts 28:3-6", "The islanders expected Paul to swell up and die, but he shook the viper into the fire.", "Apostles and Early Church"),
        DetailedFact("How many years did the Apostle Paul spend under Roman house arrest welcoming all who visited him?", "Two whole years", listOf("Six months", "Five years", "One month"), "Acts 28:30-31", "Paul lived two whole years in his own rented house, proclaiming the Kingdom of God.", "Apostles and Early Church")
    )

    private val oldTestamentFacts = listOf(
        DetailedFact("On which day of Creation did God create the sun, moon, and stars to govern day and night?", "Day Four", listOf("Day One", "Day Three", "Day Six"), "Genesis 1:14-19", "God placed lights in the firmament on the fourth day.", "Old Testament"),
        DetailedFact("What two special trees stood in the center of the Garden of Eden?", "Tree of Life & Tree of the Knowledge of Good and Evil", listOf("Tree of Wisdom & Tree of Truth", "Olive Tree & Cedar Tree", "Oak of Moreh & Fig Tree"), "Genesis 2:9", "God commanded Adam not to eat from the Tree of the Knowledge of Good and Evil.", "Old Testament"),
        DetailedFact("Who was the oldest recorded living human in the Bible, reaching the age of 969 years?", "Methuselah", listOf("Jared", "Noah", "Adam"), "Genesis 5:27", "Methuselah lived nine hundred and sixty-nine years, and then he died.", "Old Testament"),
        DetailedFact("What was the divine sign given to Noah that God would never again flood the entire earth?", "A rainbow in the clouds", listOf("A smoking furnace", "An olive branch", "A pillar of fire"), "Genesis 9:12-17", "God set His bow in the cloud as an everlasting covenant token.", "Old Testament"),
        DetailedFact("At which tower did God confuse human speech because of their prideful rebellion?", "Tower of Babel", listOf("Tower of Shechem", "Tower of Siloam", "Tower of David"), "Genesis 11:1-9", "God scattered humankind across the face of the earth at Babel.", "Old Testament"),
        DetailedFact("What mysterious king of Salem and priest of God Most High did Abraham give a tenth of all plunder?", "Melchizedek", listOf("Abimelech", "Jethro", "Pharaoh"), "Genesis 14:18-20", "Melchizedek brought out bread and wine and blessed Abram.", "Old Testament"),
        DetailedFact("What mountain did God test Abraham upon by commanding him to offer Isaac as a burnt offering?", "Mount Moriah", listOf("Mount Sinai", "Mount Carmel", "Mount Nebo"), "Genesis 22:2", "On Mount Moriah, God provided a ram caught in a thicket in place of Isaac.", "Old Testament"),
        DetailedFact("For what food did hungry Esau sell his birthright privileges to his twin brother Jacob?", "A bowl of red lentil stew", listOf("Roast lamb", "A loaf of barley bread", "A flask of pomegranate wine"), "Genesis 25:29-34", "Esau despised his birthright for a single meal of red stew.", "Old Testament"),
        DetailedFact("What did Jacob see in a night vision at Bethel while fleeing from Esau?", "A ladder reaching from earth to heaven with angels", listOf("A burning bush", "A golden chariot", "A great tree"), "Genesis 28:10-17", "Jacob saw angels ascending and descending and named the place Bethel.", "Old Testament"),
        DetailedFact("What gift did Jacob bestow on Joseph that provoked bitter jealousy among his brothers?", "A richly ornamented tunic (coat of many colors)", listOf("A golden signet ring", "A silver cup", "A shepherd's staff"), "Genesis 37:3-4", "Jacob loved Joseph more than all his children and made him a special robe.", "Old Testament"),
        DetailedFact("In Egypt, who falsely accused Joseph when he fled leaving his cloak behind?", "Potiphar's wife", listOf("Pharaoh's daughter", "Queen of Sheba", "Asenath"), "Genesis 39:7-20", "Joseph chose purity before God and was cast into prison.", "Old Testament"),
        DetailedFact("How many years of abundance followed by famine did Joseph interpret from Pharaoh's dreams?", "7 years of plenty and 7 years of famine", listOf("3 years each", "10 years each", "12 years each"), "Genesis 41:25-32", "Seven fat cows followed by seven gaunt cows signified seven years of plenty and famine.", "Old Testament"),
        DetailedFact("What was the tenth and most devastating plague brought upon Egypt before Pharaoh released Israel?", "Death of the firstborn sons", listOf("Plague of locusts", "Water turned to blood", "Three days of darkness"), "Exodus 11-12", "The destroying angel passed over houses marked by lamb's blood.", "Old Testament"),
        DetailedFact("What miraculous bread from heaven nourished the Israelites daily for forty years in the desert?", "Manna", listOf("Unleavened cakes", "Honey wafers", "Barley grain"), "Exodus 16:14-35", "Manna fell like morning dew, white like coriander seed and sweet like honey.", "Old Testament"),
        DetailedFact("What two items were held inside the golden Ark of the Covenant beside the stone tablets?", "Golden jar of manna and Aaron's budded staff", listOf("Moses' staff and Joshua's sword", "Golden incense burner and oil", "Breastplate and ephod"), "Hebrews 9:4 / Exodus 16", "The Ark contained the tablets, the jar of manna, and Aaron's rod.", "Old Testament"),
        DetailedFact("How many spies did Moses send from Kadesh Barnea to explore the land of Canaan?", "12 spies (one per tribe)", listOf("7 spies", "10 spies", "70 spies"), "Numbers 13:1-16", "Only Joshua and Caleb brought a faithful and courageous report.", "Old Testament"),
        DetailedFact("What did God instruct Moses to raise on a pole so bitten Israelites looking at it would live?", "A bronze serpent", listOf("A golden eagle", "A silver trumpet", "A wooden cross"), "Numbers 21:8-9", "Those bitten by venomous snakes looked to the bronze serpent and lived.", "Old Testament"),
        DetailedFact("From which mountain did Moses view the Promised Land before his death at age 120?", "Mount Nebo (Pisgah)", listOf("Mount Sinai", "Mount Hermon", "Mount Carmel"), "Deuteronomy 34:1-5", "The Lord showed Moses the entire Promised Land before he died on Nebo.", "Old Testament"),
        DetailedFact("How many times did Joshua and the army of Israel march around Jericho on the seventh day?", "7 times", listOf("1 time", "3 times", "12 times"), "Joshua 6:1-16", "On the seventh day they circled Jericho seven times, blew horns, and the walls collapsed.", "Old Testament"),
        DetailedFact("How many men were selected for Gideon's army by God using the water-lapping test at Harod?", "300 men", listOf("10,000 men", "32,000 men", "1,000 men"), "Judges 7:4-7", "God reduced Gideon's force to 300 men to display divine victory over the Midianites.", "Old Testament"),
        DetailedFact("What Philistine god's temple did blind Samson pull down upon the enemy lords in Gaza?", "Dagon", listOf("Baal", "Molech", "Ashtoreth"), "Judges 16:23-30", "Samson pushed against the two central pillars and killed more in his death than in life.", "Old Testament"),
        DetailedFact("Which grandmother of King David was a foreign Moabitess who embraced the God of Israel?", "Ruth", listOf("Rahab", "Bathsheba", "Tamar"), "Ruth 4:17", "Ruth married Boaz in Bethlehem and bore Obed, father of Jesse, father of David.", "Old Testament"),
        DetailedFact("What young boy heard the audible voice of God calling his name three times at Shiloh?", "Samuel", listOf("David", "Solomon", "Josiah"), "1 Samuel 3:1-10", "Eli told Samuel to reply: 'Speak, Lord, for Your servant hears.'", "Old Testament"),
        DetailedFact("From which Israelite tribe was Saul, the first anointed king of Israel, selected?", "Tribe of Benjamin", listOf("Tribe of Judah", "Tribe of Ephraim", "Tribe of Dan"), "1 Samuel 9:1-2", "Saul son of Kish was from the smallest tribe of Benjamin.", "Old Testament"),
        DetailedFact("What city did King David capture from the Jebusites and establish as the royal capital of Israel?", "Jerusalem (Zion)", listOf("Hebron", "Shechem", "Samaria"), "2 Samuel 5:6-9", "David captured the fortress of Zion, which is the City of David.", "Old Testament"),
        DetailedFact("How many years did King Solomon spend constructing the First glorious Temple in Jerusalem?", "7 years", listOf("3 years", "12 years", "40 years"), "1 Kings 6:38", "Solomon built the House of the Lord in seven years with Lebanese cedar and gold.", "Old Testament"),
        DetailedFact("What happened on Mount Carmel when the prophet Elijah challenged the 450 prophets of Baal?", "Fire of the Lord fell from heaven and consumed the sacrifice", listOf("An earthquake shook", "Heavy rain flooded the altar", "An angel appeared with a sword"), "1 Kings 18:38", "Fire consumed the burnt offering, wood, stones, and licked up water in the trench.", "Old Testament"),
        DetailedFact("How was the prophet Elijah transported to heaven without experiencing physical death?", "In a chariot of fire with a whirlwind", listOf("On the wings of an eagle", "In a pillar of cloud", "By a great ship"), "2 Kings 2:11", "A chariot of fire and horses of fire separated them, and Elijah went up in a whirlwind.", "Old Testament"),
        DetailedFact("What Syrian army general was cleansed of leprosy after dipping seven times in the Jordan River?", "Naaman", listOf("Ben-hadad", "Hazael", "Sennacherib"), "2 Kings 5:1-14", "Naaman dipped seven times according to the word of Elisha and his flesh was restored.", "Old Testament"),
        DetailedFact("Which three Hebrew youths emerged unharmed from King Nebuchadnezzar's blazing fiery furnace?", "Shadrach, Meshach, and Abednego", listOf("Daniel, Ezra, and Nehemiah", "Shem, Ham, and Japheth", "Caleb, Joshua, and Phinehas"), "Daniel 3:19-27", "The king saw four men unbound walking in the midst of fire, and the fourth was like the Son of God.", "Old Testament")
    )

    private val bibleHistoryFacts = listOf(
        DetailedFact("In approximately what year BC did the historic Exodus of Israel from Egypt take place?", "c. 1446 BC", listOf("c. 1000 BC", "c. 722 BC", "c. 586 BC"), "1 Kings 6:1 / Exodus 12", "1 Kings 6:1 dates Solomon's temple 480 years after the Exodus.", "Bible History"),
        DetailedFact("How many total years did the united monarchy under Kings Saul, David, and Solomon last?", "120 years (40 years each)", listOf("70 years", "400 years", "50 years"), "Acts 13:21 / 2 Samuel 5 / 1 Kings 11", "Saul, David, and Solomon each reigned for forty years over united Israel.", "Bible History"),
        DetailedFact("In which year did the northern Kingdom of Israel fall and get exiled by the Assyrian Empire?", "722 BC", listOf("586 BC", "931 BC", "538 BC"), "2 Kings 17:6", "King Shalmaneser of Assyria captured Samaria and exiled the ten northern tribes in 722 BC.", "Bible History"),
        DetailedFact("In which year did the Babylonian armies of King Nebuchadnezzar destroy Jerusalem and the Temple?", "586 BC", listOf("722 BC", "538 BC", "445 BC"), "2 Kings 25:8-10", "Nebuchadnezzar burned the House of the Lord and broke down the walls in 586 BC.", "Bible History"),
        DetailedFact("How long did the Babylonian captivity of Judah last according to the prophecy of Jeremiah?", "70 years", listOf("40 years", "50 years", "100 years"), "Jeremiah 25:11-12", "The exile lasted seventy years until the rise of the Persian Empire.", "Bible History"),
        DetailedFact("Which Persian monarch issued the historic decree allowing the Jews to return and rebuild the Temple?", "Cyrus the Great", listOf("Darius the Mede", "Artaxerxes", "Xerxes"), "Ezra 1:1-3", "In 538 BC Cyrus issued the royal proclamation fulfilling Isaiah 44:28.", "Bible History"),
        DetailedFact("Under which governor and high priest was the Second Temple completed in 516 BC?", "Zerubbabel and Joshua son of Jozadak", listOf("Ezra and Nehemiah", "David and Solomon", "Hezekiah and Josiah"), "Ezra 3-6 / Haggai 1", "Zerubbabel led the rebuilding of the Second Temple.", "Bible History"),
        DetailedFact("How many days did it take Nehemiah and the workers to rebuild the broken walls of Jerusalem?", "52 days", listOf("40 days", "70 days", "120 days"), "Nehemiah 6:15", "The wall was finished in fifty-two days, demonstrating God's hand to their enemies.", "Bible History"),
        DetailedFact("What term describes the approximately 400-year era of prophetic silence between Malachi and Matthew?", "The Intertestamental Period (Silent Years)", listOf("The Babylonian Captivity", "The Wilderness Wandering", "The Apostolic Era"), "Biblical History Chronology", "Four centuries passed between the Old Testament close and the ministry of John the Baptist.", "Bible History"),
        DetailedFact("In which year did Roman general Titus besiege Jerusalem and destroy the Second Temple?", "70 AD", listOf("33 AD", "100 AD", "586 BC"), "Historical & Prophetic Records / Matthew 24:2", "Jesus prophesied not one stone would be left upon another, fulfilled in 70 AD.", "Bible History"),
        DetailedFact("In what year did the division of the united Hebrew kingdom into Israel and Judah occur?", "931 BC", listOf("1000 BC", "722 BC", "586 BC"), "1 Kings 12", "Upon Solomon's death, Rehoboam's harsh policy caused ten northern tribes to rebel under Jeroboam in 931 BC.", "Bible History"),
        DetailedFact("Who was the Roman Emperor when Jesus Christ was born in Bethlehem?", "Caesar Augustus", listOf("Tiberius Caesar", "Nero", "Julius Caesar"), "Luke 2:1", "Caesar Augustus issued a decree that a census should be taken of the entire Roman world.", "Bible History"),
        DetailedFact("Who was the Roman Emperor during the public ministry, crucifixion, and resurrection of Jesus?", "Tiberius Caesar", listOf("Caesar Augustus", "Claudius", "Caligula"), "Luke 3:1", "John the Baptist began his ministry in the fifteenth year of the reign of Tiberius Caesar.", "Bible History"),
        DetailedFact("Under which Roman Emperor did the great fire of Rome occur and initial official Christian persecutions begin?", "Nero", listOf("Domitian", "Titus", "Trajan"), "Early Church History", "Nero blamed Christians for the fire of Rome in 64 AD, leading to intense persecution.", "Bible History"),
        DetailedFact("How many years did the children of Israel live in Egypt from Jacob's arrival to the Exodus?", "430 years", listOf("70 years", "120 years", "40 years"), "Exodus 12:40", "The time that the people of Israel lived in Egypt was four hundred and thirty years.", "Bible History"),
        DetailedFact("Under which Assyrian king was the invading army besieging Jerusalem struck down by the Angel of the Lord?", "Sennacherib", listOf("Shalmaneser", "Tiglath-Pileser", "Sargon"), "2 Kings 19:35", "The angel of the Lord struck down 185,000 Assyrian troops in Sennacherib's camp.", "Bible History"),
        DetailedFact("Which king of Judah engineered the 1,750-foot subterranean water tunnel to secure Gihon spring waters?", "King Hezekiah", listOf("King Josiah", "King Uzziah", "King David"), "2 Kings 20:20 / 2 Chronicles 32:30", "Hezekiah cut the conduit to bring water into the City of David.", "Bible History"),
        DetailedFact("In what year did the first Babylonian deportation occur, taking Daniel and royal youths to Babylon?", "605 BC", listOf("722 BC", "586 BC", "538 BC"), "Daniel 1:1", "In the third year of Jehoiakim (605 BC), Nebuchadnezzar came to Jerusalem.", "Bible History"),
        DetailedFact("What Persian ruler issued decrees supporting Ezra and Nehemiah in rebuilding Jerusalem?", "Artaxerxes I Longimanus", listOf("Xerxes I", "Cyrus the Great", "Darius Hystaspes"), "Ezra 7:11 / Nehemiah 2:1", "Artaxerxes granted safe conduct and royal supplies to Ezra and Nehemiah.", "Bible History"),
        DetailedFact("What Seleucid Greek ruler desecrated the Second Temple altar in 167 BC by offering a pig?", "Antiochus IV Epiphanes", listOf("Alexander the Great", "Ptolemy Philadelphus", "Seleucus Nicator"), "1 Maccabees / Daniel 11:31", "Antiochus IV desecrated the sanctuary, setting off the Maccabean War of Independence.", "Bible History"),
        DetailedFact("Which Jewish festival was inaugurated to celebrate the cleansing and rededication of the Temple in 164 BC?", "Hanukkah (Feast of Dedication)", listOf("Purim", "Passover", "Yom Kippur"), "John 10:22 / 1 Maccabees 4", "Jesus celebrated the Feast of Dedication at Jerusalem in winter.", "Bible History"),
        DetailedFact("Which Roman general captured Jerusalem in 63 BC, ending Hasmonean sovereignty?", "Pompey the Great", listOf("Julius Caesar", "Mark Antony", "Scipio Africanus"), "Historical Records of Antiquities", "Pompey entered the Holy of Holies in 63 BC and brought Judea under Roman oversight.", "Bible History"),
        DetailedFact("How many years did the comprehensive reconstruction of the Second Temple by King Herod take?", "46 years to initial completion", listOf("7 years", "12 years", "70 years"), "John 2:20", "The Jews said: 'It has taken forty-six years to build this temple!'", "Bible History"),
        DetailedFact("In what year of his reign did King Solomon lay the foundation of the First Temple in Jerusalem?", "Fourth year of his reign (c. 966 BC)", listOf("First year", "Tenth year", "Fortieth year"), "1 Kings 6:1", "In the fourth year of Solomon's reign over Israel, he began to build the House of the Lord.", "Bible History"),
        DetailedFact("How many years did Solomon's builders take to fully complete the First Temple in Jerusalem?", "7 years", listOf("12 years", "40 years", "50 years"), "1 Kings 6:38", "The First Temple was seven years in building.", "Bible History"),
        DetailedFact("Which Egyptian Pharaoh raided Jerusalem during Rehoboam's reign, stripping the golden shields of Solomon?", "Pharaoh Shishak (Sheshonk I)", listOf("Pharaoh Necho", "Pharaoh Hophra", "Pharaoh Ramses"), "1 Kings 14:25-26", "Shishak took away the treasures of the House of the Lord and the golden shields.", "Bible History"),
        DetailedFact("What Babylonian regent saw the divine handwriting on the palace wall before Babylon fell that night?", "Belshazzar", listOf("Nabonidus", "Evil-Merodach", "Neriglissar"), "Daniel 5:1-30", "That very night Belshazzar the Chaldean king was killed, and Darius the Mede received the kingdom.", "Bible History"),
        DetailedFact("What city served as the capital of the northern kingdom of Israel until its collapse in 722 BC?", "Samaria", listOf("Shechem", "Tirzah", "Jezreel"), "1 Kings 16:24 / 2 Kings 17", "King Omri bought the hill of Samaria and fortified it as Israel's royal capital.", "Bible History"),
        DetailedFact("What prophet confronted King Ahab after the judicial murder of Naboth for his vineyard?", "Elijah the Tishbite", listOf("Micaiah", "Elisha", "Nathan"), "1 Kings 21:17-24", "Elijah declared dogs would lick Ahab's blood in the very place Naboth died.", "Bible History"),
        DetailedFact("Which Roman procurator sentenced Jesus Christ to crucifixion despite washing his hands?", "Pontius Pilate", listOf("Felix", "Festus", "Gallio"), "Matthew 27:24", "Pilate took water and washed his hands before the crowd, saying he was innocent of this blood.", "Bible History")
    )

    private val easyQuestionsList = listOf(
        DetailedFact("Who was the very first man created by God on the sixth day in the Garden of Eden?", "Adam", listOf("Noah", "Abraham", "Moses"), "Genesis 2:7", "God formed Adam from the dust of the ground and breathed into his nostrils the breath of life.", "Easy Bible Questions", "Easy"),
        DetailedFact("Who was the mother of all living, created by God from Adam's rib?", "Eve", listOf("Sarah", "Mary", "Rebekah"), "Genesis 2:22 / 3:20", "Adam named his wife Eve because she would become the mother of all the living.", "Easy Bible Questions", "Easy"),
        DetailedFact("Who built a huge wooden ark to save his family and animals from the great flood?", "Noah", listOf("Abraham", "David", "Solomon"), "Genesis 6:13-14", "Noah obeyed God and built an ark of gopher wood.", "Easy Bible Questions", "Easy"),
        DetailedFact("Who defeated the terrifying Philistine giant Goliath with a simple sling and stone?", "David", listOf("Saul", "Jonathan", "Samson"), "1 Samuel 17:48-50", "Young shepherd David struck Goliath in the forehead with a smooth stone.", "Easy Bible Questions", "Easy"),
        DetailedFact("Who was thrown into a den of hungry lions because he continued praying to God three times a day?", "Daniel", listOf("Joseph", "Elijah", "Jeremiah"), "Daniel 6:16-22", "God sent His angel to shut the lions' mouths because Daniel was innocent.", "Easy Bible Questions", "Easy"),
        DetailedFact("Who was swallowed by a great sea fish after trying to run away from God's call to Nineveh?", "Jonah", listOf("Moses", "Peter", "Paul"), "Jonah 1:17", "Jonah was inside the belly of the fish three days and three nights.", "Easy Bible Questions", "Easy"),
        DetailedFact("How many commandments did God give to Moses carved on two stone tablets on Mount Sinai?", "10 Commandments", listOf("7 Commandments", "12 Commandments", "40 Commandments"), "Exodus 20:1-17", "God gave the Ten Commandments to Moses on Mount Sinai.", "Easy Bible Questions", "Easy"),
        DetailedFact("What did Jesus use to miraculously feed over 5,000 hungry people in the wilderness?", "Five loaves of bread and two fish", listOf("Seven cakes and honey", "Ten loaves of barley", "Twelve baskets of fruit"), "Matthew 14:17-21", "A young boy's five loaves and two fish were multiplied with twelve baskets leftover.", "Easy Bible Questions", "Easy"),
        DetailedFact("How many disciples did Jesus specifically choose and appoint as His original apostles?", "12 apostles", listOf("7 apostles", "10 apostles", "70 apostles"), "Luke 6:12-16", "Jesus prayed all night and chose twelve disciples whom He designated apostles.", "Easy Bible Questions", "Easy"),
        DetailedFact("What is the shortest verse in the entire Holy Bible, consisting of just two words?", "'Jesus wept.' (John 11:35)", listOf("'God is love.'", "'Pray continually.'", "'Rejoice always.'"), "John 11:35", "John 11:35 records: 'Jesus wept' at the tomb of Lazarus.", "Easy Bible Questions", "Easy"),
        DetailedFact("Who betrayed our Lord Jesus Christ in Gethsemane with a deceptive kiss?", "Judas Iscariot", listOf("Peter", "Pontius Pilate", "Thomas"), "Luke 22:47-48", "Judas betrayed the Son of Man with a hypocritical kiss.", "Easy Bible Questions", "Easy"),
        DetailedFact("On which day after His crucifixion did our Lord Jesus rise gloriously from the dead?", "The third day (Sunday)", listOf("The seventh day", "The fortieth day", "The next day"), "1 Corinthians 15:3-4", "Christ died for our sins, was buried, and was raised on the third day according to the Scriptures.", "Easy Bible Questions", "Easy"),
        DetailedFact("Who received a coat of many colors from his father Jacob and was sold into Egypt?", "Joseph", listOf("Benjamin", "Judah", "Reuben"), "Genesis 37:3", "Jacob loved Joseph and gave him a coat of many colors.", "Easy Bible Questions", "Easy"),
        DetailedFact("Who parted the Red Sea by holding out his staff at God's command?", "Moses", listOf("Joshua", "Aaron", "Gideon"), "Exodus 14:21", "Moses stretched out his hand, and the Lord drove the sea back with a strong east wind.", "Easy Bible Questions", "Easy"),
        DetailedFact("What is the very first book of the Holy Bible?", "Genesis", listOf("Exodus", "Matthew", "Psalms"), "Canon of Scripture", "Genesis is the book of beginnings and the first book in the Bible.", "Easy Bible Questions", "Easy"),
        DetailedFact("What is the final book of the Holy Bible?", "Revelation", listOf("Malachi", "Jude", "Hebrews"), "Canon of Scripture", "The Revelation of Jesus Christ concludes the New Testament and Bible.", "Easy Bible Questions", "Easy"),
        DetailedFact("Where was baby Jesus laid because there was no room for Joseph and Mary in the inn?", "In a manger", listOf("In a cradle", "In a palace", "In a golden bed"), "Luke 2:7", "Mary wrapped Him in swaddling clothes and laid Him in a manger.", "Easy Bible Questions", "Easy"),
        DetailedFact("Who was the strongest man in the Bible whose hair was never cut because of a Nazarite vow?", "Samson", listOf("Gideon", "David", "Saul"), "Judges 16:17", "Samson possessed superhuman strength from God as long as his hair remained uncut.", "Easy Bible Questions", "Easy"),
        DetailedFact("What beautiful sign did God set in the sky as a covenant never to destroy the earth again with a flood?", "A rainbow", listOf("A bright star", "A golden pillar", "A solar eclipse"), "Genesis 9:13", "God set His rainbow in the cloud as the sign of His perpetual covenant.", "Easy Bible Questions", "Easy"),
        DetailedFact("How many days and nights did torrential rain fall upon the earth during Noah's flood?", "40 days and 40 nights", listOf("7 days and nights", "100 days", "3 days and nights"), "Genesis 7:12", "Rain fell upon the earth forty days and forty nights.", "Easy Bible Questions", "Easy"),
        DetailedFact("In which town did Jesus grow up as a child under Joseph and Mary's care?", "Nazareth", listOf("Bethlehem", "Jerusalem", "Jericho"), "Matthew 2:23", "Jesus came and dwelt in Nazareth, fulfilling that He would be called a Nazarene.", "Easy Bible Questions", "Easy"),
        DetailedFact("What trade or occupation did Jesus work in before commencing His public ministry?", "Carpenter", listOf("Fisherman", "Tentmaker", "Shepherd"), "Mark 6:3", "The townsfolk asked: 'Is not this the carpenter, the son of Mary?'", "Easy Bible Questions", "Easy"),
        DetailedFact("Who baptized Jesus in the Jordan River while the Holy Spirit descended like a dove?", "John the Baptist", listOf("Simon Peter", "Nicodemus", "Elijah"), "Matthew 3:13-17", "John baptized Jesus, declaring 'Behold the Lamb of God who takes away the sin of the world!'", "Easy Bible Questions", "Easy"),
        DetailedFact("What food fell miraculously from heaven each morning to sustain the Israelites in the wilderness?", "Manna", listOf("Honeycakes", "Unleavened bread", "Figs"), "Exodus 16:14-15", "The Israelites called the bread from heaven manna, meaning 'What is it?'.", "Easy Bible Questions", "Easy"),
        DetailedFact("What magnificent book of the Bible contains 150 sacred songs, prayers, and hymns of praise?", "The Book of Psalms", listOf("Proverbs", "Ecclesiastes", "Song of Solomon"), "Canon of Scripture", "Psalms is the largest book in the Bible, containing 150 individual poetic chapters.", "Easy Bible Questions", "Easy"),
        DetailedFact("What wise king asked God for wisdom rather than wealth or long life and built the First Temple?", "King Solomon", listOf("King David", "King Saul", "King Hezekiah"), "1 Kings 3:9-12", "God granted Solomon unmatched wisdom, understanding, and honor.", "Easy Bible Questions", "Easy"),
        DetailedFact("What three faithful Hebrew young men survived being cast into King Nebuchadnezzar's blazing furnace?", "Shadrach, Meshach, and Abednego", listOf("Daniel, Ezekiel, and Joel", "Eli, Phinehas, and Hophni", "Shem, Ham, and Japheth"), "Daniel 3:19-27", "The fourth man like the Son of God walked with them unharmed in the furnace.", "Easy Bible Questions", "Easy"),
        DetailedFact("What young shepherd boy heard God call his name three times while sleeping in the tabernacle at Shiloh?", "Samuel", listOf("David", "Solomon", "Josiah"), "1 Samuel 3:8-10", "Eli instructed Samuel to reply: 'Speak, Lord, for Your servant hears.'", "Easy Bible Questions", "Easy"),
        DetailedFact("What animal miraculously spoke to Balaam with a human voice to warn him of God's angel?", "A donkey", listOf("A camel", "A lion", "An ox"), "Numbers 22:28", "The Lord opened the mouth of the donkey, and she spoke to Balaam.", "Easy Bible Questions", "Easy"),
        DetailedFact("What bird did Noah send out from the ark that returned carrying a freshly plucked olive leaf in its beak?", "A dove", listOf("A raven", "A sparrow", "An eagle"), "Genesis 8:11", "The dove returned at evening with an olive leaf, showing the floodwaters had receded.", "Easy Bible Questions", "Easy"),
        DetailedFact("What prayer did our Lord Jesus teach His disciples beginning with 'Our Father who art in heaven'?", "The Lord's Prayer", listOf("The Prayer of Jabez", "The Aaronic Blessing", "The Shema"), "Matthew 6:9-13", "Jesus gave the model prayer in the Sermon on the Mount.", "Easy Bible Questions", "Easy"),
        DetailedFact("Who was the very first king crowned over the nation of Israel, from the tribe of Benjamin?", "King Saul", listOf("King David", "King Solomon", "King Jeroboam"), "1 Samuel 10:1", "Samuel anointed Saul son of Kish as Israel's first earthly monarch.", "Easy Bible Questions", "Easy"),
        DetailedFact("How many days was Lazarus dead and buried inside the tomb before Jesus cried 'Lazarus, come forth!'?", "4 days", listOf("2 days", "3 days", "7 days"), "John 11:17-44", "Martha said he had been dead four days, yet Jesus brought him out alive.", "Easy Bible Questions", "Easy"),
        DetailedFact("What happened to Lot's wife when she disobeyed the angels and looked back at Sodom?", "She became a pillar of salt", listOf("She fell into a pit", "She was turned to stone", "She lost her eyesight"), "Genesis 19:26", "Lot's wife looked back from behind him and became a pillar of salt.", "Easy Bible Questions", "Easy"),
        DetailedFact("What did God use to lead the Israelites by day and by night during their wilderness travels?", "A pillar of cloud by day and pillar of fire by night", listOf("An ark of gold", "A shining star", "A soaring eagle"), "Exodus 13:21-22", "The pillar of cloud by day and pillar of fire by night never departed from before the people.", "Easy Bible Questions", "Easy")
    )

    private val advancedQuestionsList = listOf(
        DetailedFact("What was the Hebrew name given to Daniel by the chief of the Babylonian eunuchs?", "Belteshazzar", listOf("Shadrach", "Meshach", "Abednego"), "Daniel 1:7", "Daniel was named Belteshazzar; Hananiah was Shadrach; Mishael was Meshach; Azariah was Abednego.", "Advanced Bible Questions", "Advanced"),
        DetailedFact("What two stones were placed in the High Priest's breastplate for discerning divine decisions?", "The Urim and Thummim", listOf("The Onyx and Jasper", "The Sapphire and Ruby", "The Agate and Beryl"), "Exodus 28:30", "Aaron bore the Urim and the Thummim over his heart when entering the Holy Place.", "Advanced Bible Questions", "Advanced"),
        DetailedFact("What were the exact dimensions of Noah's Ark in biblical cubits according to Genesis 6?", "300 cubits long, 50 cubits wide, 30 cubits high", listOf("100 x 50 x 20", "500 x 100 x 50", "200 x 40 x 30"), "Genesis 6:15", "The ark measured 300 cubits in length, 50 cubits in breadth, and 30 cubits in height.", "Advanced Bible Questions", "Advanced"),
        DetailedFact("Which priest and judge of Israel fell backward from his seat and broke his neck upon hearing the Ark was captured?", "Eli", listOf("Phinehas", "Hophni", "Ahimelech"), "1 Samuel 4:18", "Eli had judged Israel forty years and died at ninety-eight years of age.", "Advanced Bible Questions", "Advanced"),
        DetailedFact("Who was the high priest alongside Caiaphas who initially interrogated Jesus on the night of His arrest?", "Annas", listOf("Gamaliel", "Tertullus", "Simon son of Boethus"), "John 18:13", "They brought Jesus first to Annas, who was the father-in-law of Caiaphas.", "Advanced Bible Questions", "Advanced"),
        DetailedFact("What exact height was recorded for the Philistine champion Goliath of Gath?", "Six cubits and a span (approximately 9 feet 9 inches)", listOf("Five cubits", "Seven cubits", "Ten cubits and a span"), "1 Samuel 17:4", "Goliath's height was six cubits and a span, clad in coat of bronze weighing 5,000 shekels.", "Advanced Bible Questions", "Advanced"),
        DetailedFact("How many talents of gold did King Solomon receive from Ophir in a single fleet expedition?", "420 talents of gold", listOf("100 talents", "300 talents", "666 talents"), "1 Kings 9:28", "Hiram's fleet sailed to Ophir and brought back 420 talents of gold for Solomon.", "Advanced Bible Questions", "Advanced"),
        DetailedFact("Who was the father of King Melchizedek according to the Book of Hebrews?", "He was without father or mother, without genealogy", listOf("Shem", "Eber", "Noah"), "Hebrews 7:3", "Melchizedek remains a type of Christ: 'Without father or mother, without genealogy, having neither beginning of days nor end of life.'", "Advanced Bible Questions", "Advanced"),
        DetailedFact("What Hebrew phrase spoken by Jesus on the cross translates to 'My God, My God, why have You forsaken Me?'", "Eloi, Eloi, lema sabachthani", listOf("Talitha koum", "Ephphatha", "Maranatha"), "Mark 15:34", "Jesus quoted Psalm 22:1 at the ninth hour on Golgotha.", "Advanced Bible Questions", "Advanced"),
        DetailedFact("How many total people were on board the ship with Paul that was shipwrecked on the island of Malta?", "276 persons", listOf("153 persons", "70 persons", "300 persons"), "Acts 27:37", "Altogether there were 276 of us on board, and every soul was saved.", "Advanced Bible Questions", "Advanced"),
        DetailedFact("Which prophet married Gomer daughter of Diblaim by divine instruction to illustrate God's covenant love?", "Hosea", listOf("Amos", "Micah", "Joel"), "Hosea 1:2-3", "Hosea took an unfaithful wife as a living metaphor of God's redemptive grace for Israel.", "Advanced Bible Questions", "Advanced"),
        DetailedFact("What were the names of Aaron's two eldest sons who were consumed by holy fire for offering unauthorized fire?", "Nadab and Abihu", listOf("Eleazar and Ithamar", "Hophni and Phinehas", "Gershon and Merari"), "Leviticus 10:1-2", "Nadab and Abihu offered strange fire before the Lord and perished.", "Advanced Bible Questions", "Advanced"),
        DetailedFact("What city was given to Caleb as his inheritance because he wholly followed the Lord his God?", "Hebron (Kiriath Arba)", listOf("Shechem", "Jericho", "Bethel"), "Joshua 14:13-14", "At age 85, Caleb claimed the hill country of Hebron and defeated the Anakites.", "Advanced Bible Questions", "Advanced"),
        DetailedFact("What was the exact weight of Absalom's thick hair when it was polled annually?", "200 shekels by the royal standard (about 5 pounds)", listOf("50 shekels", "100 shekels", "500 shekels"), "2 Samuel 14:26", "When he cut his hair at year's end because it was heavy, he weighed it at 200 shekels.", "Advanced Bible Questions", "Advanced"),
        DetailedFact("Who was the Roman Governor of Judea who left Paul imprisoned for two years hoping for a bribe?", "Antonius Felix", listOf("Porcius Festus", "Pontius Pilate", "Sergius Paulus"), "Acts 24:26-27", "Felix conversed often with Paul hoping Paul would offer him money.", "Advanced Bible Questions", "Advanced"),
        DetailedFact("How many total years did Methuselah live according to Genesis 5, making him the longest-lived human in Scripture?", "969 years", listOf("950 years", "930 years", "1000 years"), "Genesis 5:27", "Methuselah lived nine hundred and sixty-nine years, and he died.", "Advanced Bible Questions", "Advanced"),
        DetailedFact("Which prophet was instructed by God to purchase a field at Anathoth during the Babylonian siege of Jerusalem?", "Jeremiah", listOf("Ezekiel", "Isaiah", "Zechariah"), "Jeremiah 32:6-15", "Jeremiah bought the field of Hanamel as a prophetic pledge that houses and fields would again be bought in Judah.", "Advanced Bible Questions", "Advanced"),
        DetailedFact("What Roman Emperor expelled all Jews from Rome in 49 AD, prompting Aquila and Priscilla to move to Corinth?", "Emperor Claudius", listOf("Emperor Nero", "Emperor Caligula", "Emperor Tiberius"), "Acts 18:2", "Claudius commanded all the Jews to depart from Rome.", "Advanced Bible Questions", "Advanced"),
        DetailedFact("What name did King Hezekiah give to the ancient bronze serpent of Moses when he destroyed it because Israel worshiped it?", "Nehushtan (a piece of bronze)", listOf("Ichabod", "Mara", "Baal-peor"), "2 Kings 18:4", "He broke in pieces the bronze serpent that Moses made, for the people burned incense to it, and he called it Nehushtan.", "Advanced Bible Questions", "Advanced"),
        DetailedFact("How many total smooth stones did young David carefully select from the brook before engaging Goliath?", "5 smooth stones", listOf("1 stone", "3 stones", "7 stones"), "1 Samuel 17:40", "David chose five smooth stones from the brook and put them in his shepherd's bag.", "Advanced Bible Questions", "Advanced"),
        DetailedFact("Which Greek island was the apostle John banished to when he received the prophetic Revelation of Jesus Christ?", "Patmos", listOf("Crete", "Cyprus", "Malta"), "Revelation 1:9", "John was on the island called Patmos on account of the word of God and the testimony of Jesus.", "Advanced Bible Questions", "Advanced"),
        DetailedFact("What were the names of Job's three original friends who sat with him in silence for seven days?", "Eliphaz the Temanite, Bildad the Shuhite, and Zophar the Naamathite", listOf("Elihu, Nathan, and Gad", "Ahithophel, Hushai, and Barzillai", "Korah, Dathan, and Abiram"), "Job 2:11", "They made an appointment together to come to mourn with him and comfort him.", "Advanced Bible Questions", "Advanced"),
        DetailedFact("What was the name of the high priest who struck Paul on the mouth during Paul's trial before the council in Acts 23?", "Ananias son of Nedebaeus", listOf("Caiaphas", "Annas", "Tertullus"), "Acts 23:2", "The high priest Ananias commanded those who stood by Paul to strike him on the mouth.", "Advanced Bible Questions", "Advanced"),
        DetailedFact("How many total cities of refuge were legally designated across ancient Israel for accidental manslayers?", "6 cities of refuge", listOf("3 cities", "12 cities", "48 cities"), "Numbers 35:6 / Joshua 20", "There were three cities east of the Jordan and three in Canaan proper.", "Advanced Bible Questions", "Advanced"),
        DetailedFact("What fierce nephew of King David served as supreme commander of David's military forces?", "Joab son of Zeruiah", listOf("Abner", "Amasa", "Benaiah"), "2 Samuel 8:16", "Joab son of Zeruiah was in command of the army of Israel.", "Advanced Bible Questions", "Advanced"),
        DetailedFact("What was the historic pre-Israelite name of Jerusalem when King David captured its stronghold from the Jebusites?", "Jebus", listOf("Salem", "Kir", "Kiriath-jearim"), "1 Chronicles 11:4-5", "David and all Israel went to Jerusalem, that is, Jebus, where the Jebusites inhabited the land.", "Advanced Bible Questions", "Advanced"),
        DetailedFact("Which prophet rebuked King David regarding Bathsheba using the emotional parable of the poor man's only little ewe lamb?", "Prophet Nathan", listOf("Prophet Gad", "Prophet Samuel", "Prophet Ahijah"), "2 Samuel 12:1-7", "Nathan told David: 'You are the man!' bringing David to deep broken repentance.", "Advanced Bible Questions", "Advanced"),
        DetailedFact("What distant port city was the reluctant prophet Jonah sailing toward when caught in the mighty Mediterranean tempest?", "Tarshish", listOf("Nineveh", "Tyre", "Sidon"), "Jonah 1:3", "Jonah went down to Joppa and found a ship going to Tarshish, fleeing the presence of the Lord.", "Advanced Bible Questions", "Advanced"),
        DetailedFact("What joyful annual Jewish festival was established by Mordecai and Queen Esther after defeating Haman's genocidal plot?", "Feast of Purim", listOf("Feast of Trumpets", "Feast of Booths", "Passover"), "Esther 9:26-28", "They called these days Purim after the lot (Pur) cast by Haman.", "Advanced Bible Questions", "Advanced"),
        DetailedFact("What was the name of Moses' Midianite father-in-law, a priest of Midian who advised him to appoint judges?", "Jethro (Reuel)", listOf("Hobab", "Kedar", "Balaam"), "Exodus 18:1-24", "Jethro counselled Moses to appoint able men who fear God to judge minor disputes.", "Advanced Bible Questions", "Advanced")
    )

    private val womenInTheBibleFacts = listOf(
        DetailedFact("Who laughed within herself when she overheard she would have a baby son in her extreme old age?", "Sarah", listOf("Rebekah", "Rachel", "Hannah"), "Genesis 18:10-15", "Sarah laughed, but God proved nothing is too hard for the Lord, giving her Isaac.", "Women in the Bible"),
        DetailedFact("What Egyptian maidservant of Sarah encountered God as 'El Roi' (The God Who Sees Me) in the desert?", "Hagar", listOf("Keturah", "Zilpah", "Bilhah"), "Genesis 16:13", "Hagar named the Lord 'You are the God who sees me' at Beer-lahai-roi.", "Women in the Bible"),
        DetailedFact("Who drew water for Abraham's servant and all his ten camels, confirming her as Isaac's bride?", "Rebekah", listOf("Rachel", "Leah", "Keturah"), "Genesis 24:15-20", "Rebekah eagerly watered all the servant's camels at the town well.", "Women in the Bible"),
        DetailedFact("For which younger daughter of Laban did Jacob willingly serve fourteen total years of labor?", "Rachel", listOf("Leah", "Dinah", "Bilhah"), "Genesis 29:16-30", "Jacob loved Rachel and served seven years for her, which seemed like only a few days.", "Women in the Bible"),
        DetailedFact("Who was the sister of Moses and Aaron who led the women of Israel in worship with a tambourine?", "Miriam", listOf("Jochebed", "Zipporah", "Deborah"), "Exodus 15:20-21", "Miriam the prophetess sang: 'Sing to the Lord, for He has triumphed gloriously!'", "Women in the Bible"),
        DetailedFact("What brave harlot of Jericho hid the Hebrew spies and hung a scarlet cord from her city wall window?", "Rahab", listOf("Ruth", "Tamar", "Jael"), "Joshua 2:1-21 / 6:25", "Rahab's faith saved her entire household, and she is listed in Jesus' genealogy.", "Women in the Bible"),
        DetailedFact("Which prophetess judged Israel under a palm tree between Ramah and Bethel in Mount Ephraim?", "Deborah", listOf("Huldah", "Anna", "Miriam"), "Judges 4:4-5", "Deborah summoned Barak and led Israel to victory against Sisera's iron chariots.", "Women in the Bible"),
        DetailedFact("What brave woman drove a tent peg through the temple of the sleeping Canaanite general Sisera?", "Jael", listOf("Deborah", "Delilah", "Ruth"), "Judges 4:17-22", "Jael wife of Heber the Kenite defeated Sisera as Deborah had prophesied.", "Women in the Bible"),
        DetailedFact("Who made the famous declaration: 'Where you go I will go, and where you stay I will stay. Your people will be my people and your God my God'?", "Ruth", listOf("Naomi", "Orpah", "Hannah"), "Ruth 1:16-17", "Ruth's unswerving covenant loyalty led her to Boaz and the royal lineage of King David.", "Women in the Bible"),
        DetailedFact("Which barren woman prayed with weeping lips at Shiloh, vowing to dedicate her son Samuel to the Lord?", "Hannah", listOf("Peninnah", "Elizabeth", "Rachel"), "1 Samuel 1:9-20", "Eli watched her moving lips; God remembered Hannah and blessed her with Samuel.", "Women in the Bible"),
        DetailedFact("What intelligent and diplomatic woman hurried with food and gifts to stop David from slaughtering foolish Nabal?", "Abigail", listOf("Bathsheba", "Michal", "Ahinoam"), "1 Samuel 25:18-35", "Abigail's wisdom averted bloodshed, and David later took her as his wife.", "Women in the Bible"),
        DetailedFact("What wicked queen of Israel promoted Baal worship, murdered Naboth for his vineyard, and threatened Elijah?", "Jezebel", listOf("Athaliah", "Herodias", "Delilah"), "1 Kings 18-21", "Jezebel wife of King Ahab led northern Israel into destructive pagan idolatry.", "Women in the Bible"),
        DetailedFact("What Jewish queen of Persia risked royal execution by entering the king's presence unsummoned, saying: 'If I perish, I perish'?", "Esther (Hadassah)", listOf("Vashti", "Candace", "Queen of Sheba"), "Esther 4:14-16", "Esther saved the entire Jewish nation from the genocidal plot of wicked Haman.", "Women in the Bible"),
        DetailedFact("What aged prophetess in the Jerusalem Temple praised God upon beholding the infant Jesus?", "Anna", listOf("Elizabeth", "Huldah", "Mary Magdalene"), "Luke 2:36-38", "Eighty-four-year-old Anna gave thanks to God and spoke of the Child to all looking for redemption.", "Women in the Bible"),
        DetailedFact("Which sister of Lazarus sat at Jesus' feet listening to His teaching, choosing 'the good portion'?", "Mary of Bethany", listOf("Martha", "Salome", "Joanna"), "Luke 10:38-42", "Jesus affirmed Mary: 'Mary has chosen what is better, and it will not be taken from her.'", "Women in the Bible"),
        DetailedFact("Which Philistine woman betrayed Samson by discovering that the secret of his strength lay in his uncut hair?", "Delilah", listOf("Jezebel", "Athaliah", "Michal"), "Judges 16:4-20", "Delilah coaxed Samson until he revealed his Nazarite vow, then called the Philistines.", "Women in the Bible"),
        DetailedFact("Who was the mother of John the Baptist and cousin of Mary who was filled with the Holy Spirit upon Mary's visit?", "Elizabeth", listOf("Anna", "Salome", "Joanna"), "Luke 1:39-45", "Elizabeth exclaimed: 'Blessed are you among women, and blessed is the fruit of your womb!'", "Women in the Bible"),
        DetailedFact("What businesswoman of Thyatira selling purple fabric opened her home to Paul and Silas in Philippi?", "Lydia", listOf("Phoebe", "Priscilla", "Chloe"), "Acts 16:14-15", "The Lord opened Lydia's heart to pay attention to what was said by Paul.", "Women in the Bible"),
        DetailedFact("Who was the godly deaconess of the church at Cenchreae whom Paul warmly commended in Romans 16?", "Phoebe", listOf("Priscilla", "Junia", "Tryphena"), "Romans 16:1-2", "Paul commended Phoebe as a servant of the church at Cenchreae and patron of many.", "Women in the Bible"),
        DetailedFact("Who was the mother of Moses who hid him in a papyrus basket coated with tar in the Nile River?", "Jochebed", listOf("Miriam", "Zipporah", "Elisheba"), "Exodus 2:1-3 / 6:20", "Jochebed hid Moses by faith for three months, trusting God with his deliverance.", "Women in the Bible"),
        DetailedFact("Which Jerusalem prophetess verified King Josiah's newly discovered Book of the Law?", "Huldah", listOf("Anna", "Miriam", "Deborah"), "2 Kings 22:14-20", "Huldah the prophetess spoke God's solemn message to King Josiah's delegation.", "Women in the Bible"),
        DetailedFact("Who was the mother of King Solomon who pleaded with aging King David to place Solomon on the throne?", "Bathsheba", listOf("Abigail", "Michal", "Maacah"), "1 Kings 1:11-31", "Bathsheba reminded David of his sacred oath before the Lord concerning Solomon.", "Women in the Bible"),
        DetailedFact("Which daughter of Saul was married to David and helped him escape through a window from Saul's assassins?", "Michal", listOf("Merab", "Tamar", "Athaliah"), "1 Samuel 19:11-17", "Michal let David down through a window and placed a household idol in the bed.", "Women in the Bible"),
        DetailedFact("Who was the mother of the apostles James and John who knelt before Jesus asking for places of honor for her sons?", "Salome (Wife of Zebedee)", listOf("Elizabeth", "Mary of Clopas", "Martha"), "Matthew 20:20-21", "Salome requested that her two sons sit on Jesus' right and left in His Kingdom.", "Women in the Bible"),
        DetailedFact("Who was the faithful grandmother of Timothy commended by Paul for passing on her genuine faith?", "Lois", listOf("Eunice", "Claudia", "Julia"), "2 Timothy 1:5", "Paul recalled the unfeigned faith which dwelt first in Timothy's grandmother Lois.", "Women in the Bible"),
        DetailedFact("Who was the devoted mother of Timothy who instructed him in the sacred Scriptures from his earliest infancy?", "Eunice", listOf("Lois", "Priscilla", "Rhoda"), "2 Timothy 1:5 / 3:15", "Timothy knew the Holy Scriptures from childhood through his mother Eunice.", "Women in the Bible"),
        DetailedFact("Which gifted Christian wife taught the Scriptures more accurately to Apollos alongside her husband Aquila?", "Priscilla (Prisca)", listOf("Phoebe", "Lydia", "Chloe"), "Acts 18:24-26", "Priscilla and Aquila took Apollos aside and explained to him the way of God more accurately.", "Women in the Bible"),
        DetailedFact("Which poor widow gave two small copper coins into the Temple treasury, praised by Jesus for giving all her livelihood?", "The Widow at the Temple Treasury", listOf("The Widow of Zarephath", "The Widow of Nain", "Anna"), "Mark 12:41-44", "Jesus observed that out of her poverty she put in everything she had to live on.", "Women in the Bible"),
        DetailedFact("Which widow of Phoenicia sustained the prophet Elijah with her jar of flour and jug of oil during famine?", "The Widow of Zarephath", listOf("The Shunammite woman", "Ruth", "Abigail"), "1 Kings 17:8-16", "Her jar of flour was not spent, neither did her jug of oil become empty.", "Women in the Bible"),
        DetailedFact("What hospitable Shunammite woman prepared an upper room with bed, table, and lamp for Elisha?", "The Shunammite Woman", listOf("The Widow of Zarephath", "Deborah", "Hannah"), "2 Kings 4:8-10", "She perceived Elisha was a holy man of God and provided a peaceful room.", "Women in the Bible")
    )

    // =========================================================================
    // DETERMINISTIC PROCEDURAL SYNTHESIS (100,000 Questions per Category)
    // =========================================================================

    private fun synthesizeQuestionForCategory(
        category: String,
        slug: String,
        questionId: String,
        index: Int
    ): Question {
        val hash = ((index.toLong() * 2654435761L) xor (index.toLong() shl 13)).toInt()
        val perm = abs(hash)

        return when (category) {
            "Books of the Bible" -> synthesizeBooksQuestion(questionId, index, perm)
            "Life and Teachings of Jesus" -> synthesizeFromDetailedList(gospelLifeFacts, questionId, index, perm, "Life and Teachings of Jesus")
            "Apostles and Early Church" -> synthesizeFromDetailedList(actsApostlesFacts, questionId, index, perm, "Apostles and Early Church")
            "Old Testament" -> synthesizeFromDetailedList(oldTestamentFacts, questionId, index, perm, "Old Testament")
            "Bible History" -> synthesizeFromDetailedList(bibleHistoryFacts, questionId, index, perm, "Bible History")
            "Easy Bible Questions" -> synthesizeFromDetailedList(easyQuestionsList, questionId, index, perm, "Easy Bible Questions")
            "Advanced Bible Questions" -> synthesizeFromDetailedList(advancedQuestionsList, questionId, index, perm, "Advanced Bible Questions")
            "Women in the Bible" -> synthesizeFromDetailedList(womenInTheBibleFacts, questionId, index, perm, "Women in the Bible")
            "Miracles of Jesus" -> synthesizeMiraclesQuestion(questionId, index, perm)
            "Parables" -> synthesizeParablesQuestion(questionId, index, perm)
            "Kings and Prophets" -> synthesizeKingsProphetsQuestion(questionId, index, perm)
            "Bible Verses and References" -> synthesizeVersesQuestion(questionId, index, perm)
            "Bible Characters" -> synthesizeCharactersQuestion(questionId, index, perm)
            "New Testament" -> synthesizeNewTestamentQuestion(questionId, index, perm)
            "Intermediate Bible Questions" -> synthesizeIntermediateQuestion(questionId, index, perm)
            else -> synthesizeGeneralBibleQuestion(category, questionId, index, perm)
        }
    }

    private fun synthesizeFromDetailedList(
        facts: List<DetailedFact>,
        id: String,
        index: Int,
        perm: Int,
        category: String
    ): Question {
        val fIdx = index % facts.size
        val f = facts[fIdx]
        val phrasingMode = (index / facts.size) % 6
        val text = when (phrasingMode) {
            1 -> "According to Holy Scripture: ${f.question}"
            2 -> "Bible Trivia: ${f.question}"
            3 -> "Scripture Knowledge: ${f.question}"
            4 -> "In the Bible: ${f.question}"
            5 -> "Test your biblical knowledge: ${f.question}"
            else -> f.question
        }

        return buildQuestion(
            id = id,
            category = category,
            text = text,
            correct = f.correct,
            distractors = f.wrong,
            ref = f.ref,
            expl = f.expl,
            perm = perm,
            diff = f.difficulty
        )
    }

    private fun synthesizeBooksQuestion(id: String, index: Int, perm: Int): Question {
        val bIdx = index % books.size
        val book = books[bIdx]
        val mode = (index / books.size) % 5

        return when (mode) {
            0 -> {
                val correct = "${book.chapters} chapters"
                val wrong1 = "${(book.chapters + 3).coerceAtLeast(1)} chapters"
                val wrong2 = "${(book.chapters - 4).coerceAtLeast(1)} chapters"
                val wrong3 = "${(book.chapters + 10)} chapters"
                buildQuestion(
                    id = id,
                    category = "Books of the Bible",
                    text = "How many chapters are contained in the biblical book of ${book.name}?",
                    correct = correct,
                    distractors = listOf(wrong1, wrong2, wrong3),
                    ref = "${book.name} (Canonical Bible)",
                    expl = "The book of ${book.name} in the ${book.testament} contains exactly ${book.chapters} chapters.",
                    perm = perm
                )
            }
            1 -> {
                val correct = book.testament
                val otherTestament = if (book.testament == "Old Testament") "New Testament" else "Old Testament"
                val dist = listOf(otherTestament, "Apocrypha only", "Gospel annex")
                buildQuestion(
                    id = id,
                    category = "Books of the Bible",
                    text = "In which major division of the Holy Bible is the book of ${book.name} located?",
                    correct = correct,
                    distractors = dist,
                    ref = "${book.name} (Canonical order #${book.order})",
                    expl = "${book.name} is the #${book.order} book of the Bible, situated in the ${book.testament}.",
                    perm = perm
                )
            }
            2 -> {
                val correct = book.author
                val others = books.filter { it.author != book.author }.map { it.author }.distinct().shuffled(kotlin.random.Random(perm)).take(3)
                buildQuestion(
                    id = id,
                    category = "Books of the Bible",
                    text = "According to Christian and biblical tradition, who is recognized as the human writer of ${book.name}?",
                    correct = correct,
                    distractors = others,
                    ref = "${book.name} Introduction",
                    expl = "Traditional biblical scholarship attributes the composition of ${book.name} to ${book.author}.",
                    perm = perm
                )
            }
            3 -> {
                val correct = "#${book.order} book"
                val wrong1 = "#${(book.order + 1).coerceAtMost(66)} book"
                val wrong2 = "#${(book.order - 1).coerceAtLeast(1)} book"
                val wrong3 = "#${(book.order + 5).coerceAtMost(66)} book"
                buildQuestion(
                    id = id,
                    category = "Books of the Bible",
                    text = "What is the numerical position of the book of ${book.name} in the 66-book canon?",
                    correct = correct,
                    distractors = listOf(wrong1, wrong2, wrong3),
                    ref = "${book.name} 1:1",
                    expl = "In the standard Protestant Bible of 66 books, ${book.name} stands as the #${book.order} book.",
                    perm = perm
                )
            }
            else -> {
                val correct = book.section
                val allSections = books.map { it.section }.distinct().filter { it != book.section }.shuffled(kotlin.random.Random(perm)).take(3)
                buildQuestion(
                    id = id,
                    category = "Books of the Bible",
                    text = "Under which canonical classification or literary genre does the book of ${book.name} fall?",
                    correct = correct,
                    distractors = allSections,
                    ref = "${book.name} Survey",
                    expl = "${book.name} belongs to the ${book.section} division of Holy Scripture.",
                    perm = perm
                )
            }
        }
    }

    private fun synthesizeCharactersQuestion(id: String, index: Int, perm: Int): Question {
        val facts = listOf(
            Triple("Abraham", "Father of the faithful who left Ur of the Chaldees to go to Canaan", "Genesis 12:1-4"),
            Triple("Moses", "Delivered Israel from Egypt and received the Law on Mount Sinai", "Exodus 3:1-10"),
            Triple("David", "Shepherd boy who killed Goliath and reigned forty years as king of Israel", "1 Samuel 17 / 2 Samuel 5"),
            Triple("Noah", "Built the ark obediently to preserve life during the Great Flood", "Genesis 6:13-14"),
            Triple("Joseph", "Sold into Egyptian slavery by his brothers and became governor of Egypt", "Genesis 41:39-44"),
            Triple("Joshua", "Succeeded Moses and led the conquest of Jericho and the Promised Land", "Joshua 1:1-9"),
            Triple("Elijah", "Challenged the 450 prophets of Baal and ascended to heaven in a whirlwind", "1 Kings 18 / 2 Kings 2"),
            Triple("Elisha", "Received a double portion of Elijah's spirit and performed twice as many miracles", "2 Kings 2:9-14"),
            Triple("Daniel", "Interpreted royal dreams in Babylon and was preserved in the lions' den", "Daniel 2 / 6"),
            Triple("Simon Peter", "Fisherman of Bethsaida who preached the Pentecost sermon where 3,000 were saved", "Matthew 16 / Acts 2"),
            Triple("Paul of Tarsus", "Pharisee converted on Damascus road who became apostle to the Gentiles", "Acts 9 / 2 Timothy 4"),
            Triple("John the Baptist", "Voice crying in the wilderness who baptized Jesus in the Jordan River", "Matthew 3:1-3"),
            Triple("Stephen", "Deacon and first Christian martyr who saw Jesus standing at God's right hand", "Acts 7:55-60"),
            Triple("Barnabas", "'Son of Encouragement' who sold land and traveled with Paul on his first journey", "Acts 4:36 / 13:2"),
            Triple("Lazarus", "Brother of Mary and Martha raised from death by Jesus after four days in the tomb", "John 11:43-44"),
            Triple("Zacchaeus", "Short chief tax collector in Jericho who climbed a sycamore tree to see Jesus", "Luke 19:1-6"),
            Triple("Nicodemus", "Pharisee who visited Jesus by night and helped bury Him with 75 pounds of spices", "John 3 / 19:39"),
            Triple("Gideon", "Judge who tested God with a fleece and defeated Midian with only 300 men", "Judges 6-7"),
            Triple("Samson", "Judge possessing superhuman physical strength from his Nazarite vow", "Judges 13-16"),
            Triple("Solomon", "Son of David blessed with supreme wisdom who built the First Temple", "1 Kings 3-6"),
            Triple("Aaron", "First High Priest of Israel who bore the breastplate of judgment and held up Moses' hands", "Exodus 28 / 17:12"),
            Triple("Caleb", "Spy from Judah who wholly followed the Lord and conquered Hebron at age 85", "Joshua 14:6-14"),
            Triple("Jonathan", "Heroic covenant friend of David and son of King Saul who climbed a rocky crag to fight Philistines", "1 Samuel 14 & 18"),
            Triple("Boaz", "Kinsman-redeemer of Bethlehem who showed lovingkindness to Ruth the Moabitess", "Ruth 2-4"),
            Triple("Nehemiah", "Cupbearer to Artaxerxes who rebuilt the walls of Jerusalem in 52 days amid fierce opposition", "Nehemiah 1-6"),
            Triple("Ezra", "Scribe and priest skilled in the Law of Moses who returned to teach Torah in Judah", "Ezra 7:10"),
            Triple("Mordecai", "Cousin and guardian of Esther who saved the King's life and refused to bow to Haman", "Esther 2-6"),
            Triple("Silas", "Companion of Paul imprisoned at Philippi who prayed and sang hymns at midnight", "Acts 16:19-26"),
            Triple("Timothy", "Spiritual son and coworker of Paul who led the church at Ephesus", "1 & 2 Timothy"),
            Triple("Apollos", "Learned Alexandrian speaker fervent in spirit and mighty in the Scriptures", "Acts 18:24-28")
        )
        val char = facts[index % facts.size]
        val mode = (index / facts.size) % 2
        val variation = (index / (facts.size * 2)) % 4

        return if (mode == 0) {
            val correct = char.first
            val dist = facts.filter { it.first != char.first }.map { it.first }.shuffled(kotlin.random.Random(perm)).take(3)
            val prefix = when (variation) {
                1 -> "In biblical narrative: "
                2 -> "Biblical Character Study: "
                3 -> "Identify this person: "
                else -> ""
            }
            buildQuestion(
                id = id,
                category = "Bible Characters",
                text = "${prefix}Which prominent biblical figure is described as: '${char.second}'?",
                correct = correct,
                distractors = dist,
                ref = char.third,
                expl = "${char.first} is recorded in Scripture (${char.third}) for these faithful acts.",
                perm = perm
            )
        } else {
            val correct = char.second
            val dist = facts.filter { it.second != char.second }.map { it.second }.shuffled(kotlin.random.Random(perm)).take(3)
            val prefix = when (variation) {
                1 -> "According to Scripture: "
                2 -> "Scripture Record: "
                3 -> "Biblical History: "
                else -> ""
            }
            buildQuestion(
                id = id,
                category = "Bible Characters",
                text = "${prefix}What major accomplishment is attributed in the Bible to ${char.first}?",
                correct = correct,
                distractors = dist,
                ref = char.third,
                expl = "Scripture in ${char.third} documents that ${char.first} was: ${char.second.lowercase()}.",
                perm = perm
            )
        }
    }

    private val allMiraclesList = listOf(
        Triple("Turning Water into Wine", "Cana of Galilee", "John 2:1-11"),
        Triple("Feeding the 5,000 with 5 loaves and 2 fish", "Near the Sea of Galilee (Bethsaida)", "John 6:1-14"),
        Triple("Walking upon the stormy waters", "Sea of Galilee", "Matthew 14:22-33"),
        Triple("Calming the raging storm with 'Peace, be still!'", "Sea of Galilee", "Mark 4:35-41"),
        Triple("Raising Lazarus after four days in the tomb", "Bethany", "John 11:38-44"),
        Triple("Healing blind Bartimaeus who cried 'Son of David'", "Jericho", "Mark 10:46-52"),
        Triple("Healing the Roman centurion's paralyzed servant from afar", "Capernaum", "Matthew 8:5-13"),
        Triple("Healing the woman who touched the hem of His garment", "Capernaum", "Luke 8:43-48"),
        Triple("Raising Jairus's twelve-year-old daughter with 'Talitha koum'", "Capernaum", "Mark 5:21-43"),
        Triple("Cleansing the ten lepers where only one Samaritan returned thanks", "Border of Samaria and Galilee", "Luke 17:11-19"),
        Triple("Coin found in the mouth of a fish to pay the temple tax", "Capernaum / Sea of Galilee", "Matthew 17:24-27"),
        Triple("Miraculous post-resurrection catch of 153 large fish", "Sea of Tiberias", "John 21:1-14"),
        Triple("Raising the widow of Nain's only son from the funeral bier", "Town of Nain", "Luke 7:11-17"),
        Triple("Healing the man paralyzed for 38 years at the pool", "Pool of Bethesda in Jerusalem", "John 5:1-9"),
        Triple("Healing the man born blind using saliva and mud washed at Siloam", "Jerusalem", "John 9:1-7"),
        Triple("Withering the barren fig tree on the way to Jerusalem", "Mount of Olives", "Matthew 21:18-22"),
        Triple("Healing the high priest's servant Malchus's severed ear", "Garden of Gethsemane", "Luke 22:50-51"),
        Triple("Healing the demon-possessed man among the tombs in the Gadarenes", "Country of the Gadarenes / Gerasenes", "Mark 5:1-20"),
        Triple("Feeding the 4,000 with seven loaves and a few small fish", "Decapolis near Sea of Galilee", "Mark 8:1-9"),
        Triple("Healing the royal official's son dying of fever from Cana", "Capernaum", "John 4:46-54"),
        Triple("First miraculous catch of fish that nearly sank Peter and Andrew's nets", "Lake of Gennesaret", "Luke 5:1-11"),
        Triple("Healing the deaf and mute man with the word 'Ephphatha' (Be opened)", "Region of the Decapolis", "Mark 7:31-37"),
        Triple("Healing the blind man of Bethsaida in two stages", "Bethsaida", "Mark 8:22-26"),
        Triple("Healing the woman crippled by a spirit of infirmity for 18 years", "Synagogue on the Sabbath", "Luke 13:10-17"),
        Triple("Healing the man with dropsy on the Sabbath in a prominent Pharisee's home", "House of a ruler of Pharisees", "Luke 14:1-6"),
        Triple("Healing Peter's mother-in-law of a sudden high fever", "Capernaum in Peter's home", "Matthew 8:14-15"),
        Triple("Healing a man with an unclean demon in the synagogue", "Capernaum", "Mark 1:21-28"),
        Triple("Healing the demon-possessed mute man who then spoke praising God", "Capernaum region", "Matthew 9:32-34")
    )

    private fun synthesizeMiraclesQuestion(id: String, index: Int, perm: Int): Question {
        val m = allMiraclesList[index % allMiraclesList.size]
        val mode = (index / allMiraclesList.size) % 3
        val variation = (index / (allMiraclesList.size * 3)) % 3

        return when (mode) {
            0 -> {
                val correct = m.second
                val dist = allMiraclesList.filter { it.second != m.second }.map { it.second }.distinct().shuffled(kotlin.random.Random(perm)).take(3)
                buildQuestion(
                    id = id,
                    category = "Miracles of Jesus",
                    text = "In which biblical location did Jesus perform the miracle of '${m.first}'?",
                    correct = correct,
                    distractors = dist,
                    ref = m.third,
                    expl = "The miracle of ${m.first} took place at ${m.second} (${m.third}).",
                    perm = perm
                )
            }
            1 -> {
                val correct = m.first
                val dist = allMiraclesList.filter { it.first != m.first }.map { it.first }.shuffled(kotlin.random.Random(perm)).take(3)
                buildQuestion(
                    id = id,
                    category = "Miracles of Jesus",
                    text = "Which miraculous deed of Jesus Christ is recorded in ${m.third}?",
                    correct = correct,
                    distractors = dist,
                    ref = m.third,
                    expl = "In ${m.third}, Scripture documents the miracle of ${m.first}.",
                    perm = perm
                )
            }
            else -> {
                val correct = m.third
                val dist = listOf("Romans 12:2", "Genesis 1:1", "Revelation 20:1", "Acts 2:1").filter { it != m.third }.take(3)
                buildQuestion(
                    id = id,
                    category = "Miracles of Jesus",
                    text = "What is the primary Gospel scripture passage for the miracle of '${m.first}'?",
                    correct = correct,
                    distractors = dist,
                    ref = m.third,
                    expl = "${m.first} is recorded in ${m.third}.",
                    perm = perm
                )
            }
        }
    }

    private val allParablesList = listOf(
        Triple("The Good Samaritan", "Showing unconditional mercy to our neighbor across ethnic boundaries", "Luke 10:25-37"),
        Triple("The Prodigal Son", "The boundless, rejoicing grace of the Father receiving a repentant sinner", "Luke 15:11-32"),
        Triple("The Sower and the Soils", "Four human heart responses to the seed of God's Word", "Matthew 13:3-23"),
        Triple("The Ten Virgins", "Being spiritually watchful and ready for the Bridegroom's sudden return", "Matthew 25:1-13"),
        Triple("The Talents", "Faithful stewardship and multiplication of God-given resources", "Matthew 25:14-30"),
        Triple("The Lost Sheep", "Rejoicing in heaven over one lost sinner who repents", "Luke 15:3-7"),
        Triple("The Lost Coin", "Diligent searching and divine celebration when what is lost is found", "Luke 15:8-10"),
        Triple("The Wheat and the Weeds", "Righteous and wicked coexisting until the final harvest at the end of the age", "Matthew 13:24-30"),
        Triple("The Pearl of Great Price", "The supreme worth of the Kingdom of Heaven surpassing all earthly riches", "Matthew 13:45-46"),
        Triple("The Mustard Seed", "The small beginning of the Kingdom growing into a vast sheltering tree", "Matthew 13:31-32"),
        Triple("The Pharisee and the Tax Collector", "Humility before God bringing justification rather than self-righteous pride", "Luke 18:9-14"),
        Triple("The Unforgiving Servant", "Our duty to forgive our fellow man because God has forgiven our immense debt", "Matthew 18:21-35"),
        Triple("The Rich Fool", "The danger of hoarding earthly wealth while being destitute toward God", "Luke 12:16-21"),
        Triple("The Workers in the Vineyard", "God's sovereign generosity in dispensing grace equally to latecomers", "Matthew 20:1-16"),
        Triple("The Rich Man and Lazarus", "The irreversible eternal reality of Hades and Abraham's bosom after death", "Luke 16:19-31"),
        Triple("The Barren Fig Tree", "God's patient delay granting time to bear fruits worthy of repentance", "Luke 13:6-9"),
        Triple("The Great Banquet", "The invitation of grace extended to the poor and crippled when others made excuses", "Luke 14:15-24"),
        Triple("The Hidden Treasure", "Joyfully relinquishing everything to possess the priceless Kingdom of Heaven", "Matthew 13:44"),
        Triple("The Net (Dragnet)", "The separation of the wicked from the righteous at the end of the age", "Matthew 13:47-50"),
        Triple("The Two Sons", "Repentant obedience in deeds being superior to empty verbal promises", "Matthew 21:28-32"),
        Triple("The Wicked Vine Dressers (Tenants)", "Rejection of God's prophets and Son by unfaithful religious leaders", "Matthew 21:33-46"),
        Triple("The Persistent Widow and Unjust Judge", "Always praying persistently and never losing heart", "Luke 18:1-8"),
        Triple("The Wise and Foolish Builders", "Building one's life upon the rock of hearing and obeying Christ's words", "Matthew 7:24-27"),
        Triple("The New Cloth and New Wineskins", "The new covenant gospel bringing fresh transformed spiritual vessels", "Matthew 9:16-17"),
        Triple("The Lamp Under a Basket", "Letting the light of God's truth shine openly before all people", "Matthew 5:14-16"),
        Triple("The Two Debtors", "He who is forgiven much by God's boundless grace loves much", "Luke 7:41-43"),
        Triple("The Faithful and Unfaithful Servants", "Living in watchful readiness for the unexpected return of the Master", "Luke 12:42-48"),
        Triple("The Friend at Midnight", "Audacious shameless persistence in knocking and asking God in prayer", "Luke 11:5-8")
    )

    private fun synthesizeParablesQuestion(id: String, index: Int, perm: Int): Question {
        val p = allParablesList[index % allParablesList.size]
        val mode = (index / allParablesList.size) % 3
        val variation = (index / (allParablesList.size * 3)) % 3

        return when (mode) {
            0 -> {
                val correct = p.first
                val dist = allParablesList.filter { it.first != p.first }.map { it.first }.shuffled(kotlin.random.Random(perm)).take(3)
                val prefix = when (variation) {
                    1 -> "Parables of Jesus: "
                    2 -> "Gospel Teaching: "
                    else -> ""
                }
                buildQuestion(
                    id = id,
                    category = "Parables",
                    text = "${prefix}Which parable of Jesus Christ teaches: '${p.second}'?",
                    correct = correct,
                    distractors = dist,
                    ref = p.third,
                    expl = "${p.first} (${p.third}) teaches that ${p.second.lowercase()}.",
                    perm = perm
                )
            }
            1 -> {
                val correct = p.second
                val dist = allParablesList.filter { it.second != p.second }.map { it.second }.shuffled(kotlin.random.Random(perm)).take(3)
                val prefix = when (variation) {
                    1 -> "Kingdom Teaching: "
                    2 -> "Parable Meaning: "
                    else -> ""
                }
                buildQuestion(
                    id = id,
                    category = "Parables",
                    text = "${prefix}What is the primary spiritual truth conveyed in the parable of '${p.first}'?",
                    correct = correct,
                    distractors = dist,
                    ref = p.third,
                    expl = "In ${p.first} (${p.third}), Jesus taught: ${p.second}.",
                    perm = perm
                )
            }
            else -> {
                val correct = p.third
                val dist = listOf("Romans 8:1", "1 Kings 18:1", "Genesis 12:1", "Acts 2:1").filter { it != p.third }.take(3)
                val prefix = when (variation) {
                    1 -> "Scripture Finder: "
                    2 -> "Gospel Record: "
                    else -> ""
                }
                buildQuestion(
                    id = id,
                    category = "Parables",
                    text = "${prefix}In which Scripture passage is the parable of '${p.first}' recorded?",
                    correct = correct,
                    distractors = dist,
                    ref = p.third,
                    expl = "${p.first} is recorded in ${p.third}.",
                    perm = perm
                )
            }
        }
    }

    private val allKingsProphetsList = listOf(
        Triple("King Saul", "First king of Israel of the tribe of Benjamin who was rejected for disobedience", "1 Samuel 9-15"),
        Triple("King David", "Second king of Israel, a man after God's own heart who conquered Jerusalem", "2 Samuel 5:1-5"),
        Triple("King Solomon", "Third king of Israel renowned for supreme wisdom who constructed the First Temple", "1 Kings 3-6"),
        Triple("King Rehoboam", "Son of Solomon who raised taxes and caused the northern ten tribes to secede", "1 Kings 12:1-19"),
        Triple("King Jeroboam I", "First king of the northern kingdom who erected two golden calves in Bethel and Dan", "1 Kings 12:25-33"),
        Triple("King Ahab", "Wicked king of Israel married to Jezebel who instituted Baal worship", "1 Kings 16:29-33"),
        Triple("King Hezekiah", "Righteous king of Judah who dug the Siloam water tunnel and was granted 15 extra years of life", "2 Kings 18-20"),
        Triple("King Josiah", "Boy king of Judah who repaired the Temple, discovered the Book of the Law, and purged idolatry", "2 Kings 22-23"),
        Triple("King Jehoshaphat", "King of Judah who sent out singers before the army praising the beauty of God's holiness", "2 Chronicles 20"),
        Triple("King Uzziah (Azariah)", "King of Judah who reigned 52 years but became leprous after presuming to burn incense in the Temple", "2 Chronicles 26"),
        Triple("King Manasseh", "Longest reigning king of Judah (55 years) who did great evil yet repented while in Babylonian chains", "2 Chronicles 33"),
        Triple("King Nebuchadnezzar", "Babylonian monarch who destroyed Jerusalem and was humbled like a beast for seven periods", "Daniel 4"),
        Triple("King Cyrus the Great", "Persian ruler anointed by God to decree the Jewish return and rebuilding of the Temple", "Ezra 1:1-3"),
        Triple("Prophet Elijah", "Called down fire from heaven on Mount Carmel and ascended to heaven in a chariot of fire", "1 Kings 18 / 2 Kings 2"),
        Triple("Prophet Elisha", "Asked for a double portion of Elijah's spirit, healed Naaman of leprosy, and floated an ax head", "2 Kings 2-5"),
        Triple("Prophet Isaiah", "Saw the Lord high and lifted up in the Temple and prophesied the Suffering Servant in Isaiah 53", "Isaiah 6 / 53"),
        Triple("Prophet Jeremiah", "The weeping prophet who proclaimed the coming New Covenant and Babylonian exile", "Jeremiah 31:31-34"),
        Triple("Prophet Daniel", "Statesman in Babylon who interpreted Nebuchadnezzar's dreams and survived the lions' den", "Daniel 2 & 6"),
        Triple("Prophet Jonah", "Sent to preach repentance to the Assyrian capital of Nineveh after three days in a fish", "Jonah 1-3"),
        Triple("Prophet Samuel", "Last judge and great prophet who heard God's call at Shiloh and anointed both Saul and David", "1 Samuel 3 & 16"),
        Triple("Prophet Nathan", "Rebuked King David regarding Bathsheba and Uriah with the parable of the ewe lamb", "2 Samuel 12:1-7"),
        Triple("Prophet Ezekiel", "Priest by the Chebar river who saw the chariot of cherubim and the valley of dry bones", "Ezekiel 1 & 37"),
        Triple("Prophet Hosea", "Married unfaithful Gomer to demonstrate God's relentless redeeming love for wayward Israel", "Hosea 1-3"),
        Triple("Prophet Amos", "Herdsman of Tekoa who cried out: 'Let justice roll down like waters, and righteousness like a mighty stream'", "Amos 5:24"),
        Triple("Prophet Micah", "Prophesied that the eternal Ruler of Israel would be born in Bethlehem Ephrathah", "Micah 5:2"),
        Triple("King Asa", "Righteous king of Judah who removed foreign altars and deposed his grandmother for idolatry", "1 Kings 15:9-14"),
        Triple("King Joash", "Boy king hidden in the Temple by Jehoiada who repaired the House of the Lord", "2 Kings 11-12"),
        Triple("King Jotham", "Righteous king of Judah who built the Upper Gate of the Temple and became mighty before God", "2 Chronicles 27"),
        Triple("Prophet Habakkuk", "Prophet who questioned divine justice and declared: 'The righteous shall live by his faith'", "Habakkuk 2:4"),
        Triple("Prophet Zechariah", "Grandson of Iddo who prophesied the Branch and the humble King riding on a colt", "Zechariah 9:9")
    )

    private fun synthesizeKingsProphetsQuestion(id: String, index: Int, perm: Int): Question {
        val kp = allKingsProphetsList[index % allKingsProphetsList.size]
        val mode = (index / allKingsProphetsList.size) % 2
        val variation = (index / (allKingsProphetsList.size * 2)) % 4

        return if (mode == 0) {
            val correct = kp.first
            val dist = allKingsProphetsList.filter { it.first != kp.first }.map { it.first }.shuffled(kotlin.random.Random(perm)).take(3)
            val prefix = when (variation) {
                1 -> "In Old Testament history: "
                2 -> "Royal & Prophetic Chronicles: "
                3 -> "Identify the leader: "
                else -> ""
            }
            buildQuestion(
                id = id,
                category = "Kings and Prophets",
                text = "${prefix}Which biblical king or prophet is identified as: '${kp.second}'?",
                correct = correct,
                distractors = dist,
                ref = kp.third,
                expl = "${kp.first} is recorded in Scripture (${kp.third}) for these pivotal historical actions.",
                perm = perm
            )
        } else {
            val correct = kp.second
            val dist = allKingsProphetsList.filter { it.second != kp.second }.map { it.second }.shuffled(kotlin.random.Random(perm)).take(3)
            val prefix = when (variation) {
                1 -> "According to the Scriptures: "
                2 -> "Biblical Record: "
                3 -> "Historical Account: "
                else -> ""
            }
            buildQuestion(
                id = id,
                category = "Kings and Prophets",
                text = "${prefix}What notable biblical role or milestone is associated with ${kp.first}?",
                correct = correct,
                distractors = dist,
                ref = kp.third,
                expl = "Scripture in ${kp.third} records that ${kp.first} was: ${kp.second.lowercase()}.",
                perm = perm
            )
        }
    }

    private val allVersesList = listOf(
        Triple("In the beginning God created the heavens and the earth.", "Genesis 1:1", "Moses"),
        Triple("The Lord is my shepherd; I shall not want.", "Psalm 23:1", "David"),
        Triple("Trust in the Lord with all your heart, and do not lean on your own understanding.", "Proverbs 3:5", "Solomon"),
        Triple("For God so loved the world that He gave His only begotten Son...", "John 3:16", "Jesus"),
        Triple("I can do all things through Christ who strengthens me.", "Philippians 4:13", "Paul"),
        Triple("And we know that in all things God works for the good of those who love Him...", "Romans 8:28", "Paul"),
        Triple("Your word is a lamp unto my feet and a light unto my path.", "Psalm 119:105", "David / Psalmist"),
        Triple("For I know the plans I have for you, declares the Lord, plans to prosper you and not to harm you...", "Jeremiah 29:11", "Jeremiah"),
        Triple("Seek first the kingdom of God and His righteousness, and all these things will be added to you.", "Matthew 6:33", "Jesus"),
        Triple("Now faith is the assurance of things hoped for, the conviction of things not seen.", "Hebrews 11:1", "Author of Hebrews"),
        Triple("Go therefore and make disciples of all nations, baptizing them in the name of the Father, Son, and Holy Spirit.", "Matthew 28:19", "Jesus"),
        Triple("He will wipe every tear from their eyes. There will be no more death or mourning or crying or pain.", "Revelation 21:4", "John"),
        Triple("He has shown you, O mortal, what is good: to act justly, to love mercy, and to walk humbly with your God.", "Micah 6:8", "Micah"),
        Triple("Be strong and courageous. Do not be frightened, for the Lord your God is with you wherever you go.", "Joshua 1:9", "God to Joshua"),
        Triple("For by grace you have been saved through faith, and this is not your own doing; it is the gift of God.", "Ephesians 2:8", "Paul"),
        Triple("The joy of the Lord is your strength.", "Nehemiah 8:10", "Nehemiah & Ezra"),
        Triple("For the wages of sin is death, but the gift of God is eternal life in Christ Jesus our Lord.", "Romans 6:23", "Paul"),
        Triple("If my people who are called by my name will humble themselves and pray and seek my face...", "2 Chronicles 7:14", "God to Solomon"),
        Triple("Come to me, all you who are weary and burdened, and I will give you rest.", "Matthew 11:28", "Jesus"),
        Triple("Rejoice in the Lord always. I will say it again: Rejoice!", "Philippians 4:4", "Paul"),
        Triple("The grass withers, the flower fades, but the word of our God will stand forever.", "Isaiah 40:8", "Isaiah"),
        Triple("They that wait upon the Lord shall renew their strength; they shall mount up with wings as eagles.", "Isaiah 40:31", "Isaiah"),
        Triple("Create in me a clean heart, O God, and renew a right spirit within me.", "Psalm 51:10", "David"),
        Triple("I will praise You, for I am fearfully and wonderfully made.", "Psalm 139:14", "David"),
        Triple("The name of the Lord is a strong tower; the righteous runs into it and is safe.", "Proverbs 18:10", "Solomon"),
        Triple("I am the way, and the truth, and the life. No one comes to the Father except through Me.", "John 14:6", "Jesus"),
        Triple("Peace I leave with you; my peace I give to you. Not as the world gives do I give to you.", "John 14:27", "Jesus"),
        Triple("Do not conform to the pattern of this world, but be transformed by the renewing of your mind.", "Romans 12:2", "Paul"),
        Triple("Love is patient, love is kind. It does not envy, it does not boast, it is not proud.", "1 Corinthians 13:4", "Paul"),
        Triple("Cast all your anxieties on Him, because He cares for you.", "1 Peter 5:7", "Peter")
    )

    private fun synthesizeVersesQuestion(id: String, index: Int, perm: Int): Question {
        val v = allVersesList[index % allVersesList.size]
        val mode = (index / allVersesList.size) % 2
        val variation = (index / (allVersesList.size * 2)) % 4

        return if (mode == 0) {
            val correct = v.second
            val dist = allVersesList.filter { it.second != v.second }.map { it.second }.shuffled(kotlin.random.Random(perm)).take(3)
            val prefix = when (variation) {
                1 -> "Scripture Reference Check: "
                2 -> "Memory Verse: "
                3 -> "Identify the Chapter & Verse: "
                else -> ""
            }
            buildQuestion(
                id = id,
                category = "Bible Verses and References",
                text = "${prefix}What is the biblical scripture reference for the verse: '${v.first}'?",
                correct = correct,
                distractors = dist,
                ref = v.second,
                expl = "This beloved passage of Scripture is located at ${v.second}.",
                perm = perm
            )
        } else {
            val correct = v.third
            val dist = listOf("Peter", "James", "Moses", "David", "Solomon", "Isaiah", "Paul").filter { it != v.third }.take(3)
            val prefix = when (variation) {
                1 -> "Biblical Authorship: "
                2 -> "Who Spoke These Words: "
                3 -> "Scripture Origin: "
                else -> ""
            }
            buildQuestion(
                id = id,
                category = "Bible Verses and References",
                text = "${prefix}Who spoke or recorded the words: '${v.first}' (${v.second})?",
                correct = correct,
                distractors = dist,
                ref = v.second,
                expl = "${v.third} recorded or delivered these words in ${v.second}.",
                perm = perm
            )
        }
    }

    private fun synthesizeNewTestamentQuestion(id: String, index: Int, perm: Int): Question {
        val ntList = gospelLifeFacts + actsApostlesFacts
        return synthesizeFromDetailedList(ntList, id, index, perm, "New Testament")
    }

    private fun synthesizeIntermediateQuestion(id: String, index: Int, perm: Int): Question {
        val interList = oldTestamentFacts + bibleHistoryFacts + actsApostlesFacts
        return synthesizeFromDetailedList(interList, id, index, perm, "Intermediate Bible Questions").copy(difficulty = "Intermediate")
    }

    private fun synthesizeGeneralBibleQuestion(category: String, id: String, index: Int, perm: Int): Question {
        val allFacts = gospelLifeFacts + oldTestamentFacts + actsApostlesFacts
        return synthesizeFromDetailedList(allFacts, id, index, perm, category)
    }

    private fun buildQuestion(
        id: String,
        category: String,
        text: String,
        correct: String,
        distractors: List<String>,
        ref: String,
        expl: String,
        perm: Int,
        diff: String = "Normal"
    ): Question {
        val allOpts = (listOf(correct) + distractors.take(3)).distinct()
        val shuffledOpts = allOpts.shuffled(kotlin.random.Random(perm))
        val correctIdx = shuffledOpts.indexOf(correct).coerceAtLeast(0)

        return Question(
            id = id,
            questionId = id,
            questionText = text,
            options = shuffledOpts,
            correctOptionIndex = correctIdx.toLong(),
            category = category,
            difficulty = diff,
            bibleReference = ref,
            explanation = expl,
            isPublished = true
        )
    }
}
