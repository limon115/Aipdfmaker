package com.example.domain.models.document

import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DocumentModelTest {

    private val jsonFormat = Json {
        ignoreUnknownKeys = true
        classDiscriminator = "type"
        isLenient = true
    }

    @Test
    fun `test parsing chemistry JSON schema into Document models`() {
        val chemistryJson = """
            {
              "schemaVersion": 1,
              "title": "Chemical Reactions",
              "author": "AI Assistant",
              "language": "en",
              "blocks": [
                {
                  "type": "heading",
                  "level": 1,
                  "text": "Introduction to Chemistry"
                },
                {
                  "type": "paragraph",
                  "text": "Water is formed by the reaction of hydrogen and oxygen."
                },
                {
                  "type": "equation",
                  "latex": "2H_2 + O_2 \\rightarrow 2H_2O",
                  "display": true
                },
                {
                  "type": "bullet_list",
                  "items": ["Hydrogen", "Oxygen", "Water"]
                },
                {
                  "type": "table",
                  "columns": ["Element", "Symbol", "Atomic Number"],
                  "rows": [
                    ["Hydrogen", "H", "1"],
                    ["Oxygen", "O", "8"]
                  ]
                }
              ]
            }
        """.trimIndent()

        val document = jsonFormat.decodeFromString<Document>(chemistryJson)

        assertEquals(1, document.schemaVersion)
        assertEquals("Chemical Reactions", document.title)
        assertEquals("AI Assistant", document.author)
        assertEquals("en", document.language)
        assertEquals(5, document.blocks.size)

        val headingBlock = document.blocks[0] as HeadingBlock
        assertEquals(1, headingBlock.level)
        assertEquals("Introduction to Chemistry", headingBlock.text)

        val paragraphBlock = document.blocks[1] as ParagraphBlock
        assertEquals("Water is formed by the reaction of hydrogen and oxygen.", paragraphBlock.text)

        val equationBlock = document.blocks[2] as EquationBlock
        assertEquals("2H_2 + O_2 \\rightarrow 2H_2O", equationBlock.latex)
        assertTrue(equationBlock.display)

        val bulletListBlock = document.blocks[3] as BulletListBlock
        assertEquals(3, bulletListBlock.items.size)
        assertEquals("Oxygen", bulletListBlock.items[1])

        val tableBlock = document.blocks[4] as TableBlock
        assertEquals(3, tableBlock.columns.size)
        assertEquals(2, tableBlock.rows.size)
        assertEquals("Hydrogen", tableBlock.rows[0][0])
    }
}
