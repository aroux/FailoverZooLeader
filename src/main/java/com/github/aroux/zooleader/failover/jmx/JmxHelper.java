package com.github.aroux.zooleader.failover.jmx;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.apache.log4j.Logger;

import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;

public class JmxHelper {

	private final static Logger logger = Logger.getLogger(JmxHelper.class);

	private static final String CONNECTOR_ADDRESS = "com.sun.management.jmxremote.localConnectorAddress";

	private JMXConnector jmxConnector;
	private MBeanServerConnection mbeanConnection;
	private VirtualMachine vm;

	public void connectToPid(String pid) {
		try {
			vm = VirtualMachine.attach(pid);
			// get the connector address
			String connectorAddress = vm.getAgentProperties().getProperty(CONNECTOR_ADDRESS);

			// no connector address, so we start the JMX agent
			if (connectorAddress == null) {
				String agent = vm.getSystemProperties().getProperty("java.home") + File.separator + "lib" + File.separator
						+ "management-agent.jar";
				vm.loadAgent(agent);

				// agent is started, get the connector address
				connectorAddress = vm.getAgentProperties().getProperty(CONNECTOR_ADDRESS);
			}

			// establish connection to connector server
			JMXServiceURL jmxServiceURL = new JMXServiceURL(connectorAddress);
			jmxConnector = JMXConnectorFactory.connect(jmxServiceURL, null);
		} catch (AttachNotSupportedException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (AgentLoadException e) {
			throw new RuntimeException(e);
		} catch (AgentInitializationException e) {
			throw new RuntimeException(e);
		}
	}

	public boolean mbeanExists(String pattern) {
		try {
			mbeanConnection = jmxConnector.getMBeanServerConnection();
			ObjectName objectName = new ObjectName(pattern);
			Set<ObjectName> beanSet = mbeanConnection.queryNames(objectName, null);
			return !beanSet.isEmpty();
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (NullPointerException e) {
			throw new RuntimeException(e);
		} catch (MalformedObjectNameException e) {
			throw new RuntimeException(e);
		}
	}

	public void disconnect() {
		try {
			vm.detach();
			jmxConnector.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
