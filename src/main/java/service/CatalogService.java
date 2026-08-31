package service;

import dto.response.CatalogFileResponse;
import dto.response.CatalogPageResponse;
import dto.response.CatalogSummaryResponse;

/**
 * Service interface for querying ThreadVault catalog, storage metrics, and file metadata.
 */
public interface CatalogService {

    /**
     * Calculates aggregate storage and deduplication statistics for the backup repository.
     *
     * @return catalog summary response
     */
    CatalogSummaryResponse getSummary();

    /**
     * Queries and paginates backed-up files with optional path and hash filters.
     *
     * @param pathFilter optional substring filter on originalPath
     * @param hashFilter optional exact/substring filter on SHA-256 hash
     * @param page zero-based page index
     * @param size page size (clamped to max 100)
     * @return paginated file metadata response
     */
    CatalogPageResponse<CatalogFileResponse> getFiles(String pathFilter, String hashFilter, int page, int size);
}

