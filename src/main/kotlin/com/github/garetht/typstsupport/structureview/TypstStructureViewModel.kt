package com.github.garetht.typstsupport.structureview

import com.intellij.ide.structureView.StructureViewModelBase
import com.intellij.ide.structureView.StructureViewTreeElement
import com.intellij.psi.PsiFile

class TypstStructureViewModel(psiFile: PsiFile) :
    StructureViewModelBase(psiFile, TypstStructureViewElement(psiFile)) {

    override fun isAlwaysShowsPlus(element: StructureViewTreeElement): Boolean = false

    override fun isAlwaysLeaf(element: StructureViewTreeElement): Boolean =
        element is TypstStructureViewElement && element.children.isEmpty()
}

