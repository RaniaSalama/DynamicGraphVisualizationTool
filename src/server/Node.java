package server;

public class Node implements Comparable<Node> {

	private double distortionValue;
	private int id;

	public Node(double distortionValue, int id) {
		this.distortionValue = distortionValue;
		this.id = id;
	}

	public int compareTo(Node node) {
		if (this.distortionValue >= node.distortionValue)
			return -1;
		return 1;
	}

	public double getDistortionValue() {
		return distortionValue;
	}

	public void setDistortionValue(double distortionValue) {
		this.distortionValue = distortionValue;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	@Override
	public int hashCode() {
		return id;
	}

	public boolean equals(Node obj) {
		if (this.id == obj.id)
			return true;
		else
			return false;
	}
}
