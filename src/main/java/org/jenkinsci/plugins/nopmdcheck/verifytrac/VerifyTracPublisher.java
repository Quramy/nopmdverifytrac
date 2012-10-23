package org.jenkinsci.plugins.nopmdcheck.verifytrac;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
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

public class VerifyTracPublisher extends Publisher {

	private String ticketPattern;

	private String tracUrl;

	private String user;

	private String password;

	private Integer thresholdTicketCount;

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

	public Integer getThresholdTicketCount() {
		return thresholdTicketCount;
	}

	public void setThresholdTicketCount(Integer thresholdTicketCount) {
		this.thresholdTicketCount = thresholdTicketCount;
	}

	@DataBoundConstructor
	public VerifyTracPublisher(String ticketPattern, String tracUrl,
			String user, String password, Integer thresholdTicketCount) {
		this.tracUrl = tracUrl.replaceAll("/\\s*$", "");
		this.user = user;
		this.password = password;

		if (ticketPattern == null || ticketPattern.length() == 0) {
			this.ticketPattern = "#(\\d+)";
		} else {
			this.ticketPattern = ticketPattern;
		}

		if (thresholdTicketCount == null || thresholdTicketCount <= 0) {
			this.thresholdTicketCount = 10;
		} else {
			this.thresholdTicketCount = thresholdTicketCount;
		}
	}

	@Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher,
			BuildListener listener) throws InterruptedException {
		//
		NopmdCheckResultAction action = build
				.getAction(NopmdCheckResultAction.class);

		if (action == null) {
			listener.getLogger().println(
					"Cannot find nopmd-check-plugin result.");
			return false;
		}
		List<CheckResult> resultList = action.getResultList();

		Set<Integer> idSet = new TracClient(this.tracUrl, this.user,
				this.password).getClosedSet();

		Map<String, Integer> typeMap = new HashMap<String, Integer>();
		Pattern p = Pattern.compile(this.ticketPattern);

		Map<Integer, Integer> ticketCountMap = new HashMap<Integer, Integer>();
		for (CheckResult result : resultList) {
			for (LineHolder line : result.getLineHolders()) {
				Matcher m = p.matcher(line.getComment());
				if (m.find() && m.groupCount() > 0) {
					Integer ticketId = Integer.parseInt(m.group(1));
					if (idSet.contains(ticketId)) {
						// TODO replace OK code
						// listener.getLogger().println(ticketId + ", OK!");
						typeMap.put(line.getHashcode(),
								VerifyTracAction.TYPE_OK);
					} else {
						typeMap.put(line.getHashcode(),
								VerifyTracAction.TYPE_NOT_CLOSE);
					}
					Integer count = ticketCountMap.get(ticketId);
					if (count != null) {
						ticketCountMap.put(ticketId, count + 1);
					} else {
						ticketCountMap.put(ticketId, 1);
					}
				} else {
					typeMap.put(line.getHashcode(),
							VerifyTracAction.TYPE_NO_TICKET);
				}
			}
		}
		

		VerifyTracAction verifyTracAction;
		verifyTracAction = new VerifyTracAction(build);
		verifyTracAction.setTypeMap(typeMap);
		
		//TODO rm
		listener.getLogger().println(this.thresholdTicketCount);
		verifyTracAction.setThresholdTicketCount(thresholdTicketCount);
		Map<Integer, Integer> resultMap = new HashMap<Integer, Integer>();
		for(Integer tickeId:ticketCountMap.keySet()){
			Integer count = ticketCountMap.get(tickeId);
			if(count >= this.thresholdTicketCount){
//				ticketCountMap.remove(tickeId);
				resultMap.put(tickeId, count);
			}
		}
		verifyTracAction.setTicketCountMap(resultMap);
		

		SvnInfo svnInfo = fetchSvnInfo(build);
		if (svnInfo != null) {
			verifyTracAction.setRevision(svnInfo.revision);
			verifyTracAction.setSvnPath(svnInfo.rootPath);
			listener.getLogger().println(
					"Success to fetch SVN info from SCM plugin result.");
			listener.getLogger().println("SVN path: " + svnInfo.rootPath);
			listener.getLogger().println("SVN revision" + svnInfo.revision);
		} else {
			listener.getLogger().println(
					"Fail to fetch SVN info from SCM plugin result.");
		}
		verifyTracAction.setTracUrl(tracUrl);

		verifyTracAction.calcNopmdCount();
		verifyTracAction.calcNgCount();

		listener.getLogger().println(
				"All 'NOPMD' count: " + verifyTracAction.getNopmdCount());
		listener.getLogger().println(
				"NG 'NOPMD' count: " + verifyTracAction.getNgCount());

		boolean isSuccess = verifyTracAction.getNgCount() == 0;
		if (!isSuccess) {
			listener.getLogger().println("NOPMD check verify trac failed.");
			listener.getLogger()
					.println(
							"There are one more 'NOPMD' whose ticket does not exits nor not close...");
		}

		build.addAction(verifyTracAction);

		return isSuccess;
	}

	/**
	 * fetch SVN url and revision from revision.txt.
	 * 
	 * @param build
	 * @return
	 */
	private SvnInfo fetchSvnInfo(AbstractBuild<?, ?> build) {

		// "http://com.example/svn/trunk/fooo/100" in revision.txt.
		// The last path fragment is revision.
		File f = new File(build.getRootDir(), "revision.txt");
		if (f.exists()) {
			SvnInfo svnInfo = new SvnInfo();

			String rev = "";
			String svnUrl = "";
			String prjName = "";
			try {
				BufferedReader br = new BufferedReader(new FileReader(f));
				svnUrl = br.readLine();
				// System.out.println(svnUrl);
				Pattern pRev = Pattern.compile("\\d+\\s*$");
				Matcher m = pRev.matcher(svnUrl);

				if (m.find()) {
					// System.out.println("match!");
					rev = m.group().trim();
					svnInfo.revision = rev;
				}

				// If tracUrl is "http://com.example/trac/hogehoge", prjName is
				// "hogehoge".
				Pattern pPrj = Pattern.compile("[^/]+$");
				Matcher mPrj = pPrj.matcher(tracUrl);
				if (mPrj.find()) {
					prjName = mPrj.group();
					// System.out.println(prjName);
				}

				// "http://com.example/svn/hogehoge/trunk/fooo/100" ->
				// "trunk/fooo"
				svnInfo.rootPath = svnUrl.replaceAll("/" + rev, "")
						.replaceFirst(".*" + prjName + "/", "");
				br.close();
				// System.out.println(svnInfo.rootPath + ',' +
				// svnInfo.revision);

			} catch (IOException e) {
			} finally {
			}
			return svnInfo;
		} else {
			return null;
		}

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

		public boolean isApplicable(
				Class<? extends AbstractProject<?, ?>> aClass) {
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
		public boolean configure(StaplerRequest req, JSONObject formData)
				throws FormException {
			// To persist global configuration information,
			// set that to properties and call save().
			// ^Can also use req.bindJSON(this, formData);
			// (easier when there are many fields; need set* methods for this,
			// like setUseFrench)
			save();
			return super.configure(req, formData);
		}

	}

	class SvnInfo {
		String revision = "";
		String rootPath = "";
	}
}
