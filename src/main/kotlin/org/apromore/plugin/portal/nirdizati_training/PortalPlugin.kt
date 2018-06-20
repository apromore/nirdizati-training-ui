/*
 * Copyright © 2009-2018 The Apromore Initiative.
 *
 * This file is part of "Apromore".
 *
 * "Apromore" is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 3 of the
 * License, or (at your option) any later version.
 *
 * "Apromore" is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program.
 * If not, see <http://www.gnu.org/licenses/lgpl-3.0.html>.
 */

package org.apromore.plugin.portal.nirdizati_training

// Java 2 Standard Edition
import java.util.Locale

// Java 2 Enterprise Edition
import javax.inject.Inject

// Third party packages
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.zkoss.zk.ui.Executions
import org.zkoss.zk.ui.Sessions
import org.zkoss.zul.ListModelList
import org.zkoss.zul.Messagebox

// Local packages
import org.apromore.plugin.portal.DefaultPortalPlugin
import org.apromore.plugin.portal.PortalContext
import org.apromore.service.predictivemonitor.PredictiveMonitorService
import org.apromore.service.predictivemonitor.Predictor

@Component
public class PortalPlugin() : DefaultPortalPlugin() {

    private val LOGGER = LoggerFactory.getLogger("org.apromore.plugin.portal.nirdizati_training.PortalPlugin")

    private var label      = "Train Predictor with Log(2)"
    private var groupLabel = "Monitor"

    @Inject private val predictiveMonitorService : PredictiveMonitorService? = null

    override public fun getLabel(locale: Locale): String {
        return label
    }

    public fun setLabel(label: String) {
        this.label = label
    }

    override public fun getGroupLabel(locale: Locale): String {
        return groupLabel
    }

    public fun setGroupLabel(groupLabel: String) {
        this.groupLabel = groupLabel
    }

    override public fun execute(portalContext: PortalContext) {

        LOGGER.info("Execute predictor UI with " + predictiveMonitorService!!.getPredictors().size + " predictors")
        Sessions.getCurrent().setAttribute("predictiveMonitorService", predictiveMonitorService)
        Executions.sendRedirect("/nirdizati-training-ui/#training")
    }
}
