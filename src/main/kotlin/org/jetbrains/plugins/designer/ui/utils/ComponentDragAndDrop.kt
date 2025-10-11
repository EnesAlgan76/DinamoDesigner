package org.jetbrains.plugins.template.designer.ui

import org.jetbrains.plugins.template.designer.components.ComponentDefinition
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException

object ComponentDataFlavor : DataFlavor(ComponentDefinition::class.java, "ComponentDefinition")

class ComponentTransferable(private val definition: ComponentDefinition) : Transferable {
    override fun getTransferDataFlavors() = arrayOf(ComponentDataFlavor)
    override fun isDataFlavorSupported(flavor: DataFlavor) = flavor == ComponentDataFlavor
    override fun getTransferData(flavor: DataFlavor): Any {
        if (flavor == ComponentDataFlavor) return definition
        throw UnsupportedFlavorException(flavor)
    }
}


