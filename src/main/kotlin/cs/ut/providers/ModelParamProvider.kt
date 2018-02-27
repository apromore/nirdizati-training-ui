package cs.ut.providers

import cs.ut.engine.item.ModelParameter
import cs.ut.engine.item.Property
import cs.ut.configuration.ConfigNode
import cs.ut.configuration.ConfigurationReader
import cs.ut.logging.NirdizatiLogger
import cs.ut.util.PREDICTIONTYPE
import cs.ut.util.readHyperParameterJson

/**
 * Parses model parameters from configuration
 *
 * @see ModelParameter
 * @see ConfigurationReader
 */
class ModelParamProvider {
    private val config = ConfigurationReader.findNode("models")

    var properties: Map<String, List<ModelParameter>> = mutableMapOf()

    init {
        log.debug("Started parsing configuration with node $config")
        parseParameters()
        log.debug("Successfully parsed model parameters from configuration")
    }

    /**
     * Get basic parameters based on configuration definition
     */
    fun getBasicParameters(): List<ModelParameter> {
        val basicParameters = ConfigurationReader.findNode("models/basic").itemListValues()
        return mutableListOf<ModelParameter>().apply {
            basicParameters.forEach { p ->
                this.add(properties.flatMap { it.value }.first { it.id == p })
            }
        }
    }

    /**
     * Get all prediction types
     *
     * @return list of model parameters marked as 'predictiontype'
     */
    fun getPredictionTypes() = properties[PREDICTIONTYPE]!!

    /**
     * Get all properties from all model parameters
     *
     * @return list of properties
     * @see Property
     */
    fun getAllProperties(): List<Property> = properties.flatMap { it.value.flatMap { it.properties } }

    /**
     * Parses config nodes into model parameters
     *
     * @see ConfigNode
     * @see ModelParameter
     */
    private fun parseParameters() {
        val params = mutableListOf<ModelParameter>()
        config.childNodes.first { it.identifier == PARAM_NODE }.childNodes.forEach {
            log.debug("Parsing parameter node: $it")
            params.add(ModelParameter().apply {
                this.enabled = it.values.first { it.identifier == ENABLED }.booleanValue()
                this.id = it.identifier
                this.type = it.values.first { it.identifier == TYPE }.value
                this.parameter = it.values.first { it.identifier == PARAMETER }.value

                val propNode = it.childNodes.firstOrNull { it.identifier == PROPERTIES }
                this.properties = parseProperties(this, propNode)
            })
        }

        properties = params.groupBy { it.type }
    }

    /**
     * Parses config nodes into properties
     *
     * @param modelParameter model parameter that is owner of the properties
     * @param propNode node where to take properties from
     *
     * @return list of properties defined by the configuration
     *
     * @see Property
     * @see ConfigNode
     */
    private fun parseProperties(modelParameter: ModelParameter, propNode: ConfigNode?): MutableList<Property> {
        val properties = mutableListOf<Property>()
        log.debug("Parsing properties for $modelParameter")

        propNode?.childNodes?.forEach {
            properties.add(Property().apply {
                this.id = it.identifier
                this.type = it.values.firstOrNull { it.identifier == CONTROL }?.value ?: ""
                this.maxValue = it.values.firstOrNull { it.identifier == MAX }?.doubleValue() ?: -1.0
                this.minValue = it.values.firstOrNull { it.identifier == MIN }?.doubleValue() ?: -1.0
                this.property = it.values.firstOrNull { it.identifier == DEFAULT }?.value ?: ""
            })
        }

        return properties
    }

    companion object {
        private val log = NirdizatiLogger.getLogger(ModelParamProvider::class.java)

        const val PARAM_NODE = "parameters"
        const val ENABLED = "isEnabled"
        const val TYPE = "type"
        const val PROPERTIES = "properties"
        const val CONTROL = "control"
        const val MAX = "max"
        const val MIN = "min"
        const val DEFAULT = "default"
        const val PARAMETER = "parameter"

        fun getOptimizedParameters() = readHyperParameterJson()
    }
}