public class KruskalEdge {
    public int u;
    public int v;
    public int frictionWeight;

    public int load;



    public KruskalEdge(int node1, int node2, int weight) {
        if (node1 < node2) {
            this.u = node1;
            this.v = node2;
        } else {
            this.u = node2;
            this.v = node1;
        }
        this.frictionWeight = weight;
        this.load = 0;
    }

    public int compareTo(KruskalEdge other) {
        if (this.frictionWeight < other.frictionWeight) return -1;
        if (this.frictionWeight > other.frictionWeight) return 1;

        if (this.u < other.u) return -1;
        if (this.u > other.u) return 1;

        if (this.v < other.v) return -1;
        if (this.v > other.v) return 1;

        return 0;
    }
}