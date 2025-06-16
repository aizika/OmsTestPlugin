package com.workday.plugin.omstest

import com.intellij.openapi.actionSystem.DefaultActionGroup

/**
 * Action group for running OMS Gradle test actions in IntelliJ IDEA.
 * Provides options to run tests by method, class, package, or rerun the last test.
 *
 * @author alexander.aizikivsky
 * @since Jun-2025
 */
class OmsTestActionsGroup : DefaultActionGroup() {
    init {
        isPopup = true  // makes the group show as a popup menu
    }
}