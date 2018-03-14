package cs.ut.ui.controllers

import cs.ut.configuration.ConfigurationReader
import cs.ut.logging.NirdizatiLogger
import cs.ut.providers.Dir
import cs.ut.providers.DirectoryConfiguration
import cs.ut.ui.UIComponent
import cs.ut.ui.controllers.modal.ParameterModalController.Companion.FILE
import org.apache.commons.io.FilenameUtils
import org.zkoss.util.media.Media
import org.zkoss.util.resource.Labels
import org.zkoss.zk.ui.Component
import org.zkoss.zk.ui.Executions
import org.zkoss.zk.ui.event.UploadEvent
import org.zkoss.zk.ui.select.SelectorComposer
import org.zkoss.zk.ui.select.annotation.Listen
import org.zkoss.zk.ui.select.annotation.Wire
import org.zkoss.zul.Button
import org.zkoss.zul.Label
import org.zkoss.zul.Vbox
import org.zkoss.zul.Window
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.Reader

class UploadLogController : SelectorComposer<Component>(), Redirectable, UIComponent {
    private val log = NirdizatiLogger.getLogger(UploadLogController::class.java, getSessionId())

    @Wire
    private lateinit var fileName: Label

    @Wire
    private lateinit var upload: Button

    @Wire
    private lateinit var fileNameCont: Vbox

    private lateinit var media: Media

    private val allowedExtensions = ConfigurationReader.findNode("fileUpload/extensions").itemListValues()

    /**
     * Method that analyzes uploaded file. Checks that the file has required extension.
     *
     * @param event upload event where media should be retrieved from
     */
    @Listen("onUpload = #chooseFile, #dropArea")
    fun analyzeFile(event: UploadEvent) {
        upload.isDisabled = true
        log.debug("Upload event. Analyzing file")

        val uploaded = event.media ?: return

        if (FilenameUtils.getExtension(uploaded.name) in allowedExtensions) {
            log.debug("Log is in allowed format")
            fileNameCont.sclass = "file-upload"
            fileName.value = uploaded.name
            media = uploaded
            upload.isDisabled = false
        } else {
            log.debug("Log is not in allowed format -> showing error")
            fileNameCont.sclass = "file-upload-err"
            fileName.value = Labels.getLabel(
                "upload.wrong.format",
                arrayOf(uploaded.name, FilenameUtils.getExtension(uploaded.name))
            )
            upload.isDisabled = true
        }
    }

    /**
     * Listener - user log has been accepted and now we need to generate data set parameters for it
     */
    @Listen("onClick = #upload")
    fun processLog() {
        val runnable = Runnable {
            val tmpDir = DirectoryConfiguration.dirPath(Dir.TMP_DIR)
            val file = File(tmpDir + media.name.replace("/", "_"))
            log.debug("Creating file: ${file.absolutePath}")
            file.createNewFile()

            FileOutputStream(file).use {
                if (media.isBinary) {
                    it.write(media.streamData.readBinary())
                } else {
                    it.write(media.readerData.readFromStream())
                }
            }

            val args = mapOf(FILE to file)
            val window: Window = Executions.createComponents(
                "/views/modals/params.zul",
                self,
                args
            ) as Window
            if (self.getChildren<Component>().contains(window)) {
                window.doModal()
                upload.isDisabled = true
            }
        }
        runnable.run()
        log.debug("Started training file generation thread")
    }

    private fun Reader.readFromStream(): ByteArray {
        log.debug("Reading bytes from stream")
        val start = System.currentTimeMillis()

        val buffer = CharArray(1024)
        val bytes = mutableListOf<Byte>()

        while (this.read(buffer) == buffer.size) {
            buffer.forEach { bytes.add(it.toByte()) }
        }

        val end = System.currentTimeMillis()
        log.debug("Finished reading bytes in ${end - start} ms. Read ${bytes.size} bytes total")
        return bytes.toByteArray()
    }

    private fun InputStream.readBinary(): ByteArray {
        log.debug("Started reading binary data from input stream")
        val start = System.currentTimeMillis()

        val buffer = ByteArray(1024)
        val bytes = mutableListOf<Byte>()


        while (this.read(buffer) == buffer.size) {
            buffer.forEach { bytes.add(it) }
        }

        val end = System.currentTimeMillis()
        log.debug("Finished reading bytes from input stream in ${end - start} ms. Read ${bytes.size} bytes total.")

        return bytes.toByteArray()
    }
}

