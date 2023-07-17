package cc.unitmesh.devti.context

import cc.unitmesh.devti.context.base.NamedElementContext
import com.google.gson.Gson
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference

class ClassContext(
    override val root: PsiElement,
    override val text: String?,
    override val name: String?,
    val methods: List<PsiElement>,
    val fields: List<PsiElement>,
    val superClasses: List<String>?,
    val usages: List<PsiReference>
) : NamedElementContext(root, text, name) {
    private fun getFieldNames(): List<String> = fields.mapNotNull {
        VariableContextProvider(false, false, false).from(it).name
    }

    private fun getMethodSignatures(): List<String> = methods.mapNotNull {
        MethodContextProvider(false, gatherUsages = false).from(it).signature
    }

    override fun toQuery(): String {
        val className = name ?: "_"
        val classFields = getFieldNames().joinToString(separator = " ")
        val classMethods = getMethodSignatures().joinToString(separator = "\n")
        return "class name: $className\nclass fields: $classFields\nclass methods: $classMethods\nsuper classes: $superClasses\n"
    }

    override fun toJson(): String = Gson().toJson({
        mapOf(
            "name" to name,
            "methods" to getMethodSignatures(),
            "fields" to getFieldNames()
        )
    }).toString()
}