package com.penpal.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.penpal.domain.model.Character
import com.penpal.domain.model.Scene
import com.penpal.domain.model.Story
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExportService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun exportToText(
        story: Story,
        scenes: List<Scene>,
        characters: List<Character>
    ): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            val content = buildString {
                appendLine("=" .repeat(40))
                appendLine(story.title)
                appendLine("=".repeat(40))
                appendLine()

                story.description?.let {
                    appendLine(it)
                    appendLine()
                }

                appendLine("Characters")
                appendLine("-".repeat(20))
                characters.forEach { char ->
                    appendLine("- ${char.name}")
                    char.description?.let { appendLine("  $it") }
                }
                appendLine()

                appendLine("Scenes")
                appendLine("-".repeat(20))
                scenes.sortedBy { it.orderIndex }.forEach { scene ->
                    appendLine()
                    appendLine("Scene ${scene.orderIndex + 1}: ${scene.title}")
                    appendLine("-".repeat(scene.title.length + 8))

                    scene.summary?.let { appendLine("Summary: $it") }
                    scene.location?.let { appendLine("Location: $it") }
                    scene.mood?.let { appendLine("Mood: $it") }
                    appendLine()
                    appendLine(scene.content)
                }

                appendLine()
                appendLine("=".repeat(40))
                appendLine("Exported from pen-pal")
                appendLine("=".repeat(40))
            }

            val file = File(context.cacheDir, "${story.title.replace(" ", "_")}_${System.currentTimeMillis()}.txt")
            file.writeText(content)

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            Result.success(uri)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun exportToJson(
        story: Story,
        scenes: List<Scene>,
        characters: List<Character>
    ): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            val jsonContent = buildString {
                appendLine("{")
                appendLine("  \"story\": {")
                appendLine("    \"id\": \"${story.id}\",")
                appendLine("    \"title\": \"${story.title}\",")
                appendLine("    \"description\": \"${story.description ?: ""}\"")
                appendLine("  },")
                
                appendLine("  \"characters\": [")
                characters.forEachIndexed { index, char ->
                    appendLine("    {")
                    appendLine("      \"id\": \"${char.id}\",")
                    appendLine("      \"name\": \"${char.name}\"")
                    appendLine("    }${if (index < characters.size - 1) "," else ""}")
                }
                appendLine("  ],")
                
                appendLine("  \"scenes\": [")
                scenes.sortedBy { it.orderIndex }.forEachIndexed { index, scene ->
                    appendLine("    {")
                    appendLine("      \"id\": \"${scene.id}\",")
                    appendLine("      \"title\": \"${scene.title}\",")
                    appendLine("      \"content\": \"${scene.content.take(100).replace("\"", "\\\"")}\"")
                    appendLine("    }${if (index < scenes.size - 1) "," else ""}")
                }
                appendLine("  ]")
                appendLine("}")
            }

            val file = File(context.cacheDir, "${story.title.replace(" ", "_")}_${System.currentTimeMillis()}.json")
            file.writeText(jsonContent)

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            Result.success(uri)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun createShareIntent(uri: Uri, isJson: Boolean): Intent {
        return Intent(Intent.ACTION_SEND).apply {
            this.data = uri
            type = if (isJson) "application/json" else "text/plain"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}