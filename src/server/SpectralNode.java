package server;

public class SpectralNode implements Comparable<SpectralNode> {

  // Distortion value calculated based on the spectral method.
  private double distortionValue = 0.0;
  // Node id.
  private int id = -1;
  // Delta change of the node based on the absolute difference between its edges in graph1 and
  // graph2.
  private double delta = 0.0;

  /**
   * Node constructor.
   * 
   * @param distortionValue value of distortion for this node.
   * @param id node id.
   */
  public SpectralNode(double distortionValue, int id) {
    this.distortionValue = distortionValue;
    this.id = id;
  }

  /**
   * compareTo compares to nodes, which is used in sort functionality.
   */
  public int compareTo(SpectralNode node) {
    if (this.distortionValue > node.distortionValue) {
      return -1;
    } else if (this.distortionValue == node.distortionValue) {
      return 0;
    } else {
      return 1;
    }
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
  public int getId() {
    return id;
  }

  /**
   * Set node id.
   * 
   * @param id to set the node id to.
   */
  public void setId(int id) {
    this.id = id;
  }

  /**
   * Get delta change of the node.
   * @return delta change of the node.
   */
  public double getDelta() {
    return delta;
  }

  /**
   * Set delta change of the node.
   * @param delta change of the node.
   */
  public void setDelta(double delta) {
    this.delta = delta;
  }

  /**
   * Set the hash value of the node to its id.
   */
  @Override
  public int hashCode() {
    return id;
  }

  /**
   * Compare whether two nodes are the same or not by looking at their ids.
   * 
   * @param obj to determine whether it is the same as this node or not.
   * @return true if the two nodes are the same (have same id), otherwise return false.
   */
  public boolean equals(SpectralNode obj) {
    if (this.id == obj.id) {
      return true;
    } else {
      return false;
    }
  }
}
