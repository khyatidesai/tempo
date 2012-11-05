/**
 * Copyright (c) 2005-2006 Intalio inc.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Intalio inc. - initial API and implementation
 *
 * $Id: VacationController.java 5440 2006-06-09 08:58:15Z imemruk $
 * $Log:$
 */

/**
 *  This java file acts as a controller to vacation management insert's ,selects & delete's the vacation details of a particular user  
 */
package org.intalio.tempo.uiframework;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.intalio.tempo.web.ApplicationState;
import org.intalio.tempo.workflow.task.Vacation;
import org.intalio.tempo.workflow.tms.ITaskManagementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import org.springframework.web.servlet.view.json.JsonView;

public class VacationController implements Controller {
	private static final Logger LOG = LoggerFactory.getLogger(VacationController.class);
	JsonView json = null;
	Map<String, Object> model = null;
	String message = "Failure";
	String _endpoint = Configuration.getInstance().getServiceEndpoint();
	ITaskManagementService taskManager = null;
	SimpleDateFormat df = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy");
	SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy");

	public VacationController(JsonView json) {
		this.json = json;
	}

	public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			String endpoint = URIUtils.resolveURI(request, _endpoint);
			String pToken = getParticipantToken(request);
			taskManager = Configuration.getInstance().getTmsFactory().getService(endpoint, pToken);
			String name = null;
			ApplicationState appState = ApplicationState.getCurrentInstance(request);
			if (appState != null)
				name = appState.getCurrentUser().getName();
			model = new LinkedHashMap<String, Object>();
			if (request.getParameter("action") != null && request.getParameter("action").equalsIgnoreCase("Validate") && name!=null) {
					model = getVacationDetails(name);
			} else if (request.getParameter("action") != null
					&& request.getParameter("action").equalsIgnoreCase("endVacation")) {
				if (request.getParameter("id") != null)
					model = deleteVacationDetails(request.getParameter("id"));
			} else if (request.getParameter("action") != null
					&& request.getParameter("action").equalsIgnoreCase("insertVacation") && name!=null) {
				if (request.getParameter("fromDate") != null && request.getParameter("toDate") != null
						&& request.getParameter("desc") != null)
					model = insertVacationDetails(request.getParameter("fromDate"), request.getParameter("toDate"),
							request.getParameter("desc").trim(), name);
			}
		} catch (Exception e) {
			message = e.getMessage();
			LOG.error("Failed to execute action. " + e.getMessage(), e);
		}
		return new ModelAndView(json, model);
	}

	public Map<String, Object> getVacationDetails(String user) {
		try {
			List<Vacation> vac = taskManager.getUserVacation(user);
			if (vac.size() >= 1) {
				for (int i = 0; i < vac.size(); i++) {
					model.put("vacId", vac.get(i).getId());
					model.put("vacDesc", vac.get(i).getDescription());
					model.put("vacFromdate", format.format(df.parse(vac.get(i).getFromDate().toString())));
					model.put("vacToDate", format.format(df.parse(vac.get(i).getToDate().toString())));
					model.put("vacUser", vac.get(i).getUser());
				}
			}
		} catch (ParseException e) {
			LOG.error("Failed to parse. " + e.getMessage(), e);
		} catch (Exception e) {
			LOG.error("Exception while fetching vacation record. " + e.getMessage(), e);
		}
		return model;
	}

	public Map<String, Object> deleteVacationDetails(String id) {
		taskManager.deleteVacation(id);
		message = "Deleted";
		model.put("message", message);
		return model;
	}

	public Map<String, Object> insertVacationDetails(String fromDate, String toDate, String description, String user) {
		taskManager.insertVacation(fromDate, toDate, description, user);
		message = "Inserted";
		model.put("message", message);
		return model;
	}

	protected String getParticipantToken(HttpServletRequest request) {
		ApplicationState state = ApplicationState.getCurrentInstance(request);
		return state.getCurrentUser().getToken();
	}

}