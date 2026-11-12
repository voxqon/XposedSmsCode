package io.github.magisk317.uikit.shell

open class RecordItem<T>(
    val item: T,
) {
    var isSelected: Boolean = false

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RecordItem<*>) return false
        return item == other.item
    }

    override fun hashCode(): Int = item?.hashCode() ?: 0
}
