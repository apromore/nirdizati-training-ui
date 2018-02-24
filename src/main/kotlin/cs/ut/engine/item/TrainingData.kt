package cs.ut.engine.item

/**
 * Entity that is used to hold deserialized JSON
 */
class TrainingData {
    var static_cat_cols: Array<String> = arrayOf()
    var dynamic_cat_cols: Array<String> = arrayOf()

    var timestamp_col: String = ""
    var activity_col: String = ""

    var static_num_cols: Array<String> = arrayOf()
    var dynamic_num_cols: Array<String> = arrayOf()
    var future_values: Array<String> = arrayOf()

    var case_id_col: String = ""

    fun getAllColumns(): List<String> = ((static_cat_cols + static_num_cols + future_values).toList() - activity_col)
}