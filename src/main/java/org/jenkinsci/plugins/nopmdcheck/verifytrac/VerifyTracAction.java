package org.jenkinsci.plugins.nopmdcheck.verifytrac;

import hudson.model.Action;
import hudson.model.AbstractBuild;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jenkinsci.plugins.nopmdcheck.NopmdCheckResultAction;
import org.jenkinsci.plugins.nopmdcheck.model.CheckResult;
import org.jenkinsci.plugins.nopmdcheck.model.LineHolder;

public class VerifyTracAction implements Action {
	
	public static final int TYPE_OK = 0;
	public static final int TYPE_NOT_CLOSE = 1;
	public static final int TYPE_NO_TICKET = 2;

	private AbstractBuild<?, ?> owner;
	
	private Map<String, Integer> typeMap;

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
	
	public List<CheckResult> getNgList(){
		NopmdCheckResultAction action = owner.getAction(NopmdCheckResultAction.class);
		if(action == null){
			return null;
		}
		
		List<CheckResult> results = new ArrayList<CheckResult>();
		for(CheckResult result:action.getResultList()){
			CheckResult hoge = new CheckResult();
			List<LineHolder> lineHolders = new ArrayList<LineHolder>();
			for(LineHolder lineHolder:result.getLineHolders()){
				int status = typeMap.get(lineHolder.getHashcode());
				if(status != TYPE_OK){
					lineHolders.add(lineHolder);
				}
			}
			if(lineHolders.size() > 0){
				hoge.setName(result.getName());
				hoge.setLineHolders(lineHolders);
				results.add(hoge);
			}
		}
		
		return results;
	}

}
