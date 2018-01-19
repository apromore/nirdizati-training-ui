package cs.ut.ui.controllers.training

import cs.ut.config.items.ModelParameter
import cs.ut.config.items.Property
import cs.ut.ui.FieldComponent
import cs.ut.ui.NirdizatiGrid
import cs.ut.ui.adapters.AdvancedModeAdapter
import cs.ut.ui.adapters.GeneratorArgument
import cs.ut.ui.adapters.PropertyValueAdapter
import cs.ut.ui.controllers.TrainingController
import org.apache.log4j.Logger
import org.zkoss.zk.ui.Component
import org.zkoss.zk.ui.event.CheckEvent
import org.zkoss.zk.ui.event.Events
import org.zkoss.zul.Checkbox
import org.zkoss.zul.Hlayout
import org.zkoss.zul.Vlayout

class AdvancedModeController(gridContainer: Vlayout) : AbstractModeController(gridContainer), ModeController {
    private val log: Logger = Logger.getLogger(AdvancedModeController::class.java)!!

    private val grid: NirdizatiGrid<GeneratorArgument> = NirdizatiGrid(AdvancedModeAdapter())
    private val hyperParamsContainer = Hlayout()
    private var hyperParameters: MutableMap<ModelParameter, MutableList<Property>> = mutableMapOf()

    init {
        log.debug("Initializing advanced mode controller")
        gridContainer.getChildren<Component>().clear()

        grid.generate(parameters
                .entries
                .map { GeneratorArgument(it.key, it.value) })

        gridContainer.appendChild(grid)
        gridContainer.appendChild(hyperParamsContainer)
        grid.fields.forEach { it.generateListener() }
        grid.sclass = "max-height max-width"
        grid.hflex = "min"
        log.debug("Finished grid initialization")
    }

    private fun FieldComponent.generateListener() {
        control as Checkbox
        val parameter = control.getValue<ModelParameter>()
        if (TrainingController.LEARNER == parameter.type) {
            hyperParameters[parameter] = mutableListOf()
        }

        control.addEventListener(Events.ON_CHECK, { e ->
            e as CheckEvent
            log.debug("$this value changed, regenerating grid")
            when (parameter.type) {
                TrainingController.LEARNER -> parameter.handleLearner(e)
                else -> parameter.handleOther(e)
            }
            generateGrids()
        })
    }

    private fun generateGrids() {
        hyperParamsContainer.getChildren<Component>().clear()
        hyperParamsContainer.vflex = "1"
        hyperParameters.entries.forEach { it.generateGrid() }
    }

    private fun Map.Entry<ModelParameter, List<Property>>.generateGrid() {
        if (value.size < 2) return

        log.debug("Key: $key -> value: $value")

        val propGrid = NirdizatiGrid(PropertyValueAdapter())
        propGrid.setColumns(mapOf(
                key.type + "." + key.id to "min",
                "" to "min"
        ))

        propGrid.generate(value)
        propGrid.vflex = "1"

        hyperParamsContainer.appendChild(propGrid)
    }

    private fun ModelParameter.handleOther(e: CheckEvent) {
        hyperParameters.values.forEach {
            if (e.isChecked) {
                it.addAll(this.properties)
            } else {
                it.removeAll(this.properties)
            }
        }
    }

    private fun ModelParameter.handleLearner(e: CheckEvent) {
        if (e.isChecked) {
            hyperParameters[this]?.addAll(this.properties)
        } else {
            hyperParameters[this]?.removeAll(this.properties)
        }
    }

    override fun isValid(): Boolean {
        var isValid = grid.validate()

        hyperParamsContainer.getChildren<Component>().forEach {
            isValid = (it as NirdizatiGrid<*>).validate()
        }

        return isValid
    }

    override fun gatherValues(): Map<String, List<ModelParameter>> {
        val gathered = grid.gatherValues()

        val hyperParams = mutableMapOf<String, Map<String, Any>>()
        hyperParamsContainer.getChildren<Component>().forEach {
            it as NirdizatiGrid<*>
            hyperParams[it.columns.firstChild.id] = it.gatherValues()
        }

        hyperParams.forEach { k, v ->
            val keys = k.split(".")
            if (keys.size > 1) {
                val params = gathered[keys[0]] as List<*>
                val copy = mutableListOf<ModelParameter>()
                params.forEach { param ->
                    param as ModelParameter
                    val parameter = ModelParameter(param)
                    if (parameter.id == keys[1]) {
                        parameter.properties.clear()
                        v.forEach {
                            parameter.properties.add(Property(it.key, "", it.value.toString(), -1.0, -1.0))
                        }
                    }
                    copy.add(parameter)
                }
                gathered[keys[0]] = copy
            }
        }

        return gathered as Map<String, List<ModelParameter>>
    }

}