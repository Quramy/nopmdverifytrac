package org.jenkinsci.plugins.nopmdcheck.verifytrac;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.apache.xmlrpc.client.XmlRpcCommonsTransportFactory;

public class TracClient {

	private XmlRpcClient client;

	private static final String QUERY_CLOSED = "ticket.query";
	private static final Object[] PARAM_CLOSED = { "status=closed" };

	public TracClient(String url, String user, String password) {

		client = new XmlRpcClient();

		client.setTransportFactory(new XmlRpcCommonsTransportFactory(client));

		XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
		config.setBasicUserName(user);
		config.setBasicPassword(password);
		if (url.charAt(url.length() - 1) != '/') {
			url = url + "/";
		}
		try {
			config.setServerURL(new URL(url + "login/xmlrpc"));
			client.setConfig(config);

		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}

	public Set<Integer> getClosedSet() {
		Object[] queryResult = null;
		Set<Integer> res = new HashSet<Integer>();
		try {
			queryResult = (Object[]) client.execute(QUERY_CLOSED, PARAM_CLOSED);
			for (Object id : queryResult) {
				res.add((Integer) id);
			}
		} catch (XmlRpcException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return res;

	}
}
