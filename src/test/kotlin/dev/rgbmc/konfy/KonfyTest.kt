package dev.rgbmc.konfy

import com.charleskorn.kaml.YamlComment
import dev.rgbmc.konfy.codec.ConfigTransformer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

enum class TestEnum {
    A, B, C
}

@Serializable
data class TestConfig(
    @SerialName("type")
    @YamlComment("测试类型", "这个是测试类型的注释")
    val type: String = "default",

    @SerialName("enum_value")
    @YamlComment("枚举类型测试")
    val enumValue: TestEnum = TestEnum.A,

    @SerialName("list_value")
    val listValue: List<TestEnum> = listOf(TestEnum.A, TestEnum.B),

    @SerialName("nested")
    val nested: NestedConfig = NestedConfig()
)

@Serializable
data class NestedConfig(
    @SerialName("value")
    val value: Int = 10
)

class KonfyTest {
    @Test
    fun testSerialization() {
        val config = TestConfig(
            type = "custom",
            enumValue = TestEnum.B,
            listValue = listOf(TestEnum.A, TestEnum.C),
            nested = NestedConfig(value = 20)
        )

        val yaml = Konfy.toYaml(config)
        println("=== YAML Output ===")
        println(yaml)
        println("===================")

        // Comments are no longer automatically added
        assertTrue(yaml.contains("type:") || yaml.contains("type: custom"))
        assertTrue(yaml.contains("enum_value: \"B\""))
        assertTrue(yaml.contains("- \"A\""))
        assertTrue(yaml.contains("- \"C\""))
        assertTrue(yaml.contains("nested:"))
        assertTrue(yaml.contains("  value: 20"))
    }

    @Test
    fun testDeserialization() {
        val yaml = """
            type: "custom"
            enum_value: "B"
            list_value:
              - "A"
              - "C"
            nested:
              value: 20
        """.trimIndent()

        val config = Konfy.fromYaml<TestConfig>(yaml)

        assertEquals("custom", config.type)
        assertEquals(TestEnum.B, config.enumValue)
        assertEquals(listOf(TestEnum.A, TestEnum.C), config.listValue)
        assertEquals(20, config.nested.value)
    }

    @Test
    fun testDefaultValues() {
        val yaml = """
            type: "custom"
        """.trimIndent()

        val config = Konfy.fromYaml<TestConfig>(yaml)

        assertEquals("custom", config.type)
        assertEquals(TestEnum.A, config.enumValue) // Default
        assertEquals(listOf(TestEnum.A, TestEnum.B), config.listValue) // Default
        assertEquals(10, config.nested.value) // Default nested
    }

    @Test
    fun testUpdate() {
        val yaml = """
            type: "old"
            nested:
              value: 5
        """.trimIndent()

        val newYaml = Konfy.update<TestConfig>(yaml)
        println("=== Updated YAML ===")
        println(newYaml)
        println("====================")

        assertTrue(newYaml.contains("type: \"old\""))
        assertTrue(newYaml.contains("enum_value: \"A\"")) // Added default
        assertTrue(newYaml.contains("# 这个是测试类型的注释")) // Added comment
    }

    @Test
    fun testTransformer() {
        val yaml = """
            type: "old"
        """.trimIndent()

        val transformer = object : ConfigTransformer {
            override fun transform(wrapper: dev.rgbmc.konfy.codec.YamlWrapper) {
                if (wrapper.getString("type") == "old") {
                    wrapper.set("type", "new")
                }
            }
        }

        val config = Konfy.fromYaml<TestConfig>(yaml, transformer)
        println(config)
        assertEquals("new", config.type)
    }

    @Test
    fun testYamlWrapperBasicOps() {
        val yaml = """
            name: "Test"
            database:
              host: "localhost"
              port: 5432
        """.trimIndent()

        val transformer = object : ConfigTransformer {
            override fun transform(wrapper: dev.rgbmc.konfy.codec.YamlWrapper) {
                // Test get
                assertEquals("Test", wrapper.getString("name"))
                assertEquals("localhost", wrapper.getString("database.host"))
                assertEquals(5432, wrapper.getInt("database.port"))

                // Test set
                wrapper.set("database.timeout", 30)

                // Test contains
                assertTrue(wrapper.contains("database.host"))
                assertFalse(wrapper.contains("nonexistent"))
            }
        }

        Konfy.fromYaml<TestConfig>(yaml, transformer)
    }

    @Test
    fun testNodeMigration() {
        val yaml = """
            old_type: "migrated"
            nested:
              old_value: 99
        """.trimIndent()

        val transformer = object : ConfigTransformer {
            override fun transform(wrapper: dev.rgbmc.konfy.codec.YamlWrapper) {
                // Migrate old paths to new paths
                wrapper.migrate("old_type", "type")
                wrapper.migrate("nested.old_value", "nested.value")
            }

            override fun getObsoleteKeys(): Set<String> {
                return setOf("old_type")
            }
        }

        val config = Konfy.fromYaml<TestConfig>(yaml, transformer)
        println(config)
        assertEquals("migrated", config.type)
        assertEquals(99, config.nested.value)

        // Verify old keys are removed in updated YAML
        val updatedYaml = Konfy.update<TestConfig>(yaml, transformer)
        assertFalse(updatedYaml.contains("old_type"))
        assertTrue(updatedYaml.contains("type:"))
    }

    @Test
    fun testGenericTypeConversion() {
        val yaml = """
            type: "test"
            nested:
              value: 42
        """.trimIndent()

        val transformer = object : ConfigTransformer {
            override fun transform(wrapper: dev.rgbmc.konfy.codec.YamlWrapper) {
                // Test generic get with type conversion
                val typeValue: String = wrapper.get("type", "default")
                assertEquals("test", typeValue)

                val intValue: Int = wrapper.get("nested.value", 0)
                assertEquals(42, intValue)

                // Test getOrNull
                val existingValue: String? = wrapper.getOrNull("type")
                assertEquals("test", existingValue)

                val nullValue: String? = wrapper.getOrNull("nonexistent")
                assertEquals(null, nullValue)
            }
        }

        Konfy.fromYaml<TestConfig>(yaml, transformer)
    }
}

// Test data classes for complex structures
@Serializable
data class ComplexConfig(
    @SerialName("items")
    val items: List<Item> = emptyList()
)

@Serializable
data class Item(
    @SerialName("name")
    val name: String = "",

    @SerialName("properties")
    val properties: ItemProperties = ItemProperties()
)

@Serializable
data class ItemProperties(
    @SerialName("enabled")
    val enabled: Boolean = true,

    @SerialName("count")
    val count: Int = 0
)

class ComplexStructureTest {
    @Test
    fun testComplexStructureSerialization() {
        val config = ComplexConfig(
            items = listOf(
                Item("Item1", ItemProperties(true, 10)),
                Item("Item2", ItemProperties(false, 20))
            )
        )

        val yaml = Konfy.toYaml(config)
        println("=== Complex YAML Output ===")
        println(yaml)
        println("===========================")

        assertTrue(yaml.contains("items:"))
        assertTrue(yaml.contains("- "))
        assertTrue(yaml.contains("name: \"Item1\""))
        assertTrue(yaml.contains("name: \"Item2\""))
        assertTrue(yaml.contains("properties:"))
        assertTrue(yaml.contains("enabled: true"))
        assertTrue(yaml.contains("count: 10"))
    }

    @Test
    fun testComplexStructureDeserialization() {
        val yaml = """
            items:
              - name: "Item1"
                properties:
                  enabled: true
                  count: 10
              - name: "Item2"
                properties:
                  enabled: false
                  count: 20
        """.trimIndent()

        val config = Konfy.fromYaml<ComplexConfig>(yaml)

        println(config)

        assertEquals(2, config.items.size)
        assertEquals("Item1", config.items[0].name)
        assertEquals(true, config.items[0].properties.enabled)
        assertEquals(10, config.items[0].properties.count)
        assertEquals("Item2", config.items[1].name)
        assertEquals(false, config.items[1].properties.enabled)
        assertEquals(20, config.items[1].properties.count)
    }
}
