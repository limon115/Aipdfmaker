package com.example.domain.models.document

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Document(
    val schemaVersion: Int = 1,
    val title: String = "",
    val author: String = "",
    val language: String = "",
    val blocks: List<DocumentBlock> = emptyList()
)

@Serializable
sealed class DocumentBlock

@Serializable
@SerialName("heading")
data class HeadingBlock(
    val level: Int = 1,
    val text: String = "",
    val content: List<InlineElement>? = null
) : DocumentBlock()

@Serializable
@SerialName("paragraph")
data class ParagraphBlock(
    val text: String = "",
    val content: List<InlineElement>? = null
) : DocumentBlock()

@Serializable
@SerialName("equation")
data class EquationBlock(
    val latex: String,
    val display: Boolean = true
) : DocumentBlock()

@Serializable
@SerialName("bullet_list")
data class BulletListBlock(
    val items: List<String> = emptyList()
) : DocumentBlock()

@Serializable
@SerialName("numbered_list")
data class NumberedListBlock(
    val items: List<String> = emptyList()
) : DocumentBlock()

@Serializable
@SerialName("table")
data class TableBlock(
    val columns: List<String> = emptyList(),
    val rows: List<List<String>> = emptyList()
) : DocumentBlock()

@Serializable
@SerialName("image")
data class ImageBlock(
    val path: String
) : DocumentBlock()

@Serializable
@SerialName("quote")
data class QuoteBlock(
    val text: String = ""
) : DocumentBlock()

@Serializable
@SerialName("page_break")
object PageBreakBlock : DocumentBlock()

@Serializable
sealed class InlineElement

@Serializable
@SerialName("text")
data class TextElement(
    val value: String
) : InlineElement()

@Serializable
@SerialName("inline_math")
data class InlineMathElement(
    val latex: String
) : InlineElement()
