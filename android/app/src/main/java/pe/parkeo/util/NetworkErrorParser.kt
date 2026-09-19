package pe.parkeo.util

import okhttp3.ResponseBody
import org.json.JSONObject

object NetworkErrorParser {
    /**
     * Parses Spring Boot ApiResponse error JSON from Retrofit's errorBody.
     * Extracts either field validation messages in data or the top-level message.
     */
    fun parseErrorMessage(errorBody: ResponseBody?, defaultMessage: String): String {
        if (errorBody == null) return defaultMessage
        return try {
            val jsonString = errorBody.string()
            if (jsonString.isBlank()) return defaultMessage

            val json = JSONObject(jsonString)

            // 1. Check if 'data' contains field-level validation errors (Map<String, String>)
            if (json.has("data") && !json.isNull("data")) {
                val dataObj = json.optJSONObject("data")
                if (dataObj != null && dataObj.length() > 0) {
                    val errorList = mutableListOf<String>()
                    val keys = dataObj.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        val msg = dataObj.optString(key)
                        if (msg.isNotBlank()) {
                            errorList.add(msg)
                        }
                    }
                    if (errorList.isNotEmpty()) {
                        return errorList.joinToString("\n")
                    }
                }
            }

            // 2. Check top-level 'message'
            val message = json.optString("message")
            if (message.isNotBlank() && !message.equals("null", ignoreCase = true) && !message.equals("Errores de validación", ignoreCase = true)) {
                return message
            }

            defaultMessage
        } catch (_: Exception) {
            defaultMessage
        }
    }
}
