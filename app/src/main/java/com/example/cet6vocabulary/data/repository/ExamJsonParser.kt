package com.example.cet6vocabulary.data.repository

import com.example.cet6vocabulary.data.model.Exam
import com.example.cet6vocabulary.data.model.ExamAppConfig
import com.example.cet6vocabulary.data.model.ExamMaterials
import com.example.cet6vocabulary.data.model.ExamPassage
import com.example.cet6vocabulary.data.model.ExamQuestion
import com.example.cet6vocabulary.data.model.ExamSection
import com.example.cet6vocabulary.data.model.ExamSectionData
import com.example.cet6vocabulary.data.model.ExamSourceVerification
import com.example.cet6vocabulary.data.model.ExamSubsection
import org.json.JSONArray
import org.json.JSONObject

class ExamDataException(message: String, cause: Throwable? = null) : IllegalStateException(message, cause)

class ExamJsonParser {
    fun parse(json: String, assetName: String): Exam {
        val root = try {
            JSONObject(json)
        } catch (error: Exception) {
            throw ExamDataException("Failed to parse exam JSON '$assetName': ${error.message}", error)
        }
        val examId = root.requiredString("examId", assetName)
        val setNumber = root.requiredInt("set", assetName)
        val sectionsObject = root.requiredObject("sections", assetName)
        val sections = listOf("writing", "listening", "reading", "translation").map { key ->
            if (!sectionsObject.has(key) || sectionsObject.isNull(key)) {
                throw ExamDataException("$assetName: sections.$key is missing")
            }
            when (key) {
                "reading" -> parseReadingSection(sectionsObject.getJSONObject(key), examId, assetName)
                else -> parseSimpleSection(sectionsObject.getJSONObject(key), examId, sectionForKey(key), assetName)
            }
        }
        return Exam(
            schemaVersion = root.requiredString("schemaVersion", assetName),
            examId = examId,
            title = root.requiredString("title", assetName),
            date = root.requiredString("date", assetName),
            setNumber = setNumber,
            setName = "set$setNumber",
            sections = sections,
            answerKey = parseAnswerKey(root.optJSONObject("answerKey"), assetName),
            appConfig = parseAppConfig(root.optJSONObject("appConfig"), assetName),
            sourceVerification = parseSourceVerification(root.optJSONObject("sourceVerification"), assetName)
        )
    }

    private fun parseSimpleSection(
        sectionObject: JSONObject,
        examId: String,
        section: ExamSection,
        assetName: String
    ): ExamSectionData {
        return ExamSectionData(
            section = section,
            type = sectionObject.requiredString("type", assetName),
            title = sectionObject.requiredString("title", assetName),
            durationMinutes = sectionObject.requiredInt("durationMinutes", assetName),
            questions = parseQuestions(sectionObject.requiredArray("questions", assetName), examId, section, null, assetName),
            audioRef = sectionObject.optionalString("audioRef")
        )
    }

    private fun parseReadingSection(
        sectionObject: JSONObject,
        examId: String,
        assetName: String
    ): ExamSectionData {
        val subsectionObject = sectionObject.requiredObject("sections", assetName)
        val subsections = listOf("sectionA", "sectionB", "sectionC").map { key ->
            if (!subsectionObject.has(key) || subsectionObject.isNull(key)) {
                throw ExamDataException("$assetName: sections.reading.sections.$key is missing")
            }
            parseReadingSubsection(subsectionObject.getJSONObject(key), key, examId, assetName)
        }
        return ExamSectionData(
            section = ExamSection.READING,
            type = sectionObject.requiredString("type", assetName),
            title = sectionObject.requiredString("title", assetName),
            durationMinutes = sectionObject.requiredInt("durationMinutes", assetName),
            questions = subsections.flatMap { it.questions },
            subsections = subsections,
            audioRef = sectionObject.optionalString("audioRef")
        )
    }

    private fun parseReadingSubsection(
        subsectionObject: JSONObject,
        key: String,
        examId: String,
        assetName: String
    ): ExamSubsection {
        val materials = when (key) {
            "sectionA" -> ExamMaterials(
                passage = subsectionObject.requiredString("passage", assetName),
                wordBank = subsectionObject.requiredStringMap("wordBank", assetName)
            )
            "sectionB" -> ExamMaterials(
                passageTitle = subsectionObject.requiredString("passageTitle", assetName),
                paragraphs = subsectionObject.requiredStringMap("paragraphs", assetName)
            )
            "sectionC" -> ExamMaterials(
                passages = parsePassages(subsectionObject.requiredArray("passages", assetName), assetName)
            )
            else -> throw ExamDataException("$assetName: unsupported reading subsection '$key'")
        }
        return ExamSubsection(
            key = key,
            type = subsectionObject.requiredString("type", assetName),
            title = subsectionObject.requiredString("title", assetName),
            materials = materials,
            questions = parseQuestions(
                subsectionObject.requiredArray("questions", assetName),
                examId,
                ExamSection.READING,
                key,
                assetName
            )
        )
    }

    private fun parseQuestions(
        array: JSONArray,
        examId: String,
        section: ExamSection,
        subsection: String?,
        assetName: String
    ): List<ExamQuestion> = (0 until array.length()).map { index ->
        val questionObject = array.optJSONObject(index)
            ?: throw ExamDataException("$assetName: question at index $index is not an object")
        ExamQuestion(
            questionId = questionObject.requiredString("id", assetName),
            examId = examId,
            section = section,
            subsection = subsection,
            number = questionObject.requiredInt("number", assetName),
            type = questionObject.optionalString("type"),
            question = questionObject.optionalString("question"),
            options = questionObject.optionalObject("options")?.toStringMap() ?: emptyMap(),
            correctAnswer = questionObject.optionalString("correctAnswer"),
            explanation = questionObject.optionalString("explanation"),
            prompt = questionObject.optionalString("prompt"),
            instruction = questionObject.optionalString("instruction"),
            minWords = questionObject.optionalInt("minWords"),
            maxWords = questionObject.optionalInt("maxWords"),
            blank = questionObject.optionalInt("blank"),
            passageId = questionObject.optionalString("passageId"),
            audioRef = questionObject.optionalString("audioRef")
        )
    }

    private fun parsePassages(array: JSONArray, assetName: String): List<ExamPassage> = (0 until array.length()).map { index ->
        val passage = array.optJSONObject(index)
            ?: throw ExamDataException("$assetName: passage at index $index is not an object")
        ExamPassage(
            id = passage.requiredString("id", assetName),
            title = passage.requiredString("title", assetName),
            text = passage.requiredString("text", assetName)
        )
    }

    private fun parseAnswerKey(answerKeyObject: JSONObject?, assetName: String): Map<ExamSection, Map<Int, String>> {
        if (answerKeyObject == null) return emptyMap()
        return answerKeyObject.keys().asSequence().sorted().associate { key ->
            val sectionAnswers = answerKeyObject.requiredObject(key, assetName)
            val answers = sectionAnswers.keys().asSequence()
                .sortedBy { it.toIntOrNull() ?: Int.MAX_VALUE }
                .associate { number ->
                    val parsedNumber = number.toIntOrNull()
                        ?: throw ExamDataException("$assetName: answerKey.$key contains invalid question number '$number'")
                    parsedNumber to sectionAnswers.requiredString(number, assetName)
                }
            sectionForKey(key) to answers
        }
    }

    private fun parseAppConfig(config: JSONObject?, assetName: String): ExamAppConfig? {
        if (config == null) return null
        return ExamAppConfig(
            singleChoiceScoring = config.requiredInt("singleChoiceScoring", assetName),
            wordBankScoring = config.requiredInt("wordBankScoring", assetName),
            paragraphMatchingScoring = config.requiredInt("paragraphMatchingScoring", assetName),
            showAnswerAfterSubmit = config.requiredBoolean("showAnswerAfterSubmit", assetName),
            recordWrongAnswers = config.requiredBoolean("recordWrongAnswers", assetName),
            allowRetry = config.requiredBoolean("allowRetry", assetName)
        )
    }

    private fun parseSourceVerification(verification: JSONObject?, assetName: String): ExamSourceVerification? {
        if (verification == null) return null
        val corrections = verification.requiredArray("notableCorrections", assetName)
        return ExamSourceVerification(
            source = verification.requiredString("source", assetName),
            scope = verification.requiredString("scope", assetName),
            method = verification.requiredString("method", assetName),
            notableCorrections = (0 until corrections.length()).map { index -> corrections.getString(index) },
            answerKeyVerified = verification.requiredBoolean("answerKeyVerified", assetName)
        )
    }

    private fun sectionForKey(key: String): ExamSection = when (key) {
        "writing" -> ExamSection.WRITING
        "listening" -> ExamSection.LISTENING
        "reading" -> ExamSection.READING
        "translation" -> ExamSection.TRANSLATION
        else -> throw ExamDataException("Unsupported exam section '$key'")
    }
}

private fun JSONObject.requiredString(name: String, assetName: String): String {
    if (!has(name) || isNull(name)) throw ExamDataException("$assetName: missing required field '$name'")
    return try {
        getString(name)
    } catch (error: Exception) {
        throw ExamDataException("$assetName: field '$name' must be a string", error)
    }
}

private fun JSONObject.optionalString(name: String): String? = if (!has(name) || isNull(name)) null else getString(name)

private fun JSONObject.requiredInt(name: String, assetName: String): Int {
    if (!has(name) || isNull(name)) throw ExamDataException("$assetName: missing required field '$name'")
    return try {
        getInt(name)
    } catch (error: Exception) {
        throw ExamDataException("$assetName: field '$name' must be an integer", error)
    }
}

private fun JSONObject.optionalInt(name: String): Int? = if (!has(name) || isNull(name)) null else getInt(name)

private fun JSONObject.requiredBoolean(name: String, assetName: String): Boolean {
    if (!has(name) || isNull(name)) throw ExamDataException("$assetName: missing required field '$name'")
    return try {
        getBoolean(name)
    } catch (error: Exception) {
        throw ExamDataException("$assetName: field '$name' must be a boolean", error)
    }
}

private fun JSONObject.requiredObject(name: String, assetName: String): JSONObject {
    if (!has(name) || isNull(name)) throw ExamDataException("$assetName: missing required object '$name'")
    return try {
        getJSONObject(name)
    } catch (error: Exception) {
        throw ExamDataException("$assetName: field '$name' must be an object", error)
    }
}

private fun JSONObject.optionalObject(name: String): JSONObject? = if (!has(name) || isNull(name)) null else getJSONObject(name)

private fun JSONObject.requiredArray(name: String, assetName: String): JSONArray {
    if (!has(name) || isNull(name)) throw ExamDataException("$assetName: missing required array '$name'")
    return try {
        getJSONArray(name)
    } catch (error: Exception) {
        throw ExamDataException("$assetName: field '$name' must be an array", error)
    }
}

private fun JSONObject.requiredStringMap(name: String, assetName: String): Map<String, String> =
    requiredObject(name, assetName).toStringMap()

private fun JSONObject.toStringMap(): Map<String, String> = keys().asSequence().sorted().associateWith { key ->
    getString(key)
}
