package cs.ut.controllers

import cs.ut.ui.NirdizatiGrid
import cs.ut.ui.adapters.JobValueAdataper
import org.zkoss.zk.ui.Component
import org.zkoss.zk.ui.select.SelectorComposer
import org.zkoss.zk.ui.select.annotation.Wire
import org.zkoss.zul.Hbox

class JobTrackerController : SelectorComposer<Component>(), Redirectable {

    @Wire
    private lateinit var jobTracker: Hbox

    companion object {
        const val GRID_ID = "tracker_grid"
        const val TRACKER = "tracker"
    }

    override fun doAfterCompose(comp: Component?) {
        super.doAfterCompose(comp)

        val jobGrid = NirdizatiGrid(JobValueAdataper())
        jobGrid.isVisible = false
        jobGrid.vflex = "1"
        jobGrid.id = GRID_ID
        jobTracker.appendChild(jobGrid)
    }
}