package utils;

public class UnionFind {

    // אינדקס מספר של משבצת
    private int[] parent;


    public UnionFind(int n) {
        parent = new int[n];
        for (int i = 0; i < n; i++) {
            parent[i] = i;
        }
    }


    // מחזיר מי הבוס של i צומת
    public int find(int i) {
        if (parent[i] == i) {
            return i;
        }
        return find(parent[i]);
    }


    // שם את אותו מנהל לשני צמתים
    public void union(int i, int j) {
        int rootI = find(i);
        int rootJ = find(j);

        if (rootI != rootJ) {
            parent[rootI] = rootJ;
        }
    }
}