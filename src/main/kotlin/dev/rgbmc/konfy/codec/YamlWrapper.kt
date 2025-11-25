package dev.rgbmc.konfy.codec

/**
 * Wrapper class for YAML data that provides convenient methods for accessing and modifying
 * configuration values using path notation (e.g., "database.host").
 * 
 * Inspired by Bukkit's configuration API but with Kotlin-specific enhancements.
 */
@Suppress("unused")
class YamlWrapper(private val data: MutableMap<String, Any>) {
    
    /**
     * Get the raw underlying map for advanced operations.
     */
    fun getRawMap(): MutableMap<String, Any> = data
    
    /**
     * Get value at the specified path.
     * 
     * @param path Dot-separated path (e.g., "database.host")
     * @return The value at the path, or null if not found
     */
    fun get(path: String): Any? {
        val keys = path.split(".")
        var current: Any? = data
        for (key in keys) {
            current = (current as? Map<*, *>)?.get(key) ?: return null
        }
        return current
    }
    
    /**
     * Get value at the specified path with automatic type conversion.
     * 
     * Supports conversion for:
     * - String, Int, Long, Double, Float, Boolean (with smart parsing)
     * - List<*>, Map<*, *>
     * - Custom types (returns the value as-is if it matches the type)
     * 
     * @param path Dot-separated path
     * @param default Default value if path doesn't exist or conversion fails
     * @return The value converted to type T
     */
    inline fun <reified T> get(path: String, default: T): T {
        val value = get(path) ?: return default
        return convertToType(value, default)
    }
    
    /**
     * Get value at the specified path with automatic type conversion (nullable).
     * 
     * @param path Dot-separated path
     * @return The value converted to type T, or null if not found or conversion fails
     */
    inline fun <reified T> getOrNull(path: String): T? {
        val value = get(path) ?: return null
        return try {
            convertToTypeNullable<T>(value)
        } catch (_: Exception) {
            null
        }
    }
    
    @Suppress("UNCHECKED_CAST")
    inline fun <reified T> convertToType(value: Any, default: T): T {
        return when (T::class) {
            String::class -> (value.toString() as T)
            Int::class -> when (value) {
                is Int -> value as T
                is Number -> value.toInt() as T
                is String -> (value.toIntOrNull() ?: default) as T
                else -> default
            }
            Long::class -> when (value) {
                is Long -> value as T
                is Number -> value.toLong() as T
                is String -> (value.toLongOrNull() ?: default) as T
                else -> default
            }
            Double::class -> when (value) {
                is Double -> value as T
                is Number -> value.toDouble() as T
                is String -> (value.toDoubleOrNull() ?: default) as T
                else -> default
            }
            Float::class -> when (value) {
                is Float -> value as T
                is Number -> value.toFloat() as T
                is String -> (value.toFloatOrNull() ?: default) as T
                else -> default
            }
            Boolean::class -> when (value) {
                is Boolean -> value as T
                is String -> (value.toBooleanStrictOrNull() ?: default) as T
                else -> default
            }
            List::class -> (value as? List<*> ?: default) as T
            Map::class -> (value as? Map<*, *> ?: default) as T
            else -> (value as? T) ?: default
        }
    }
    
    @Suppress("UNCHECKED_CAST")
    inline fun <reified T> convertToTypeNullable(value: Any): T? {
        return when (T::class) {
            String::class -> value.toString() as T
            Int::class -> when (value) {
                is Int -> value as T
                is Number -> value.toInt() as T
                is String -> value.toIntOrNull() as? T
                else -> null
            }
            Long::class -> when (value) {
                is Long -> value as T
                is Number -> value.toLong() as T
                is String -> value.toLongOrNull() as? T
                else -> null
            }
            Double::class -> when (value) {
                is Double -> value as T
                is Number -> value.toDouble() as T
                is String -> value.toDoubleOrNull() as? T
                else -> null
            }
            Float::class -> when (value) {
                is Float -> value as T
                is Number -> value.toFloat() as T
                is String -> value.toFloatOrNull() as? T
                else -> null
            }
            Boolean::class -> when (value) {
                is Boolean -> value as T
                is String -> value.toBooleanStrictOrNull() as? T
                else -> null
            }
            List::class -> value as? T
            Map::class -> value as? T
            else -> value as? T
        }
    }
    
    // Convenience typed getters
    fun getString(path: String, default: String = ""): String = 
        get(path, default)
    
    fun getInt(path: String, default: Int = 0): Int = 
        get(path, default)
    
    fun getLong(path: String, default: Long = 0L): Long = 
        get(path, default)
    
    fun getDouble(path: String, default: Double = 0.0): Double = 
        get(path, default)
    
    fun getFloat(path: String, default: Float = 0f): Float = 
        get(path, default)
    
    fun getBoolean(path: String, default: Boolean = false): Boolean = 
        get(path, default)
    
    fun getList(path: String): List<*>? = get(path) as? List<*>
    
    fun getMap(path: String): Map<*, *>? = get(path) as? Map<*, *>
    
    /**
     * Set value at the specified path, creating intermediate maps as needed.
     * 
     * @param path Dot-separated path
     * @param value Value to set (null to remove)
     */
    fun set(path: String, value: Any?) {
        val keys = path.split(".")
        if (keys.isEmpty()) return
        
        var current = data
        for (i in 0 until keys.size - 1) {
            val key = keys[i]
            val next = current[key]
            if (next !is MutableMap<*, *>) {
                val newMap = mutableMapOf<String, Any>()
                current[key] = newMap
                @Suppress("UNCHECKED_CAST")
                current = newMap
            } else {
                @Suppress("UNCHECKED_CAST")
                current = next as MutableMap<String, Any>
            }
        }
        
        val lastKey = keys.last()
        if (value == null) {
            current.remove(lastKey)
        } else {
            current[lastKey] = value
        }
    }
    
    /**
     * Remove the value at the specified path.
     */
    fun remove(path: String) = set(path, null)
    
    /**
     * Check if a value exists at the specified path.
     */
    fun contains(path: String): Boolean = get(path) != null
    
    /**
     * Migrate data from old path to new path.
     * The value is copied to the new path and removed from the old path.
     * 
     * @param oldPath Source path
     * @param newPath Destination path
     */
    fun migrate(oldPath: String, newPath: String) {
        val value = get(oldPath)
        if (value != null) {
            set(newPath, value)
            remove(oldPath)
        }
    }
    
    /**
     * Get a nested section as a new YamlWrapper.
     * 
     * @param path Path to the section
     * @return YamlWrapper for the nested section, or null if not found or not a map
     */
    fun getSection(path: String): YamlWrapper? {
        return when (val value = get(path)) {
            is Map<*, *> -> {
                @Suppress("UNCHECKED_CAST")
                val mutableMap = when (value) {
                    is MutableMap<*, *> -> value as MutableMap<String, Any>
                    else -> value.toMutableMap() as MutableMap<String, Any>
                }
                YamlWrapper(mutableMap)
            }
            else -> null
        }
    }
    
    companion object {
        /**
         * Create a YamlWrapper from any data object.
         * 
         * @param data The data to wrap (should be a Map)
         * @return YamlWrapper instance
         */
        fun from(data: Any): YamlWrapper {
            @Suppress("UNCHECKED_CAST")
            val map = when (data) {
                is MutableMap<*, *> -> data as MutableMap<String, Any>
                is Map<*, *> -> data.toMutableMap() as MutableMap<String, Any>
                else -> mutableMapOf()
            }
            return YamlWrapper(map)
        }
    }
}
