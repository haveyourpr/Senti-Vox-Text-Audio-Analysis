import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class MinHeap {
    public static final class FrequencyPair {
        public final String word;
        public final int freq;
        FrequencyPair(String word, int freq) { this.word = word; this.freq = freq; }
    }

    private final List<FrequencyPair> heap = new ArrayList<>();
    private final int maxSize;

    public MinHeap(int maxSize) { this.maxSize = maxSize; }

    public void push(FrequencyPair val) {
        heap.add(val);
        bubbleUp();
        if (heap.size() > maxSize) pop();   // drop the smallest once we exceed K
    }

    public FrequencyPair pop() {
        if (heap.isEmpty()) return null;
        FrequencyPair min = heap.get(0);
        FrequencyPair last = heap.remove(heap.size() - 1);
        if (!heap.isEmpty()) { heap.set(0, last); siftDown(); }
        return min;
    }

    private void swap(int a, int b) { FrequencyPair t = heap.get(a); heap.set(a, heap.get(b)); heap.set(b, t); }

    private void bubbleUp() {
        int index = heap.size() - 1;
        while (index > 0) {
            int parent = (index - 1) / 2;
            if (heap.get(index).freq >= heap.get(parent).freq) break;
            swap(index, parent);
            index = parent;
        }
    }

    private void siftDown() {
        int index = 0, length = heap.size();
        while (true) {
            int left = 2 * index + 1, right = 2 * index + 2, smallest = index;
            if (left < length && heap.get(left).freq < heap.get(smallest).freq) smallest = left;
            if (right < length && heap.get(right).freq < heap.get(smallest).freq) smallest = right;
            if (smallest == index) break;
            swap(index, smallest);
            index = smallest;
        }
    }

    /** word -> frequency, in heap-array order (same as building the JS object from this.heap). */
    public Map<String, Integer> getAllElements() {
        Map<String, Integer> result = new LinkedHashMap<>();
        for (FrequencyPair p : heap) result.put(p.word, p.freq);
        return result;
    }

    public int size() { return heap.size(); }

    public static Map<String, Integer> getTopKWords(Map<String, Integer> wordMap, int k) {
        MinHeap heap = new MinHeap(k);
        for (Map.Entry<String, Integer> e : wordMap.entrySet()) heap.push(new FrequencyPair(e.getKey(), e.getValue()));
        return heap.getAllElements();
    }
}
