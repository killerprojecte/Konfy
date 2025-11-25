package dev.rgbmc.konfy.codec

import it.krzeminski.snakeyaml.engine.kmp.api.Dump
import it.krzeminski.snakeyaml.engine.kmp.api.DumpSettings
import it.krzeminski.snakeyaml.engine.kmp.api.Load
import it.krzeminski.snakeyaml.engine.kmp.api.LoadSettings
import it.krzeminski.snakeyaml.engine.kmp.common.FlowStyle
import it.krzeminski.snakeyaml.engine.kmp.common.ScalarStyle


/**
 * Low-level YAML processor using snakeyaml-engine for transformation.
 * Used to process YAML before/after kaml serialization for config migration and auto-completion.
 */
object YamlProcessor {
    private val loadSettings = LoadSettings()
    private val dumpSettings = DumpSettings(
        defaultFlowStyle = FlowStyle.BLOCK,
        defaultScalarStyle = ScalarStyle.PLAIN
    )
    
    private val loader = Load(loadSettings)
    private val dumper = Dump(dumpSettings)
    
    /**
     * Process YAML with transformer before deserialization.
     * This allows for config migration and field transformations.
     */
    fun processYaml(yamlString: String, transformer: ConfigTransformer): String {
        // Load as generic structure
        // Convert to MutableMap
        @Suppress("UNCHECKED_CAST")
        val mutableMap = when (val data = loader.loadOne(yamlString)) {
            is Map<*, *> -> data.toMutableMap() as MutableMap<String, Any>
            else -> mutableMapOf()
        }
        
        // Apply transformer
        val wrapper = YamlWrapper(mutableMap)
        transformer.transform(wrapper)
        
        // Remove obsolete keys
        for (key in transformer.getObsoleteKeys()) {
            wrapper.remove(key)
        }
        
        // Dump back to YAML
        return dumper.dumpToString(wrapper.getRawMap())
    }
}
