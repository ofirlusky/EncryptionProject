package graph;

import game.Move;
import utils.Node;
import utils.Sorter;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class EulerianCryptoGraph {

    private List<Integer>[] adjList;
    private int numNodes;

    private List<Integer> keyStream;

    public EulerianCryptoGraph(int totalNodes, List<Move> gameHistory) {
        this.numNodes = totalNodes;

        // 1. אתחול המבנה
        adjList = new ArrayList[numNodes];
        for (int i = 0; i < numNodes; i++) {
            adjList[i] = new ArrayList<>();
        }

        buildBaseRing();
        overlayHistory(gameHistory);
        balanceGraph();
        int startNode = 0;

        if (gameHistory != null && !gameHistory.isEmpty()) {
            startNode = gameHistory.get(0).getFromNode();
        }
        this.keyStream = runHierholzer(startNode);
    }

    private void buildBaseRing() {
        for (int i = 0; i < numNodes; i++) {
            int nextNode = (i + 1) % numNodes;
            adjList[i].add(nextNode);
        }
    }

    private void overlayHistory(List<Move> history) {
        for (Move move : history) {
            adjList[move.getFromNode()].add(move.getToNode());
        }
    }

    public void printGraph() {
        System.out.println("\n=== הגרף האוילרי (לפני איזון) ===");
        for (int i = 0; i < numNodes; i++) {
            System.out.println("צומת " + i + " -> " + adjList[i]);
        }
    }

    // פה אני הופך את הגרף לאוילרי
    public void balanceGraph() {
        int[] inDegree = new int[numNodes];
        int[] outDegree = new int[numNodes];

        for (int i = 0; i < numNodes; i++) {
            outDegree[i] = adjList[i].size();
            for (int target : adjList[i]) {
                inDegree[target]++;
            }
        }


        List<Integer> needsOut = new ArrayList<>();
        List<Integer> needsIn = new ArrayList<>();

        for (int i = 0; i < numNodes; i++) {
            int balance = inDegree[i] - outDegree[i];
            if (balance > 0) {
                for (int j = 0; j < balance; j++) needsOut.add(i);
            } else if (balance < 0) {
                for (int j = 0; j < Math.abs(balance); j++) needsIn.add(i);
            }
        }

        for (int i = 0; i < needsOut.size(); i++) {
            int from = needsOut.get(i);
            int to = needsIn.get(i);

            adjList[from].add(to);
        }
    }


    public List<Integer> runHierholzer(int startNode) {

        for (int i = 0; i < numNodes; i++) {
            bubbleSort(adjList[i]);
        }

        List<Integer>[] tempAdj = new ArrayList[numNodes];
        for (int i = 0; i < numNodes; i++) {
            tempAdj[i] = new ArrayList<>(adjList[i]);
        }

        Stack<Integer> currPath = new Stack<>();
        List<Integer> circuit = new ArrayList<>();
        currPath.push(startNode);

        while (!currPath.isEmpty()) {
            int u = currPath.peek();

            if (!tempAdj[u].isEmpty()) {
                int nextV = tempAdj[u].remove(0);
                currPath.push(nextV);
            } else {
                circuit.add(currPath.pop());
            }
        }

        return manualReverse(circuit);
    }

    public List<Integer> getKeyStream() {
        return this.keyStream;
    }

    public void printKeyStream() {
        System.out.println("\n***************************************************");
        System.out.println("   GENERATED EULERIAN KEYSTREAM (Encryption Ready)   ");
        System.out.println("***************************************************");
        System.out.println("Start Node: " + (keyStream != null && !keyStream.isEmpty() ? keyStream.get(0) : "N/A"));
        System.out.println("Length: " + (keyStream != null ? keyStream.size() : 0) + " values");
        System.out.println("Key: " + keyStream);
        System.out.println("***************************************************\n");
    }

    private List<Integer> manualReverse(List<Integer> list) {
        List<Integer> reversed = new ArrayList<>();
        for (int i = list.size() - 1; i >= 0; i--) {
            reversed.add(list.get(i));
        }
        return reversed;
    }

    private void bubbleSort(List<Integer> list) {
        if (list == null || list.size() < 2) return;

        int n = list.size();
        for (int i = 0; i < n - 1; i++) {
            for (int j = 0; j < n - i - 1; j++) {
                if (list.get(j) > list.get(j + 1)) {
                    int temp = list.get(j);
                    list.set(j, list.get(j + 1));
                    list.set(j + 1, temp);
                }
            }
        }
    }
}