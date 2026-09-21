package org.solidhax.shiro.features

@ConsistentCopyVisibility
data class Category private constructor(val name: String) {
    companion object {
        val categories: LinkedHashMap<String, Category> = linkedMapOf()

        @JvmField
        val GENERAL = custom("General")
        @JvmField
        val RENDER = custom("Render")
        @JvmField
        val MISC = custom("Misc")

        fun custom(name: String): Category {
            return categories.getOrPut(name) { Category(name) }
        }
    }
}
