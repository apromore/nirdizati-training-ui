package cs.ut.ui.adapters

import cs.ut.configuration.ConfigurationReader
import cs.ut.engine.JobManager
import cs.ut.engine.item.ModelParameter
import cs.ut.jobs.Job
import cs.ut.jobs.JobStatus
import cs.ut.jobs.SimulationJob
import cs.ut.providers.Dir
import cs.ut.providers.DirectoryConfiguration
import cs.ut.ui.FieldComponent
import cs.ut.ui.GridValueProvider
import cs.ut.ui.NirdizatiGrid
import cs.ut.ui.controllers.JobTrackerController
import cs.ut.ui.controllers.Redirectable
import cs.ut.util.Algorithm
import cs.ut.util.NirdizatiDownloader
import cs.ut.util.NirdizatiTranslator
import cs.ut.util.Page
import cs.ut.util.TRACKER_EAST
import org.zkoss.zk.ui.Component
import org.zkoss.zk.ui.Executions
import org.zkoss.zk.ui.event.Event
import org.zkoss.zk.ui.event.Events
import org.zkoss.zul.*

/**
 * Adapter for job tracker grid
 */
class JobValueAdapter : GridValueProvider<Job, Row>, Redirectable {
    companion object {
        const val jobArg = "JOB"
        val AVERAGE = ConfigurationReader.findNode("defaultValues").valueWithIdentifier("average").value
    }

    override var fields: MutableList<FieldComponent> = mutableListOf()

    override fun provide(data: Job): Row {
        val status = Label(data.status.name)

        val row = Row()
        row.valign = "end"

        val label = row.formJobLabel(data)

        row.appendChild(label)
        row.setValue(data)

        fields.add(FieldComponent(label, status))

        return row
    }

    /**
     * Create identifier label for a job
     */
    private fun Job.identifierLabel(): Label {
        val label = Label(this.id)
        label.sclass = "extra-small"
        return label
    }

    /**
     * Create result label for a job
     *
     * @return hlayout component with labels
     */
    private fun ModelParameter.generateResultLabel(): Hlayout {
        val hlayout = Hlayout()

        val label = Label(NirdizatiTranslator.localizeText("property.outcome"))
        label.sclass = "param-label"

        val outcome = Label(NirdizatiTranslator.localizeText(if (this.translate) this.getTranslateName() else this.id))
        hlayout.appendChild(label)
        hlayout.appendChild(outcome)

        return hlayout
    }

    /**
     * Create layout that contains job metadata
     *
     * @param job to use as datasource
     *
     * @return vlayout component with data
     */
    private fun Row.formJobLabel(job: Job): Vlayout {
        job as SimulationJob

        val encoding = job.encoding
        val bucketing = job.bucketing
        val learner = job.learner
        val outcome = job.outcome

        val label = Label(
                NirdizatiTranslator.localizeText(encoding.type + "." + encoding.id) + "\n" +
                        NirdizatiTranslator.localizeText(bucketing.type + "." + bucketing.id) + "\n" +
                        NirdizatiTranslator.localizeText(learner.type + "." + learner.id)
        )
        label.isPre = true
        label.sclass = "param-label"

        val outcomeText = "" + if (outcome.id == Algorithm.OUTCOME.value) NirdizatiTranslator.localizeText("threshold.threshold_msg") + ": " +
                (if (outcome.parameter == AVERAGE) NirdizatiTranslator.localizeText("threshold.avg").toLowerCase()
                else outcome.parameter) + "\n"
        else ""

        val bottom: Label = learner.formHyperParamRow()
        bottom.value = outcomeText + bottom.value

        val fileLayout = job.generateFileInfo()
        fileLayout.hflex = "1"

        val labelsContainer = Vlayout()
        labelsContainer.appendChild(fileLayout)
        labelsContainer.appendChild(job.outcome.generateResultLabel())
        labelsContainer.hflex = "1"

        val fileContainer = Hlayout()
        fileContainer.appendChild(labelsContainer)

        val btnContainer = Hbox()
        btnContainer.hflex = "min"
        btnContainer.pack = "end"
        btnContainer.appendChild(job.generateRemoveBtn(this))
        fileContainer.appendChild(btnContainer)
        fileContainer.hflex = "1"

        val vlayout = Vlayout()
        vlayout.appendChild(fileContainer)

        vlayout.appendChild(job.generateStatus(label))
        vlayout.appendChild(bottom)
        vlayout.appendChild(job.identifierLabel())

        val hlayout = Hlayout()
        hlayout.appendChild(job.getVisualizeBtn())
        hlayout.appendChild(job.getDeployBtn())
        vlayout.appendChild(hlayout)

        return vlayout
    }

    /**
     * Create layout that hold job status
     * @param label to put inside the layout
     */
    private fun SimulationJob.generateStatus(label: Label): Hlayout {
        val labelStatusContainer = Hlayout()
        val labelContainer = Hlayout()
        labelContainer.appendChild(label)
        label.hflex = "1"
        labelStatusContainer.appendChild(labelContainer)

        val status = Label(this.status.name)
        val statusContainer = Hbox()
        statusContainer.appendChild(status)
        statusContainer.hflex = "1"
        statusContainer.vflex = "1"
        statusContainer.pack = "end"
        statusContainer.align = "center"
        labelStatusContainer.appendChild(statusContainer)

        return labelStatusContainer
    }

    /**
     * Create layout with the file name
     *
     * @return hlayout with file name label
     */
    private fun SimulationJob.generateFileInfo(): Hlayout {
        val fileLabel = Label(NirdizatiTranslator.localizeText("attribute.log_file"))
        fileLabel.sclass = "param-label"

        val file = Label(this.logFile.name)

        val fileLayout = Hlayout()
        fileLayout.appendChild(fileLabel)
        fileLayout.appendChild(file)
        return fileLayout
    }

    /**
     * Create job removal button
     *
     * @param row that will be removed when button is clicked
     *
     * @return button that will detach the row on click
     */
    @Suppress("UNCHECKED_CAST")
    private fun SimulationJob.generateRemoveBtn(row: Row): Button {
        val btn = Button("x")
        btn.vflex = "min"
        btn.hflex = "min"
        btn.sclass = "job-remove"

        val client = Executions.getCurrent().desktop
        btn.addEventListener(Events.ON_CLICK, { _ ->
            val grid: NirdizatiGrid<Job> =
                    client.components.firstOrNull { it.id == JobTrackerController.GRID_ID } as NirdizatiGrid<Job>

            JobManager.stopJob(this)
            Executions.schedule(
                    client,
                    { _ ->
                        row.detach()
                        if (grid.rows.getChildren<Component>().isEmpty()) {
                            client.components.first { it.id == TRACKER_EAST }.isVisible = false
                        }
                    },
                    Event("abort_job", null, null)
            )
        })

        return btn
    }

    /**
     * Create visualization button that will redirect user to job visualization page
     *
     * @return button component
     */
    private fun SimulationJob.getVisualizeBtn(): Button {
        val visualize = Button(NirdizatiTranslator.localizeText("job_tracker.visualize"))
        visualize.sclass = "n-btn"
        visualize.hflex = "1"
        visualize.isDisabled = !(JobStatus.COMPLETED == this.status || JobStatus.FINISHING == this.status)

        visualize.addEventListener(Events.ON_CLICK, { _ ->
            Executions.getCurrent().setAttribute(jobArg, this)
            setContent(Page.VALIDATION.value, Executions.getCurrent().desktop.firstPage)
        })

        return visualize
    }

    private fun SimulationJob.getDeployBtn(): Button {
        val deploy = Button(NirdizatiTranslator.localizeText("job_tracker.deploy_to_runtime"))
        deploy.isDisabled = true
        deploy.sclass = "n-btn"
        deploy.vflex = "min"
        deploy.hflex = "1"

        deploy.addEventListener(Events.ON_CLICK, { _ ->
            NirdizatiDownloader(Dir.PKL_DIR, this.id).execute()
        })

        return deploy
    }

    /**
     * Create row based on hyper parameters
     *
     * @return label that contains job hyper parameter information
     */
    private fun ModelParameter.formHyperParamRow(): Label {
        var label = ""
        val iterator = this.properties.iterator()

        while (iterator.hasNext()) {
            val prop = iterator.next()
            label += NirdizatiTranslator.localizeText("property." + prop.id) + ": " + prop.property + "\n"
        }

        val res = Label(label)
        res.sclass = "small-font"
        res.isMultiline = true
        res.isPre = true
        return res
    }
}