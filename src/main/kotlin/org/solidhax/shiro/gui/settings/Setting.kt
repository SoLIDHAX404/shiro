package org.solidhax.shiro.gui.settings

import org.solidhax.shiro.features.Module
import org.solidhax.shiro.gui.settings.impl.DropdownSetting
import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

abstract class Setting<T>(
    val name: String,
    var description: String = "",
) : ReadWriteProperty<Module, T>, PropertyDelegateProvider<Module, ReadWriteProperty<Module, T>> {

    abstract val default: T

    abstract var value: T

    protected var hidden = false

    fun hide(): Setting<T> {
        hidden = true
        return this
    }

    protected var visibilityDependency: (() -> Boolean)? = null

    var parent: DropdownSetting? = null
        private set

    open fun reset() {
        value = default
    }

    val isVisible: Boolean
        get() = (visibilityDependency?.invoke() ?: true) && !hidden

    override operator fun provideDelegate(thisRef: Module, property: KProperty<*>): ReadWriteProperty<Module, T> =
        thisRef.registerSetting(this)

    override operator fun getValue(thisRef: Module, property: KProperty<*>): T =
        value

    override operator fun setValue(thisRef: Module, property: KProperty<*>, value: T) {
        this.value = value
    }

    companion object {

        fun <K : Setting<T>, T> K.withDependency(dependency: () -> Boolean): K {
            visibilityDependency = dependency
            return this
        }

        fun <K : Setting<T>, T> K.withDependency(dropdown: DropdownSetting): K {
            visibilityDependency = { dropdown.enabled }
            parent = dropdown
            return this
        }
    }
}
