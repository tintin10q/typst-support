package com.github.garetht.typstsupport.structureview

import com.intellij.psi.PsiFile

data class HeadingNode(
    val level: Int,
    val text: String,
    val offset: Int,
    val children: MutableList<HeadingNode> = mutableListOf()
)

internal object TypstStructureParser {
    fun parse(file: PsiFile): List<HeadingNode> {
        val text = file.text
        val headings = mutableListOf<Triple<Int, String, Int>>() // level, title, offset

        val eqRegex = Regex("""(?m)^(\=+)\s+(.*)$""")
        for (match in eqRegex.findAll(text)) {
            val level = match.groupValues[1].length
            val title = match.groupValues[2].trim()
            headings += Triple(level, title, match.range.first)
        }

        val macroRegex = Regex("""(?m)^#heading(?:\(([^)]*)\))?\[(.*?)]""")
        for (match in macroRegex.findAll(text)) {
            val options = match.groupValues[1]
            val title = match.groupValues[2].trim()
            val levelMatch = Regex("""level\s*:\s*(\d+)""").find(options)
            val level = levelMatch?.groupValues?.get(1)?.toInt() ?: 1
            headings += Triple(level, title, match.range.first)
        }

        headings.sortBy { it.third }

        val root = HeadingNode(0, "", 0)
        val stack = mutableListOf(root)
        for ((level, title, offset) in headings) {
            while (stack.last().level >= level) {
                stack.removeAt(stack.size - 1)
            }
            val node = HeadingNode(level, title, offset)
            stack.last().children += node
            stack += node
        }
        return root.children
    }
}

