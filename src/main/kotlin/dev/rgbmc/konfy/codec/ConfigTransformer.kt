package dev.rgbmc.konfy.codec

/**
 * Interface for transforming YAML configuration data.
 * Implementations can migrate old configuration formats to new ones,
 * set default values, or perform other transformations.
 */
interface ConfigTransformer {
    /**
     * Transform the YAML data using the provided wrapper.
     *
     * @param wrapper YamlWrapper providing convenient access to YAML data
     */
    fun transform(wrapper: YamlWrapper)

    /**
     * Return a set of keys that should be removed after transformation.
     * This is useful for cleaning up obsolete configuration nodes.
     *
     * @return Set of dot-separated paths to remove (e.g., "old.config.path")
     */
    fun getObsoleteKeys(): Set<String> = emptySet()
}
