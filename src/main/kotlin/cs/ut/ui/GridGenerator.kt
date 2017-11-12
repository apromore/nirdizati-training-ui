package cs.ut.ui

import cs.ut.config.items.Property
import cs.ut.exceptions.NirdizatiRuntimeException
import org.apache.log4j.Logger
import org.zkoss.util.resource.Labels
import org.zkoss.zk.ui.Component
import org.zkoss.zul.*
import org.zkoss.zul.impl.InputElement

class NirdizatiGrid : Grid() {
    private val log = Logger.getLogger(NirdizatiGrid::class.java)
    private val fields = mutableListOf<Component>()

    fun generate(data: List<Property>) {
        log.debug("Row generation start with ${data.size} properties")
        val start = System.currentTimeMillis()
        rows ?: appendChild(Rows())
        rows.getChildren<Component>().clear()

        generateRows(data.toMutableList(), rows)

        val end = System.currentTimeMillis()
        log.debug("Row generation finished in ${end - start} ms")
    }

    tailrec private fun generateRows(data: MutableList<Property>, rows: Rows) {
        if (data.isNotEmpty()) {
            val prop = data.first()
            val row = Row()

            row.appendChild(Label(Labels.getLabel("property." + prop.id)))
            row.appendChild(generateControl(prop))

            rows.appendChild(row)
            generateRows(data.tail(), rows);
        }
    }

    fun validate(): Boolean {
        val invalid = mutableListOf<Component>()
        validateFields(fields, invalid)
        return invalid.isEmpty()
    }

    tailrec private fun validateFields(fields: MutableList<Component>, invalid: MutableList<Component>) {
        if (fields.isNotEmpty()) {
            val comp = fields.first()

            when (comp) {
                is Intbox -> if (comp.value == null || comp.value <= 0) {
                    comp.errorMessage = Labels.getLabel("training.validation.greater_than_zero")
                    invalid.add(comp)
                }
                is Doublebox -> if (comp.value == null || comp.value <= 0.0) {
                    comp.errorMessage = Labels.getLabel("training.validation.greater_than_zero")
                    invalid.add(comp)
                }
            }

            validateFields(fields.tail(), invalid)
        }
    }


    private fun <T> MutableList<T>.tail(): MutableList<T> = drop(1).toMutableList()

    private fun generateControl(prop: Property): Component {
        val obj = Class.forName(prop.type).getConstructor().newInstance()

        when (obj) {
            is Intbox -> obj.value = prop.property.toInt()
            is Doublebox -> obj.setValue(prop.property.toDouble())
            else -> NirdizatiRuntimeException("Uknown element $prop")
        }

        fields.add(obj as InputElement)
        obj.setConstraint("no empty")
        obj.id = prop.id

        return obj
    }

    fun gatherValues(): Map<String, Number> {
        val valueMap = mutableMapOf<String, Number>()
        gatherValueFromFields(valueMap, fields)
        return valueMap
    }

    tailrec private fun gatherValueFromFields(valueMap: MutableMap<String, Number>, fields: MutableList<Component>) {
        if (fields.isNotEmpty()) {
            val field = fields.first()

            when (field) {
                is Intbox -> valueMap[field.id] = field.value
                is Doublebox -> valueMap[field.id] = field.value
            }

            gatherValueFromFields(valueMap, fields.tail())
        }
    }
}