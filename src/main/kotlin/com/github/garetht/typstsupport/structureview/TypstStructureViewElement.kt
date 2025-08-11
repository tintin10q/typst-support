package com.github.garetht.typstsupport.structureview

import com.github.garetht.typstsupport.languageserver.models.OutlineItem
import com.intellij.ide.structureView.StructureViewTreeElement
import com.intellij.ide.util.treeView.smartTree.SortableTreeElement
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.psi.PsiFile

class TypstStructureViewElement(
    private val file: PsiFile,
    private val item: OutlineItem? = null
) : StructureViewTreeElement, SortableTreeElement {

    private val myChildren: Array<StructureViewTreeElement> =
        (item?.children ?: TypstStructureParser.parse(file)).map { TypstStructureViewElement(file, it) }
            .toTypedArray()

    override fun getValue(): Any = item ?: file

    override fun navigate(requestFocus: Boolean) {
        if (item != null) {
            val offset = item.span?.substringBefore("..")?.toIntOrNull() ?: 0
            OpenFileDescriptor(file.project, file.virtualFile, offset).navigate(requestFocus)
        }
    }

    override fun canNavigate(): Boolean = item != null

    override fun canNavigateToSource(): Boolean = canNavigate()

    override fun getChildren(): Array<StructureViewTreeElement> = myChildren

    override fun getPresentation(): ItemPresentation = object : ItemPresentation {
        override fun getPresentableText(): String? = item?.title ?: file.name
        override fun getLocationString(): String? = null
        override fun getIcon(unused: Boolean) = if (item == null) file.getIcon(0) else null
    }

    override fun getAlphaSortKey(): String = item?.title ?: file.name
}

