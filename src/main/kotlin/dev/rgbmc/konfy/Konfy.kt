package dev.rgbmc.konfy

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import dev.rgbmc.konfy.codec.ConfigTransformer
import dev.rgbmc.konfy.codec.YamlProcessor
import kotlinx.serialization.serializer
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

@Suppress("unused")
object Konfy {
    @PublishedApi
    internal val yaml = Yaml(
        configuration = YamlConfiguration(
            strictMode = false
        )
    )

    /**
     * Serialize an object to YAML string.
     */
    inline fun <reified T> toYaml(instance: T): String {
        return yaml.encodeToString(serializer<T>(), instance)
    }

    /**
     * Deserialize YAML string to an object.
     * @param transformer Optional transformer to migrate old config format
     */
    inline fun <reified T> fromYaml(yamlString: String, transformer: ConfigTransformer? = null): T {
        val processedYaml = if (transformer != null) {
            YamlProcessor.processYaml(yamlString, transformer)
        } else {
            yamlString
        }
        return yaml.decodeFromString(serializer<T>(), processedYaml)
    }

    /**
     * Update YAML string: deserialize, apply defaults from code, and re-serialize.
     * This is useful to sync YAML files with code changes (new fields, updated defaults).
     */
    inline fun <reified T> update(yamlString: String, transformer: ConfigTransformer? = null): String {
        val instance = fromYaml<T>(yamlString, transformer)
        return toYaml(instance)
    }

    /**
     * Load configuration from a YAML file.
     * Automatically updates the file if it's missing fields or comments from the code.
     *
     * @param path Path to the YAML file
     * @param transformer Optional transformer to migrate old config format
     * @param autoUpdate If true (default), automatically updates and saves the file after loading
     * @return Deserialized configuration object
     */
    inline fun <reified T> loadFromFile(
        path: Path,
        transformer: ConfigTransformer? = null,
        autoUpdate: Boolean = true
    ): T {
        // If file doesn't exist, create with default values
        if (!path.exists()) {
            val defaultInstance = createDefault<T>()
            saveToFile(defaultInstance, path)
            return defaultInstance
        }

        val yamlContent = path.readText()
        val instance = fromYaml<T>(yamlContent, transformer)

        // Auto-update: re-serialize to add missing fields
        if (autoUpdate) {
            val updatedYaml = toYaml(instance)
            if (updatedYaml != yamlContent) {
                path.writeText(updatedYaml)
            }
        }

        return instance
    }

    /**
     * Load configuration from a YAML file (File overload).
     */
    inline fun <reified T> loadFromFile(
        file: File,
        transformer: ConfigTransformer? = null,
        autoUpdate: Boolean = true
    ): T = loadFromFile(file.toPath(), transformer, autoUpdate)

    /**
     * Save configuration object to a YAML file.
     *
     * @param instance Configuration object to save
     * @param path Path to the YAML file
     */
    inline fun <reified T> saveToFile(instance: T, path: Path) {
        // Create parent directories if they don't exist
        path.parent?.let { Files.createDirectories(it) }

        val yamlString = toYaml(instance)
        path.writeText(yamlString)
    }

    /**
     * Save configuration object to a YAML file (File overload).
     */
    inline fun <reified T> saveToFile(instance: T, file: File) {
        saveToFile(instance, file.toPath())
    }

    /**
     * Create a default instance of T using kotlinx.serialization.
     * This requires T to have a no-arg constructor or all parameters with defaults.
     */
    @PublishedApi
    internal inline fun <reified T> createDefault(): T {
        return yaml.decodeFromString(serializer<T>(), "{}")
    }
}
