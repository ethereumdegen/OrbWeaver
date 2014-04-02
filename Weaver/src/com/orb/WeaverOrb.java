package com.orb;

import com.Weaver;
import com.network.Node;
import com.network.NodeInfo;

/*
 * An orb is a handler for a file that should be duplicated amongst all nodes.
 * Nodes that run Orb who have the file will try to seed it to the other nodes.
 * Nodes that run Orb that don't have the file will try to download it from the other nodes.
 * 
 * Orb determines whether or not you have a file by checking at the designated filePath and comparing the
 * [SHA-256] hash of that file with the hash of the file on the master nodes.  The master nodes need to have
 * the file and need to be running Orb. 
 */
public class WeaverOrb extends Thread {

	String filePath = null;

	String fileHash = null;

	Weaver weaver;
	
	ChunkManager chunkManager;

	public WeaverOrb(String path, Weaver weaver) {
		this.setName("Orb Thread");
		
		filePath = path;
		this.weaver = weaver;
		
		
	}

	@Override
	public void start() {
		super.start();
		
	}

	@Override
	public void run() {
		


		initialize();

		try {

			while (true) {
				update();
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	VersionManager versionManager;

	private void initialize() {
		chunkManager = new ChunkManager(this);
		versionManager = new VersionManager(this);

	}

	boolean checkedVersion = false;
	boolean seeding = false;

	public void update() throws Exception {

		if (fileHash != null) {
			if (!checkedVersion) {

				boolean hasCurrentVersion = versionManager.checkVersion(fileHash);
				
				
				checkedVersion = true;

				seeding = hasCurrentVersion;// if my files hash matches the hash
											// given by a master node, seed to
											// others
				System.out.println( " seeding? " + seeding );
				
				if(seeding){
					//init seeding
					getChunkManager().generateChunksFromFile();
				}

			} else {

				if (seeding) {

					for (Node node : weaver.getNodes()) {
						if (node != null) {
							node.updateSeeding();
						}

					}
					
					
					if(weaver.getQueuedChunkRequests()!=null){
						
					for( QueuedChunkRequest qcr :   weaver.getQueuedChunkRequests()){
						getNodeFromInfo(qcr.senderInfo).sendMessage(new NodeFileChunkMessage( weaver.getRegisteredOrb().getChunkManager().getChunkFromId(qcr.chunkId) ) );
					}
					}
					
					weaver.clearChunkRequestQueue();
										

				}else{
					
					for (Node node : weaver.getNodes()) {
						if (node != null) {
							node.updateLeeching();
						}

					}
					
				}

			}

		} else {
			if (weaver.getMyNode()!=null && weaver.getMyNode().isMasterNode()) {// if I am a master
				
				if(weaver.getRegisteredOrb()!=null){
				weaver.getRegisteredOrb().readFileHashAsMaster();
				}else{
					System.err.println("no registered orb");
				}

			} else {

				System.out.println("waiting for unique file hash from Master Node");

			}
		}

		Thread.sleep(500);

	}

	private Node getNodeFromInfo(NodeInfo senderInfo) {
		
		return null;
	}

	public String getFilePath() {
		return filePath;
	}

	public void setFileHash(String hash) {
		fileHash = hash;
		System.out.println("hash set to "+hash);
	}

	public void readFileHashAsMaster() {
		try {
			setFileHash(versionManager.getGameJarHash());
		} catch (Exception e) {
			System.err.println("I am a master node but I dont have the master file!");
			e.printStackTrace();
		}
	}

	public String getFileHash() {
		return fileHash;
	}
	
	public ChunkManager getChunkManager(){
		return chunkManager;
	}

}