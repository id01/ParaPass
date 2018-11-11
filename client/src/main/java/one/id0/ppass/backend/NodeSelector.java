package one.id0.ppass.backend;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Scanner;

public class NodeSelector {
	// ArrayList of Strings representing the URLs of nodes and a String representing the last used node
	private ArrayList<String> nodes;
	private String lastUsedNode;
	// A File containing the default node in the first line followed by a list of nodes in plain text
	private File nodeListFile;
	
	// Initializes a NodeSelector
	public NodeSelector() throws IOException {
		nodes = new ArrayList<String>();
		nodeListFile = new File(Configuration.NODE_LIST_PATH);
		if (!nodeListFile.exists()) { // If the node list file doesn't exist, create it
			Configuration.createConfig();
		}
		// Try to load the nodes from the node list if it exists
		Scanner s = new Scanner(new File(Configuration.NODE_LIST_PATH));
		// The first line is the last used node, followed by lines for all nodes (including the last used node again)
		lastUsedNode = s.nextLine().trim();
		while (s.hasNextLine()) {
			nodes.add(s.nextLine().trim());
		}
		s.close();
	}
	
	// Adds a node to the NodeSelector ArrayList and appends it to the nodeListFile
	public void addNode(String node) throws IOException {
		nodes.add(node);
		PrintWriter writer = new PrintWriter(new FileWriter(nodeListFile, true));
		writer.write(node + "\n");
		writer.close();
	}
	
	// Sets the last used node by rewriting the nodeListFile. Doesn't do anything if the node is not valid
	// or if it is the same as the current last used node.
	public void setLastUsedNode(String node) throws IOException {
		if (!lastUsedNode.equals(node) && nodes.contains(node)) {
			lastUsedNode = node;
			PrintWriter writer = new PrintWriter(new FileWriter(nodeListFile));
			writer.write(node);
			for (String loadedNode: nodes) {
				writer.write(loadedNode + "\n");
			}
			writer.close();
		}
	}
	
	// Gets last used node
	public String getLastUsedNode() {
		return lastUsedNode;
	}
	
	// Gets all nodes
	public ArrayList<String> getNodes() {
		return nodes;
	}
}
