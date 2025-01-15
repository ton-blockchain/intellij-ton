package org.ton.intellij.tolk.sdk

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class TolkSdkManager(
    private val project: Project
) {
    private var sdkRef: TolkSdkRef? = null

    fun getSdkRef(): TolkSdkRef {
        var sdk = sdkRef
        if (sdk == null) {
            val propertiesComponent = PropertiesComponent.getInstance(project)
            val reference = propertiesComponent.getValue("tolk_sdk_path")
            sdk = TolkSdkRef(reference ?: "")
            sdkRef = sdk
        }
        return sdk
    }

    fun setSdkRef(sdkRef: TolkSdkRef?) {
        val propertiesComponent = PropertiesComponent.getInstance(project)
        this.sdkRef = sdkRef
        if (sdkRef == null) {
            propertiesComponent.unsetValue("tolk_sdk_path")
        } else {
            propertiesComponent.setValue("tolk_sdk_path", sdkRef.referenceName)
        }
    }

    companion object {
        operator fun get(project: Project): TolkSdkManager = project.getService(TolkSdkManager::class.java)
    }
}
