package cs.ut.ui.adapters

import cs.ut.configuration.Value
import cs.ut.ui.FieldComponent
import cs.ut.ui.GridValueProvider
import cs.ut.util.COMP_ID
import cs.ut.util.NirdizatiUtil
import org.zkoss.zul.Combobox
import org.zkoss.zul.Label
import org.zkoss.zul.Row

/**
 * Wrapper to hold the data
 * @param caption to use for the combo
 * @param values put inside the combo box
 * @param selected which option is selected
 */
data class ComboArgument(val caption: String, val values: List<Value>, val selected: String)

/**
 * Adapter used when generating data set parameters stage 2
 */
class ComboProvider : GridValueProvider<ComboArgument, Row> {
    override lateinit var fields: MutableList<FieldComponent>

    override fun provide(data: ComboArgument): Row {
        val label = Label(data.caption)
        label.setAttribute(COMP_ID, data.caption)
        label.sclass = "display-block"

        val comboBox = Combobox()
        comboBox.sclass = "max-width max-height"
        data.values.forEach {
            val item = comboBox.appendItem(NirdizatiUtil.localizeText(it.value))
            item.setValue(it.identifier)

            if (it.identifier == data.selected) {
                comboBox.selectedItem = item
            }
        }

        comboBox.isReadonly = true

        val row = Row()
        row.appendChild(label)
        row.appendChild(comboBox)
        fields.add(FieldComponent(label, comboBox))

        return row
    }
}