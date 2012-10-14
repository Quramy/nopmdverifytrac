package org.jenkinsci.plugins.nopmdcheck.verifytrac;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.scm.SCMRevisionState;
import hudson.scm.SVNRevision;
import hudson.scm.SubversionRepositoryStatus;
import hudson.scm.SCM;
import hudson.scm.SubversionSCM;
import hudson.scm.SubversionSCM.SvnInfo;
import hudson.scm.SubversionTagAction;
import hudson.scm.SubversionSCM.ModuleLocation;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.json.JSONObject;

import org.jenkinsci.plugins.nopmdcheck.NopmdCheckResultAction;
import org.jenkinsci.plugins.nopmdcheck.model.CheckResult;
import org.jenkinsci.plugins.nopmdcheck.model.LineHolder;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.tmatesoft.svn.core.SVNException;

public class VerifyTracPublisher extends Publisher {

	private String ticketPattern;

	private String tracUrl;

	private String user;

	private String password;

	public String getTicketPattern() {
		return ticketPattern;
	}

	public void setTicketPattern(String ticketPattern) {
		this.ticketPattern = ticketPattern;
	}

	public String getTracUrl() {
		return tracUrl;
	}

	public void setTracUrl(String tracUrl) {
		this.tracUrl = tracUrl;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	@DataBoundConstructor
	public VerifyTracPublisher(String ticketPattern, String tracUrl, String user, String password) {
		this.ticketPattern = ticketPattern;
		this.tracUrl = tracUrl;
		this.user = user;
		this.password = password;
	}

	@Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
			throws InterruptedException {
		//
		NopmdCheckResultAction action = build.getAction(NopmdCheckResultAction.class);

		if (action == null) {
			listener.getLogger().println("cannot find nopmd-check-plugin result.");
			return false;
		}
		List<CheckResult> resultList = action.getResultList();

		Set<Integer> idSet = new TracClient(this.tracUrl, this.user, this.password).getClosedSet();

		Map<String, Integer> typeMap = new HashMap<String, Integer>();
		Pattern p = Pattern.compile(this.ticketPattern);

		for (CheckResult result : resultList) {
			for (LineHolder line : result.getLineHolders()) {
				Matcher m = p.matcher(line.getComment());
				if (m.find() && m.groupCount() > 0) {
					Integer ticketId = Integer.parseInt(m.group(1));
					if (idSet.contains(ticketId)) {
						// TODO replace OK code
						// listener.getLogger().println(ticketId + ", OK!");
						typeMap.put(line.getHashcode(), VerifyTracAction.TYPE_OK);
					} else {
						typeMap.put(line.getHashcode(), VerifyTracAction.TYPE_NOT_CLOSE);
					}
				} else {
					typeMap.put(line.getHashcode(), VerifyTracAction.TYPE_NO_TICKET);
				}
			}
		}

//		SCM scm = build.getProject().getScm();
//		SubversionSCM sscm = null;
//		listener.getLogger().println(scm.getType());
		String svnUrl = null;
//		Long revision;

		File f = new File(build.getRootDir(), "revision.txt");
		if(f.exists()){
			listener.getLogger().println("revision!");
			try {
				BufferedReader br = new BufferedReader(new FileReader(f));
				svnUrl=br.readLine();
//				listener.getLogger().println(urlInfo);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		SCMRevisionState state =build.getAction(SCMRevisionState.class);
		if(state != null){
//			revision = SVNRevision.rev(state, svnUrl);
//			listener.getLogger().println(revision);
		}
		
		VerifyTracAction verifyTracAction;
		verifyTracAction = new VerifyTracAction(build);
		verifyTracAction.setTypeMap(typeMap);
		verifyTracAction.setSvnUrl(svnUrl);
		build.addAction(verifyTracAction);

		return true;
	}

	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.NONE;
	}

	@Override
	public DescriptorImpl getDescriptor() {
		return (DescriptorImpl) super.getDescriptor();
	}

	@Extension
	public static final class DescriptorImpl extends Descriptor<Publisher> {

		public boolean isApplicable(Class<? extends AbstractProject<?, ?>> aClass) {
			return true;
		}

		public DescriptorImpl() {
			super(VerifyTracPublisher.class);
		}

		@Override
		public String getDisplayName() {
			return "verify NOPMD correspoing ticket";
		}

		@Override
		public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
			// To persist global configuration information,
			// set that to properties and call save().
			// ^Can also use req.bindJSON(this, formData);
			// (easier when there are many fields; need set* methods for this,
			// like setUseFrench)
			save();
			return super.configure(req, formData);
		}

	}
}
