package cs.ut.util

import com.google.gson.Gson
import com.google.gson.stream.JsonReader
import cs.ut.config.MasterConfiguration
import cs.ut.config.TrainingData
import cs.ut.config.items.ModelParameter
import cs.ut.config.items.Property
import cs.ut.config.nodes.Dir
import cs.ut.config.nodes.DirectoryConfiguration
import cs.ut.exceptions.NirdizatiRuntimeException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.IOException

fun readHyperParameterJson(): Map<String, List<ModelParameter>> {
    val files = readFilesFromDir()
    val modelsParams = parseJsonFiles(files)
    mapTypes(modelsParams)
    return modelsParams
}

fun readLogColumns(logName: String): List<String> {
    val json: TrainingData = readTrainingData(logName)
    return json.getAllColumns()
}

fun readTrainingJson(key: String): Map<String, List<ModelParameter>> =
        parseJsonFiles(listOf(File(
                MasterConfiguration.dirConfig.dirPath(Dir.TRAIN_DIR) + "$key.json")))
                .apply { mapTypes(this) }

fun readLogFile(key: String): String {
    val json = JSONObject(readJsonFiles(listOf(
            File(MasterConfiguration.dirConfig.dirPath(Dir.TRAIN_DIR) + "$key.json"))).values.first())

    return json.getJSONObject(UI_DATA).let { it[LOG_FILE] as String }
}

private fun readTrainingData(logName: String): TrainingData {
    val config: DirectoryConfiguration = MasterConfiguration.dirConfig
    val path: String = config.dirPath(Dir.DATA_DIR)

    val file = File(path + logName + ".json")
    val jsonReader = JsonReader(FileReader(file))

    return Gson().fromJson(jsonReader, TrainingData::class.java)
}

private fun mapTypes(modelsParams: Map<String, List<ModelParameter>>) {
    val allProperties = MasterConfiguration.modelConfiguration.getAllProperties()

    modelsParams.values.flatMap { it }.flatMap { it.properties }.forEach { prop ->
        val withType = allProperties.first { it.id == prop.id }
        prop.type = withType.type
    }
}


private fun readFilesFromDir(): List<File> {
    val path = MasterConfiguration.dirConfig.dirPath(Dir.OHP_DIR)

    val dir = File(path)
    if (!dir.exists() && dir.isDirectory) throw NirdizatiRuntimeException("Optimized hyperparameter directory does not exist")

    return dir.listFiles()?.toList() ?: listOf()
}

private fun parseJsonFiles(files: List<File>): Map<String, List<ModelParameter>> {
    val map = mutableMapOf<String, List<ModelParameter>>()
    val jsons = readJsonFiles(files)
    parseJson(jsons.toMutableMap(), map)
    return map
}

private fun readJsonFiles(files: List<File>): Map<String, String> {
    val jsons = mutableMapOf<String, String>()
    readJson(files, jsons)
    return jsons
}

private tailrec fun readJson(files: List<File>, jsons: MutableMap<String, String>) {
    if (files.isNotEmpty()) {
        val file = files.first()
        try {
            val sb = StringBuilder()
            BufferedReader(FileReader(file)).use {
                it.lines().forEach { sb.append(it) }
            }
            jsons[file.nameWithoutExtension] = sb.toString()
        } catch (e: IOException) {
            throw NirdizatiRuntimeException("Could not read file $file")
        }
        readJson(files.drop(1), jsons)
    }
}


private tailrec fun parseJson(jsons: MutableMap<String, String>, map: MutableMap<String, List<ModelParameter>>) {
    if (jsons.isNotEmpty()) {
        val key = jsons.keys.first()
        val entry = jsons.remove(key)
        val json = JSONObject(entry)

        val params = mutableListOf<String>()

        val outcome = json.keySet().first()
        params.add(outcome)

        val secondLevel = json.getJSONObject(outcome)
        val encAndBucket = secondLevel.keySet().first().split("_")
        params.addAll(encAndBucket)

        val thirdLevel = secondLevel.getJSONObject(encAndBucket.joinToString(separator = "_"))
        val learner = thirdLevel.keySet().first()
        params.add(learner)

        val paramArray = thirdLevel.getJSONObject(learner).toMap()
        val properties = mutableListOf<Property>()
        paramArray.entries.forEach {
            properties.add(Property(it.key, "", it.value.toString(), -1.0, -1.0))
        }

        val modelProperties = getModelParams(params)
        modelProperties.first { it.type == "learner" }.properties = properties

        map[key] = modelProperties

        parseJson(jsons, map)
    }
}

private fun getModelParams(paramNames: List<String>): List<ModelParameter> {
    val alreadyDefined = MasterConfiguration.modelConfiguration.parameters

    val rightParameters = mutableListOf<ModelParameter>()
    paramNames.forEach { param -> rightParameters.add(ModelParameter(alreadyDefined.first { it.parameter == param })) }

    return rightParameters
}
