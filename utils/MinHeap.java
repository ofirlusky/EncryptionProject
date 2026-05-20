package utils;





public class MinHeap {

    private int[] heapDist;   // heapDist[i]  = מרחק של האיבר במיקום i ב-heap
    private int[] heapNode;   // heapNode[i]  = מזהה הצומת במיקום i ב-heap
    private int[] posInHeap;  // posInHeap[v] = מיקום של צומת v ב-heap  (-1 = לא קיים)

    private int size;
    private final int capacity;



    public MinHeap(int capacity) {
        this.capacity = capacity;
        this.size     = 0;
        heapDist  = new int[capacity];
        heapNode  = new int[capacity];
        posInHeap = new int[capacity];
        for (int i = 0; i < capacity; i++) posInHeap[i] = -1;
    }


    public void insert(int dist, int node) {
        heapDist[size]  = dist;
        heapNode[size]  = node;
        posInHeap[node] = size;
        size++;
        siftUp(size - 1);
    }


    public int extractMin() {
        int minNode     = heapNode[0];
        posInHeap[minNode] = -1;

        size--;
        if (size > 0) {

            heapDist[0]      = heapDist[size];
            heapNode[0]      = heapNode[size];
            posInHeap[heapNode[0]] = 0;
            siftDown(0);
        }
        return minNode;
    }


    public void decreaseKey(int node, int newDist) {
        int pos       = posInHeap[node];
        heapDist[pos] = newDist;
        siftUp(pos);
    }


    public boolean contains(int node) {
        return posInHeap[node] != -1;
    }


    public boolean isEmpty() {
        return size == 0;
    }

    // =========================================================
    // Heap maintenance
    // =========================================================


    private void siftUp(int i) {
        while (i > 0) {
            int parent = (i - 1) / 2;
            if (heapDist[parent] > heapDist[i]) {
                swap(i, parent);
                i = parent;
            } else {
                break;
            }
        }
    }


    private void siftDown(int i) {
        while (true) {
            int smallest = i;
            int left     = 2 * i + 1;
            int right    = 2 * i + 2;

            if (left  < size && heapDist[left]  < heapDist[smallest]) smallest = left;
            if (right < size && heapDist[right] < heapDist[smallest]) smallest = right;

            if (smallest != i) {
                swap(i, smallest);
                i = smallest;
            } else {
                break;
            }
        }
    }


    private void swap(int i, int j) {
        int tmpDist  = heapDist[i];
        heapDist[i]  = heapDist[j];
        heapDist[j]  = tmpDist;

        int tmpNode  = heapNode[i];
        heapNode[i]  = heapNode[j];
        heapNode[j]  = tmpNode;


        posInHeap[heapNode[i]] = i;
        posInHeap[heapNode[j]] = j;
    }
}