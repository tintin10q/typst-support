package com.github.garetht.typstsupport.structureview

import com.intellij.ide.structureView.StructureViewTreeElement
import com.intellij.ide.util.treeView.smartTree.SortableTreeElement
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.psi.PsiFile

class TypstStructureViewElement(
    private val file: PsiFile,
    private val node: HeadingNode? = null
) : StructureViewTreeElement, SortableTreeElement {

    private val myChildren: Array<StructureViewTreeElement> = if (node == null) {
        TypstStructureParser.parse(file).map { TypstStructureViewElement(file, it) }.toTypedArray()
    } else {
        node.children.map { TypstStructureViewElement(file, it) }.toTypedArray()
    }

    override fun getValue(): Any = node ?: file

    override fun navigate(requestFocus: Boolean) {
        node?.let { OpenFileDescriptor(file.project, file.virtualFile, it.offset).navigate(requestFocus) }
    }

    override fun canNavigate(): Boolean = node != null

    override fun canNavigateToSource(): Boolean = canNavigate()

    override fun getChildren(): Array<StructureViewTreeElement> = myChildren

    override fun getPresentation(): ItemPresentation = object : ItemPresentation {
        override fun getPresentableText(): String? = node?.text ?: file.name
        override fun getLocationString(): String? = null
        override fun getIcon(unused: Boolean) = if (node == null) file.getIcon(0) else null
    }

    override fun getAlphaSortKey(): String = node?.text ?: file.name
}

