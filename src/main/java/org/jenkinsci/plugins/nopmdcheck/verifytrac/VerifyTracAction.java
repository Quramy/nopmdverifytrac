package org.jenkinsci.plugins.nopmdcheck.verifytrac;

import hudson.model.Action;

public class VerifyTracAction implements Action {

	public String getIconFileName() {
		return "document.gif";
	}

	public String getDisplayName() {
		return "Nopmd check result";
	}

	public String getUrlName() {
		return "nopmdCheckResult";
	}

}
