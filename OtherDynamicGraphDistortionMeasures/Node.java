

public class Node implements Comparable<Node> {

  private double distortionValue = 0.0;
  private String id = "";

  /**
   * Node constructor.
   * 
   * @param distortionValue value of distortion for this node.
   * @param id node id.
   */
  public Node(double distortionValue, String id) {
    this.distortionValue = distortionValue;
    this.id = id;
  }

  /**
   * compareTo compares to nodes, which is used in sort functionality.
   */
  public int compareTo(Node node) {
    if (this.distortionValue >= node.distortionValue) {
      return -1;
    }
    return 1;
  }

  /**
   * Get distortion value.
   * 
   * @return distortion value of this node.
   */
  public double getDistortionValue() {
    return distortionValue;
  }

  /**
   * Set distortion value of this node.
   * 
   * @param distortionValue distortion value to set to.
   */
  public void setDistortionValue(double distortionValue) {
    this.distortionValue = distortionValue;
  }

  /**
   * Get node id.
   * 
   * @return node id.
   */
  public String getId() {
    return id;
  }

  /**
   * Set node id.
   * 
   * @param id to set the node id to.
   */
  public void setId(String id) {
    this.id = id;
  }

  /**
   * Set the hash value of the node to its id.
   */
  @Override
  public int hashCode() {
    return id.hashCode();
  }

  /**
   * Compare whether two nodes are the same or not by looking at their ids.
   * 
   * @param obj to determine whether it is the same as this node or not.
   * @return true if the two nodes are the same (have same id), otherwise return false.
   */
  public boolean equals(Node obj) {
    if (this.id.equalsIgnoreCase(obj.id)) {
      return true;
    } else {
      return false;
    }
  }
}
