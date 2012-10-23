package org.jenkinsci.plugins.nopmdcheck.verifytrac;

import hudson.model.Action;
import hudson.model.AbstractBuild;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.jenkinsci.plugins.nopmdcheck.NopmdCheckResultAction;
import org.jenkinsci.plugins.nopmdcheck.model.CheckResult;
import org.jenkinsci.plugins.nopmdcheck.model.LineHolder;

public class VerifyTracAction implements Action {

	public static final int TYPE_OK = 0;
	public static final int TYPE_NOT_CLOSE = 1;
	public static final int TYPE_NO_TICKET = 2;

	private AbstractBuild<?, ?> owner;

	private Map<String, Integer> typeMap;

	private String tracUrl;
	private String svnPath;

	private Integer ngCount = null;

	private Integer nopmdCount = null;

	public Integer getNgCount() {
		return ngCount;
	}

	public void setNgCount(Integer ngCount) {
		this.ngCount = ngCount;
	}

	public Integer getNopmdCount() {
		return nopmdCount;
	}

	public void setNopmdCount(Integer nopmdCount) {
		this.nopmdCount = nopmdCount;
	}

	public String getTracUrl() {
		return tracUrl;
	}

	public boolean getEnableBrowser() {
		return svnPath != null && svnPath.length() > 0 && revision != null
				& revision.length() > 0;
	}

	public String getBrowserUrl(String name) {
		return tracUrl + "/browser/" + svnPath + name + "?rev=" + revision;
	}

	public String getBrowserUrl(String name, int line) {
		return getBrowserUrl(name) + "#L" + line;
	}

	public void setTracUrl(String tracUrl) {
		this.tracUrl = tracUrl;
	}

	public String getSvnPath() {
		return svnPath;
	}

	public void setSvnPath(String svnPath) {
		this.svnPath = svnPath;
	}

	private String revision;

	public String getRevision() {
		return revision;
	}

	public void setRevision(String revision) {
		this.revision = revision;
	}

	public Map<String, Integer> getTypeMap() {
		return typeMap;
	}

	public void setTypeMap(Map<String, Integer> typeMap) {
		this.typeMap = typeMap;
	}

	public VerifyTracAction(AbstractBuild<?, ?> owner) {
		this.owner = owner;
	}

	public AbstractBuild<?, ?> getOwner() {
		return this.owner;
	}

	public String getIconFileName() {
		return "document.gif";
	}

	public String getDisplayName() {
		return "Nopmd check verify Trac result";
	}

	public String getUrlName() {
		return "nopmdCheckVerifyTracResult";
	}

	public void calcNgCount() {
		List<CheckResult> ngList = getNgList();
		int sum = 0;
		for (CheckResult checkResult : ngList) {
			sum += checkResult.getLineHolders().size();
		}
		this.ngCount = sum;
	}

	public void calcNopmdCount() {
		int sum = 0;

		for (CheckResult result : getOwnersResult()) {
			sum += result.getLineHolders().size();
		}
		this.nopmdCount = sum;
	}

	private List<CheckResult> getOwnersResult() {

		NopmdCheckResultAction action = owner
				.getAction(NopmdCheckResultAction.class);
		if (action == null) {
			return null;
		}

		return action.getResultList();
	}

	public List<CheckResult> getNgList() {
		List<CheckResult> results = new ArrayList<CheckResult>();
		for (CheckResult result : getOwnersResult()) {
			CheckResult checkResult = new CheckResult();
			List<LineHolder> lineHolders = new ArrayList<LineHolder>();
			for (LineHolder lineHolder : result.getLineHolders()) {
				int status = typeMap.get(lineHolder.getHashcode());
				if (status != TYPE_OK) {
					lineHolders.add(lineHolder);
				}
			}
			if (lineHolders.size() > 0) {
				checkResult.setName(result.getName());
				checkResult.setLineHolders(lineHolders);
				results.add(checkResult);
			}
		}

		return results;
	}

	public List<CheckResult> getResultList() {
		return getOwnersResult();
	}

	public String getResultListAsJson() {
		JSONArray jsonArray = JSONArray.fromObject(getOwnersResult());
		return jsonArray.toString();
	}

	public String getTypeMapAsJson() {
		return JSONObject.fromObject(typeMap).toString();
	}

}
