import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Minimal diff of two int sequences with Myers' algorithm (linear-space version).
 * After construction, deleted[i] is true if a[i] is not kept, and inserted[j] is
 * true if b[j] is not kept.
 */
class Myers {
    final boolean[] deleted;
    final boolean[] inserted;

    // The sequences the algorithm actually runs on (see the constructor).
    private final int[] a;
    private final int[] b;
    private final boolean[] aDel;
    private final boolean[] bIns;

    // V arrays: vf[off + k] = furthest x reached on diagonal k = x - y going forward,
    // vb[off + k] = the same going backward from the end (in reversed coordinates).
    private final int[] vf;
    private final int[] vb;

    Myers(int[] origA, int[] origB) {
        deleted = new boolean[origA.length];
        inserted = new boolean[origB.length];

        // Speed-up: an element that never appears in the other sequence can never be
        // matched, so it is deleted/inserted for sure. Remove those first and run
        // Myers on what is left. The result stays minimal.
        Set<Integer> inA = new HashSet<>();
        Set<Integer> inB = new HashSet<>();
        for (int x : origA) inA.add(x);
        for (int x : origB) inB.add(x);

        int[] posA = new int[origA.length]; // posA[i] = index in origA of a[i]
        int n = 0;
        for (int i = 0; i < origA.length; i++) {
            if (inB.contains(origA[i])) posA[n++] = i;
            else deleted[i] = true;
        }
        int[] posB = new int[origB.length];
        int m = 0;
        for (int j = 0; j < origB.length; j++) {
            if (inA.contains(origB[j])) posB[m++] = j;
            else inserted[j] = true;
        }

        a = new int[n];
        b = new int[m];
        for (int i = 0; i < n; i++) a[i] = origA[posA[i]];
        for (int j = 0; j < m; j++) b[j] = origB[posB[j]];
        aDel = new boolean[n];
        bIns = new boolean[m];
        vf = new int[n + m + 4];
        vb = new int[n + m + 4];

        solve(0, n, 0, m);

        // Copy the result back to the original positions.
        for (int i = 0; i < n; i++) if (aDel[i]) deleted[posA[i]] = true;
        for (int j = 0; j < m; j++) if (bIns[j]) inserted[posB[j]] = true;
    }

    /** Diffs a[aLo..aHi) against b[bLo..bHi). */
    private void solve(int aLo, int aHi, int bLo, int bHi) {
        // Equal elements at the start and end are always kept.
        while (aLo < aHi && bLo < bHi && a[aLo] == b[bLo]) {
            aLo++;
            bLo++;
        }
        while (aLo < aHi && bLo < bHi && a[aHi - 1] == b[bHi - 1]) {
            aHi--;
            bHi--;
        }

        // If one side is empty, everything on the other side is an edit.
        if (aLo == aHi) {
            for (int j = bLo; j < bHi; j++) bIns[j] = true;
            return;
        }
        if (bLo == bHi) {
            for (int i = aLo; i < aHi; i++) aDel[i] = true;
            return;
        }

        // Otherwise split at a point on a shortest path and solve both halves.
        int[] mid = middle(aLo, aHi, bLo, bHi);
        solve(aLo, mid[0], bLo, mid[1]);
        solve(mid[0], aHi, mid[1], bHi);
    }

    /**
     * Searches forward from the top-left and backward from the bottom-right, one
     * edit at a time, until the two searches meet. Returns the meeting point {x, y}
     * (absolute indices), which lies on a shortest edit path.
     */
    private int[] middle(int aLo, int aHi, int bLo, int bHi) {
        int n = aHi - aLo;
        int m = bHi - bLo;
        int maxD = (n + m + 1) / 2;
        int off = maxD + 1;          // so that index off + k is never negative
        int size = 2 * maxD + 3;
        Arrays.fill(vf, 0, size, -1); // -1 means "not reached yet"
        Arrays.fill(vb, 0, size, -1);
        vf[off + 1] = 0;
        vb[off + 1] = 0;

        int delta = n - m;
        boolean odd = delta % 2 != 0;

        // Diagonals whose path has left the grid are skipped from then on.
        int fSkipLow = 0, fSkipHigh = 0, bSkipLow = 0, bSkipHigh = 0;

        for (int d = 0; d <= maxD; d++) {
            // Forward search: d edits from the top-left.
            for (int k = -d + fSkipLow; k <= d - fSkipHigh; k += 2) {
                // Step down from diagonal k+1 (insert) or right from diagonal k-1 (delete),
                // whichever gets further.
                int x;
                if (k == -d || (k != d && vf[off + k - 1] < vf[off + k + 1])) {
                    x = vf[off + k + 1];
                } else {
                    x = vf[off + k - 1] + 1;
                }
                int y = x - k;
                // Follow the snake: equal elements cost nothing.
                while (x < n && y < m && a[aLo + x] == b[bLo + y]) {
                    x++;
                    y++;
                }
                vf[off + k] = x;

                if (x > n) {
                    fSkipHigh += 2;
                } else if (y > m) {
                    fSkipLow += 2;
                } else if (odd) {
                    // Does this point overlap the backward search on the same diagonal?
                    int back = off + delta - k;
                    if (back >= 0 && back < size && vb[back] != -1 && x + vb[back] >= n) {
                        return new int[] {aLo + x, bLo + y};
                    }
                }
            }

            // Backward search: d edits from the bottom-right (sequences read in reverse).
            for (int k = -d + bSkipLow; k <= d - bSkipHigh; k += 2) {
                int x;
                if (k == -d || (k != d && vb[off + k - 1] < vb[off + k + 1])) {
                    x = vb[off + k + 1];
                } else {
                    x = vb[off + k - 1] + 1;
                }
                int y = x - k;
                while (x < n && y < m && a[aHi - 1 - x] == b[bHi - 1 - y]) {
                    x++;
                    y++;
                }
                vb[off + k] = x;

                if (x > n) {
                    bSkipHigh += 2;
                } else if (y > m) {
                    bSkipLow += 2;
                } else if (!odd) {
                    int fwd = off + delta - k;
                    if (fwd >= 0 && fwd < size && vf[fwd] != -1 && vf[fwd] + x >= n) {
                        int fx = vf[fwd];
                        int fy = fx - (delta - k);
                        return new int[] {aLo + fx, bLo + fy};
                    }
                }
            }
        }
        throw new IllegalStateException("forward and backward searches did not meet");
    }
}
