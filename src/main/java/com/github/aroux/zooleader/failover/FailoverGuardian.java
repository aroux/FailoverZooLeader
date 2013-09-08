package com.github.aroux.zooleader.failover;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;
import org.apache.zookeeper.ZooKeeper;

import com.github.aroux.zooleader.algorithm.DistributedPrimitivesService;
import com.github.aroux.zooleader.algorithm.LeaderNotifier;
import com.github.aroux.zooleader.algorithm.LeaderService;
import com.github.aroux.zooleader.algorithm.impl.LeaderServiceImpl;
import com.github.aroux.zooleader.algorithm.impl.ZookeeperService;
import com.github.aroux.zooleader.client.GuidFactory;
import com.github.aroux.zooleader.client.ZooKeeperFactory;
import com.github.aroux.zooleader.client.ZooLeaderClient;
import com.github.aroux.zooleader.failover.jmx.JmxHelper;
import com.github.aroux.zooleader.server.ZooLeaderServer;

public class FailoverGuardian {

	private final static Logger logger = Logger.getLogger(FailoverGuardian.class);

	private static ZooLeaderServer server;

	private static ZooLeaderClient client;

	public static void main(String[] args) throws IOException, ExecutionException, InterruptedException {

		// BasicConfigurator.configure();

		String quorumConfig = args[0];

		server = new ZooLeaderServer(quorumConfig);
		server.start(1000);

		waitForConnection();

		client = new ZooLeaderClient(new LeaderNotifier() {
			@Override
			public void notifyLeadership(boolean leader) {
				logger.info("My leader status : " + leader);
			}
		});

		ZooKeeper zooKeeper = ZooKeeperFactory.createInstance(quorumConfig, 3000, client);
		DistributedPrimitivesService primitiveService = new ZookeeperService(zooKeeper, "MY_APP", GuidFactory.createInstance());
		LeaderService leaderService = new LeaderServiceImpl(primitiveService, Executors.defaultThreadFactory());

		client.setLeaderService(leaderService);
		Thread.sleep(5000);
		client.start();

		Thread.currentThread().join();
	}

	private static boolean waitForConnection() throws InterruptedException {
		String pid = ManagementFactory.getRuntimeMXBean().getName();
		JmxHelper jmxHelper = new JmxHelper();
		jmxHelper.connectToPid(pid.split("@")[0]);
		while (jmxHelper.mbeanExists("org.apache.ZooKeeperService:name0=ReplicatedServer_id*,name1=replica.*,name2=LeaderElection")) {
			Thread.sleep(500);
			logger.info("Waiting for zookeeper quorum to start.");
		}

		jmxHelper.disconnect();

		return false;
	}
}
