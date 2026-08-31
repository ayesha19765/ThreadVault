package controller;

import dto.response.CatalogFileResponse;
import dto.response.CatalogPageResponse;
import dto.response.CatalogSummaryResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import service.CatalogService;

/**
 * REST controller exposing backup catalog inspection, repository statistics, and file queries.
 */
@RestController
@RequestMapping("/api/catalog")
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    /**
     * Retrieves high-level repository summary statistics including unique files,
     * original vs stored bytes, deduplication savings, and total backups.
     *
     * @return 200 OK with catalog summary metrics
     */
    @GetMapping
    public ResponseEntity<CatalogSummaryResponse> getCatalogSummary() {
        CatalogSummaryResponse response = catalogService.getSummary();
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves a paginated list of cataloged files with optional path and hash filters.
     *
     * @param path optional substring filter on original file path
     * @param hash optional filter on SHA-256 hash
     * @param page zero-based page index (default 0)
     * @param size page size (default 20, max 100)
     * @return 200 OK with paginated catalog file metadata
     */
    @GetMapping("/files")
    public ResponseEntity<CatalogPageResponse<CatalogFileResponse>> getCatalogFiles(
            @RequestParam(value = "path", required = false) String path,
            @RequestParam(value = "hash", required = false) String hash,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        CatalogPageResponse<CatalogFileResponse> response = catalogService.getFiles(path, hash, page, size);
        return ResponseEntity.ok(response);
    }
}

