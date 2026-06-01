package com.example.tradget;

import java.util.ArrayList;
import java.util.List;

/**
 * Pagination helper for loading large lists in chunks.
 * Improves performance by reducing memory usage and UI lag.
 */
public class PaginationHelper<T> {

    public interface OnPageLoaded<T> {
        void onSuccess(List<T> items, boolean hasMore);
        void onFailure(String error);
    }

    private final int pageSize;
    private int currentPage = 0;
    private boolean hasMorePages = true;
    private final List<T> allItems = new ArrayList<>();

    public PaginationHelper(int pageSize) {
        this.pageSize = pageSize;
    }

    /**
     * Loads the next page of items.
     */
    public List<T> getNextPage(List<T> allAvailableItems) {
        if (!hasMorePages) return new ArrayList<>();

        int startIndex = currentPage * pageSize;
        int endIndex = Math.min(startIndex + pageSize, allAvailableItems.size());

        if (startIndex >= allAvailableItems.size()) {
            hasMorePages = false;
            return new ArrayList<>();
        }

        List<T> pageItems = new ArrayList<>(allAvailableItems.subList(startIndex, endIndex));
        allItems.addAll(pageItems);
        
        hasMorePages = endIndex < allAvailableItems.size();
        currentPage++;

        return pageItems;
    }

    /**
     * Resets pagination.
     */
    public void reset() {
        currentPage = 0;
        hasMorePages = true;
        allItems.clear();
    }

    /**
     * Returns whether there are more pages.
     */
    public boolean hasMorePages() {
        return hasMorePages;
    }

    /**
     * Returns all loaded items.
     */
    public List<T> getAllItems() {
        return new ArrayList<>(allItems);
    }

    /**
     * Returns the current page number.
     */
    public int getCurrentPage() {
        return currentPage;
    }

    /**
     * Returns the page size.
     */
    public int getPageSize() {
        return pageSize;
    }
}
