package com.example.testapp

import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import com.example.testapp.ui.theme.TestAppTheme
import kotlinx.coroutines.Job

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TestAppTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    Greeting("@[{user-356,nam}] @[{user-356,tran}] ()($($ vhv @[{user-356,five}]")
                }
            }
        }
    }
}

@Composable
fun Greeting(value: String) {
    val rawValue = remember { mutableStateOf(TextFieldValue(value)) }
    var finalText: AnnotatedString
    var data: MutableList<SplitPart> = remember { mutableStateListOf() }

    val clickableParts =
        Regex.MENTIONING_REGEX.findAll(rawValue.value.text).map {
            MatchedRawClickableString(
                string = it.value,
                startIndexInRawString = it.range.first,
                endIndexInRawStringInclusive = it.range.last
            )
        }
            .toList()
    val toClickableModels = clickableParts.map { it.toClickableModel() }

    if (clickableParts.isEmpty()) {
        finalText = AnnotatedString(value)
    } else {
        var replacedString = rawValue.value.text
        toClickableModels.forEach {
            if (it.type == Constant.CLICKABLE_TYPE_MENTION) { //Replace mention format to normal name
                replacedString = replacedString.replaceFirst(it.originalString, it.displayValue)
            }
        }

        data = splitStringIntoParts(
            replacedString, clickableTextToClickablePosition(replacedString, toClickableModels)
        )
        val clickablePosition: HashMap<String, SplitPart> = hashMapOf()

        finalText = getBuildAnnotatedString(
            data = data,
            clickablePosition = clickablePosition
        )
    }

    TextField(
        placeholder = { Text(text = "Android") },
        value = rawValue.value,
        onValueChange = {
            rawValue.value = it

        },
        colors = TextFieldDefaults.outlinedTextFieldColors(
            unfocusedBorderColor = Color.Transparent,
            focusedBorderColor = Color.Transparent,
            errorBorderColor = Color.Transparent,
            backgroundColor = Color.Transparent,
        ),
        visualTransformation = ClickableTransformation(
            finalText,
            rawValue.value.annotatedString,
            data
        ),
    )
}

class ClickableTransformation(
    private val annotatedString: AnnotatedString,
    private val rawAnnotatedString: AnnotatedString,
    private val splitParts: List<SplitPart>,
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        return TransformedText(
            annotatedString,
            CustomOffsetMapping(rawAnnotatedString, annotatedString, splitParts)
        )
    }
}

class CustomOffsetMapping(
    private val rawFormat: AnnotatedString,
    private val annotatedString: AnnotatedString,
    private val parts: List<SplitPart>,
) : OffsetMapping {
    override fun originalToTransformed(offset: Int): Int {
        return calculateOriginalToTransformed(offset, rawFormat.text)
    }

    override fun transformedToOriginal(offset: Int): Int {
        return calculateTransformedToOriginal(
            offset,
            parts,
            annotatedString,
        )
    }

}

private fun calculateTransformedToOriginal(
    offset: Int,
    parts: List<SplitPart>,
    transformedString: AnnotatedString
): Int {
    var calculatedOffset = 0
    transformedString.getStringAnnotations(0, offset)
        .forEachIndexed { index, range ->
            if (offset in range.start..range.end) {
                //Focusing on it
                parts[index].clickablePosition?.let {
                    val indexOfComma = it.originalString.indexOf(",")
                    val subbedString = it.originalString.substring(0, indexOfComma + 1)
                    calculatedOffset += subbedString.length
                    if (getPrecedingChars(offset, transformedString.text, " ") == range.item) { //cursor at the end of the mention, should append more MENTION_END
                        calculatedOffset += 2
                    }
                }
            } else {
                parts[index].clickablePosition?.let {
                    calculatedOffset += (it.originalString.length - range.item.length)
                }
            }
        }
    return offset + calculatedOffset
}

private fun calculateOriginalToTransformed(
    offset: Int,
    rawText: String
): Int {
    val part = rawText.substring(0, offset)
    var totalOffset = 0
    var lastIndexOfMatches = 0

    Regex(Regex.MENTION).findAll(part).forEach {
        totalOffset += it.value.length - it.value.getDisplayNameFromMentionFormat().length
        lastIndexOfMatches = it.range.first + it.value.length
    }
    if (lastIndexOfMatches < offset) {
        val remainingWord = rawText.substring(lastIndexOfMatches, offset)

        val hasPartOfMention = remainingWord.contains("@[{user-")
        if (hasPartOfMention) {
            val indexOfMention = remainingWord.indexOf("@[{user-")
            val indexOfCommaInMention = remainingWord.indexOf(",")
            if (indexOfMention >= 0 && indexOfCommaInMention >= 0) {
                totalOffset += remainingWord.substring(indexOfMention, indexOfCommaInMention + 1).length
            } else {
                Log.e("TextField","Some how word cannot be found: indexOfMention ($indexOfMention) and indexOfCommaInMention: ${indexOfCommaInMention}")
            }
        }
    }
    return offset - totalOffset
}

data class SplitPart(
    val stringPart: String,
    val isClickable: Boolean,
    val clickablePosition: ClickablePosition?
)
data class MatchedRawClickableString(
    val string: String,
    val startIndexInRawString: Int,
    val endIndexInRawStringInclusive: Int,
)
data class ClickablePosition(
    val start : Int,
    val length: Int,
    val action: String,
    val originalString: String = "",
) {
    override fun toString(): String {
        return "$action | $start - $length"
    }
}

fun String.getDisplayNameFromMentionFormat(): String {
    val commaIndex = this.indexOf(",")
    val curlyBrace = this.indexOf("}")
    return this.substring(commaIndex + 1, curlyBrace)
}

private fun getPrecedingChars(
    cursorIndex: Int,
    fullText: String,
    vararg delimiter: String
): String {
    var preceding = ""
    if (cursorIndex == 0) { //At the start
        preceding = ""
    } else if (cursorIndex == fullText.length) { //At the end
        var checkingIndex = cursorIndex - 1
        do {
            if (fullText[checkingIndex].toString() !in delimiter) {
                preceding = fullText[checkingIndex] + preceding
            } else {
                break
            }
            checkingIndex--
        } while (checkingIndex >= 0)
    } else { //In the middle
        var checkingIndexPreceding = cursorIndex - 1

        preceding@ do {
            if (fullText[checkingIndexPreceding].toString() !in delimiter) {
                preceding = fullText[checkingIndexPreceding] + preceding
            } else {
                break@preceding
            }
            checkingIndexPreceding--
        } while (checkingIndexPreceding >= 0)
    }
    return preceding
}

fun MatchedRawClickableString.toClickableModel(): ClickableModel {
    if (this.string.startsWith("@")) {
        val commaIndex = this.string.indexOf(",")
        val curlyBrace = this.string.indexOf("}")
        val displayValue = this.string.substring(commaIndex + 1, curlyBrace)
        return ClickableModel(
            type = Constant.CLICKABLE_TYPE_MENTION,
            originalString = this.string,
            value = this.string.substring(Constant.START_INDEX_TO_GET_USER_ID, commaIndex),
            displayValue = displayValue,
            charCountBeforeDisplay = this.string.substring(0, commaIndex + 1).length,
            charCountAfterDisplay = this.string.substring(
                this.string.indexOf(displayValue) + displayValue.length,
                this.string.length
            ).length,
            startIndexInRawString = this.startIndexInRawString,
            endIndexInRawStringInclusive = this.endIndexInRawStringInclusive
        )
    } else { //Is URL
        return ClickableModel(
            type = Constant.CLICKABLE_TYPE_URL,
            originalString = this.string,
            value = this.string,
            displayValue = this.string,
            startIndexInRawString = this.startIndexInRawString,
            endIndexInRawStringInclusive = this.endIndexInRawStringInclusive
        )
    }
}

data class ClickableModel(
    val type: String,
    val value: String,
    val displayValue: String,
    val originalString: String,
    val charCountBeforeDisplay: Int = 0,
    val charCountAfterDisplay: Int = 0,
    val startIndexInRawString: Int = -1,
    val endIndexInRawStringInclusive: Int = -1
) {
    fun isMention(): Boolean {
        return type == Constant.CLICKABLE_TYPE_MENTION
    }
    fun isUrl(): Boolean {
        return type == Constant.CLICKABLE_TYPE_URL
    }
}

fun splitStringIntoParts(
    wholeText: String,
    listPosition: List<ClickablePosition>,
): MutableList<SplitPart> {
    val textParts: MutableList<SplitPart> = mutableListOf()

    if (listPosition.isNotEmpty()) {
        var lastCroppedPosition: Int = 0
        var part: String
        listPosition.forEachIndexed { index, item ->

            if (lastCroppedPosition < item.start) {
                part = wholeText.substring(lastCroppedPosition, item.start)
                textParts.add((SplitPart(part, false, null)))
            }
            part = wholeText.substring(item.start, item.start + item.length)
            textParts.add((SplitPart(part, true, item)))
            lastCroppedPosition = item.start + item.length

            if (index == listPosition.lastIndex) {
                if (lastCroppedPosition < wholeText.length) { // Last clickable item, append the rest of the string to the annotation
                    part = wholeText.substring(lastCroppedPosition, wholeText.length)
                    textParts.add((SplitPart(part, false, null)))
                }
            }
        }
    }
    return textParts
}

fun clickableTextToClickablePosition(
    fullText: String,
    clickableList: List<ClickableModel>
): List<ClickablePosition> {
    val results = mutableListOf<ClickablePosition>()
    var lastCheckIndex = 0
    clickableList.forEachIndexed { index, clickableModel ->
        val startIndex = fullText.indexOf(string = clickableModel.displayValue, startIndex = lastCheckIndex)
        lastCheckIndex = startIndex + clickableModel.displayValue.length
        results.add(
            ClickablePosition(
                start = startIndex,
                length = clickableModel.displayValue.length,
                action = if (clickableModel.isMention()) "${Constant.OPEN_PROFILE}-${clickableModel.value}-${clickableModel.displayValue}-$index" else if (clickableModel.isUrl()) "${Constant.OPEN_WEBVIEW}-${clickableModel.value}-${clickableModel.displayValue}-$index" else "",
                originalString = clickableModel.originalString,
            )
        )
    }

    return results
}

fun getBuildAnnotatedString(
    data: MutableList<SplitPart>,
    clickablePosition: HashMap<String, SplitPart> = HashMap(),
): AnnotatedString{
    return buildAnnotatedString {
        data.forEach {
            if (!it.isClickable) {
                pushStringAnnotation(
                    tag = "UN_CLICKABLE_PART",// provide tag which will then be provided when you click the text
                    annotation = it.stringPart
                )
                append(it.stringPart)
                clickablePosition["UN_CLICKABLE_PART"] = it
                pop()
            } else {
                //Start of the pushing annotation which you want to color and make them clickable later
                pushStringAnnotation(
                    tag = it.clickablePosition!!.action,// provide tag which will then be provided when you click the text
                    annotation = it.stringPart
                )
                //add text with your different color/style
                append(it.stringPart)

                clickablePosition[it.clickablePosition!!.action] = it
                // when pop is called it means the end of annotation with current tag
                pop()
            }
        }
    }
}