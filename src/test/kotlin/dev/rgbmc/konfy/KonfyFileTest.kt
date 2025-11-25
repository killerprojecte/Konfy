package dev.rgbmc.konfy

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
// Comment annotations removed
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readText

@Serializable
data class FileTestConfig(
    @SerialName("name")
    val name: String = "MyApp",
    
    @SerialName("version")
    val version: String = "1.0.0",
    
    @SerialName("port")
    val port: Int = 8080
)

class KonfyFileTest {
    @Test
    fun testSaveToFile(@TempDir tempDir: Path) {
        val config = FileTestConfig(name = "TestApp", version = "2.0.0", port = 9090)
        val file = tempDir.resolve("config.yml")
        
        Konfy.saveToFile(config, file)
        
        assertTrue(file.exists())
        val content = file.readText()
        // Comments are no longer automatically added
        assertTrue(content.contains("name: \"TestApp\""))
        assertTrue(content.contains("version: \"2.0.0\""))
        assertTrue(content.contains("port: 9090"))
    }
    
    @Test
    fun testLoadFromFile(@TempDir tempDir: Path) {
        val file = tempDir.resolve("config.yml")
        file.toFile().writeText("""
            name: "LoadedApp"
            port: 3000
        """.trimIndent())
        
        val config = Konfy.loadFromFile<FileTestConfig>(file, autoUpdate = false)

        println(config)

        assertEquals("LoadedApp", config.name)
        assertEquals("1.0.0", config.version) // Default value
        assertEquals(3000, config.port)
    }
    
    @Test
    fun testLoadFromFileAutoUpdate(@TempDir tempDir: Path) {
        val file = tempDir.resolve("config.yml")
        // Write minimal config
        file.toFile().writeText("""
            name: "MinimalApp"
        """.trimIndent())
        
        val config = Konfy.loadFromFile<FileTestConfig>(file, autoUpdate = true)

        println(config)
        
        // Check loaded values
        assertEquals("MinimalApp", config.name)
        assertEquals("1.0.0", config.version)
        assertEquals(8080, config.port)
        
        // Check file was updated with missing fields and comments
        val updatedContent = file.readText()
        println(updatedContent)
        // Comments are no longer automatically added
        assertTrue(updatedContent.contains("version: \"1.0.0\""))
        assertTrue(updatedContent.contains("port: 8080"))
    }
    
    @Test
    fun testLoadFromNonExistentFile(@TempDir tempDir: Path) {
        val file = tempDir.resolve("new-config.yml")
        
        val config = Konfy.loadFromFile<FileTestConfig>(file)

        println(config)
        
        // Should create file with default values
        assertTrue(file.exists())
        assertEquals("MyApp", config.name)
        assertEquals("1.0.0", config.version)
        assertEquals(8080, config.port)
        
        val content = file.readText()
        // Comments are no longer automatically added
    }
}
