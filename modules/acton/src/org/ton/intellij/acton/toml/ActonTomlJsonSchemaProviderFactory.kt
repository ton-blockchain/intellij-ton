package org.ton.intellij.acton.toml

import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.jsonSchema.extension.JsonSchemaFileProvider
import com.jetbrains.jsonSchema.extension.JsonSchemaProviderFactory
import com.jetbrains.jsonSchema.extension.SchemaType

class ActonTomlJsonSchemaProviderFactory : JsonSchemaProviderFactory, DumbAware {
    override fun getProviders(project: Project): List<JsonSchemaFileProvider> {
        return listOf(ActonTomlJsonSchemaFileProvider())
    }
}

class ActonTomlJsonSchemaFileProvider : JsonSchemaFileProvider {
    override fun isAvailable(file: VirtualFile): Boolean = file.name == "Acton.toml"
    override fun getName(): String = "Acton.toml"
    override fun getSchemaType(): SchemaType = SchemaType.userSchema
    override fun isUserVisible(): Boolean = true
    override fun getSchemaFile(): VirtualFile? {
        return JsonSchemaProviderFactory.getResourceFile(ActonTomlJsonSchemaFileProvider::class.java, SCHEMA_PATH)
    }

    companion object {
        private const val SCHEMA_PATH: String = "/jsonSchema/acton-toml-schema.json"
    }
}
