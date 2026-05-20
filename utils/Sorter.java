package utils;

import graph.KruskalEdge;

import java.util.List;

public class Sorter {



    public static void mergeSort(KruskalEdge[] arr, int left, int right) {
        if (left < right) {
            int mid = left + (right - left) / 2;
            mergeSort(arr, left, mid);
            mergeSort(arr, mid + 1, right);
            merge(arr, left, mid, right);
        }
    }

    private static void merge(KruskalEdge[] arr, int left, int mid, int right) {
        int n1 = mid - left + 1;
        int n2 = right - mid;

        KruskalEdge[] leftArray  = new KruskalEdge[n1];
        KruskalEdge[] rightArray = new KruskalEdge[n2];

        for (int i = 0; i < n1; ++i) leftArray[i]  = arr[left + i];
        for (int j = 0; j < n2; ++j) rightArray[j] = arr[mid + 1 + j];

        int i = 0, j = 0, k = left;

        while (i < n1 && j < n2) {
            if (leftArray[i].compareTo(rightArray[j]) <= 0) {
                arr[k] = leftArray[i]; i++;
            } else {
                arr[k] = rightArray[j]; j++;
            }
            k++;
        }

        while (i < n1) { arr[k] = leftArray[i]; i++; k++; }
        while (j < n2) { arr[k] = rightArray[j]; j++; k++; }
    }



    public static void mergeSortList(List<Integer> list, int left, int right) {
        if (left < right) {
            int mid = left + (right - left) / 2;
            mergeSortList(list, left, mid);
            mergeSortList(list, mid + 1, right);
            mergeList(list, left, mid, right);
        }
    }

    private static void mergeList(List<Integer> list, int left, int mid, int right) {
        int n1 = mid - left + 1;
        int n2 = right - mid;

        int[] leftArray  = new int[n1];
        int[] rightArray = new int[n2];

        for (int i = 0; i < n1; i++) leftArray[i]  = list.get(left + i);
        for (int j = 0; j < n2; j++) rightArray[j] = list.get(mid + 1 + j);

        int i = 0, j = 0, k = left;

        while (i < n1 && j < n2) {
            if (leftArray[i] <= rightArray[j]) {
                list.set(k, leftArray[i]); i++;
            } else {
                list.set(k, rightArray[j]); j++;
            }
            k++;
        }

        while (i < n1) { list.set(k, leftArray[i]); i++; k++; }
        while (j < n2) { list.set(k, rightArray[j]); j++; k++; }
    }
}