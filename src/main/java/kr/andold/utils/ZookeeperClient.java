package kr.andold.utils;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.apache.zookeeper.AddWatchMode;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs.Ids;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import org.apache.zookeeper.ZooKeeper;

@Slf4j
public class ZookeeperClient implements Watcher {
	private static final String ZNODE_PREFIX = "c_";

	@Getter private boolean isMaster = false;
	@Getter private String currentZNodeName = "";

	private ZooKeeper zookeeper;
	private String connectString;
	private String znodeElectPath;

	public String status(boolean logging) {
		List<String> children = null;
		try {
			children = zookeeper.getChildren(znodeElectPath, false);
			if (children != null) {
				Collections.sort(children);
			}
		} catch (Exception e) {
		}

		String result = String.format("『%s』『%b』『%s』", currentZNodeName, isMaster, children);
		if (logging) {
			log.info("{} {} status({})", Utility.indentMiddle(), result, logging);
		}

		return result;
	}

	public void run(String connectString, String znodeElectPath) {
		log.info("{} run(『{}』, 『{}』)", Utility.indentStart(), connectString, znodeElectPath);

		this.connectString = connectString;
		this.znodeElectPath = znodeElectPath;

		run();

		log.info("{} run(『{}』, 『{}』)", Utility.indentEnd(), connectString, znodeElectPath);
	}

	private void run() {
		log.debug("{} run()", Utility.indentStart());

		try {
			zookeeper = new ZooKeeper(connectString, 3000, this);
		} catch (IOException e) {
			log.error("IOException:: {}", e.getMessage(), e);
		}

		log.debug("{} run()", Utility.indentEnd());
	}

	@Override
	public void process(WatchedEvent event) {
		log.info("{} process(『{}』) - 『{}:{}』『{}』", Utility.indentStart(), event, event.getType(), event.getState(), currentZNodeName);

		switch(event.getType()) {
		case None:
			switch(event.getState()) {
			case AuthFailed:
				break;
			case Closed:
				try {
					zookeeper = new ZooKeeper(connectString, 3000, this);
				} catch (IOException e) {
					log.error("IOException:: {}", e.getMessage(), e);
				}
				break;
			case ConnectedReadOnly:
				break;
			case Disconnected:
				isMaster = false;
				break;
			case Expired:
				try {
					zookeeper.removeWatches(znodeElectPath, this, WatcherType.Persistent, true);
					zookeeper.close();
					currentZNodeName = "";
					isMaster = false;

					run();
				} catch (InterruptedException e) {
					log.error("InterruptedException:: {}", e.getMessage(), e);
				} catch (KeeperException e) {
					log.error("KeeperException:: {}", e.getMessage(), e);
				}
				break;
			case SaslAuthenticated:
				break;
			case SyncConnected:
				updateMaster();
				try {
					if (currentZNodeName.startsWith(ZNODE_PREFIX)) {
						zookeeper.delete(znodeElectPath + "/" + currentZNodeName, -1);
						currentZNodeName = "";
					}
					zookeeper.addWatch(znodeElectPath, this, AddWatchMode.PERSISTENT);
					String zNodeFullPath = zookeeper.create(znodeElectPath + "/" + ZNODE_PREFIX, new byte[] {}, Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
					currentZNodeName = zNodeFullPath.replace(znodeElectPath + "/", "");
				} catch (KeeperException e) {
					log.error("KeeperException:: {}", e.getMessage(), e);
				} catch (InterruptedException e) {
					log.error("InterruptedException:: {}", e.getMessage(), e);
				}
				break;
			default:
				break;
			}
			break;
		case NodeDeleted:
		case NodeCreated:
		case NodeDataChanged:
		case NodeChildrenChanged:
			updateMaster();
			break;
		default:
			break;
		}
		
		log.info("{} process(『{}』) - 『{}:{}』『{}』", Utility.indentEnd(), event, event.getType(), event.getState(), currentZNodeName);
	}

	private boolean updateMaster() {
		if (zookeeper == null) {
			return false;
		}

		try {
			List<String> children = zookeeper.getChildren(znodeElectPath, false);
			if (children == null || children.isEmpty()) {
				return false;
			}

			Collections.sort(children);
			String smallestChild = children.get(0);
			isMaster = smallestChild.equals(currentZNodeName);
			log.info("{} updateMaster() - {} {} {}", Utility.indentMiddle(), children, currentZNodeName, isMaster);
			return true;
		} catch(Exception e) {
			isMaster = false;
			log.error("Exception:: {}", e.getMessage(), e);
		}

		return false;
	}

}
