package controller;

import dto.response.CatalogFileResponse;
import dto.response.CatalogPageResponse;
import dto.response.CatalogSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
@Tag(name = "Catalog & Metadata", description = "Endpoints for inspecting stored backup metadata, storage reduction metrics, and paginated file listings")
public class CatalogController {

    private static final Logger logger = LoggerFactory.getLogger(CatalogController.class);

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
    @Operation(summary = "Get Catalog Summary", description = "Calculates repository-wide storage metrics, deduplication savings percentage, and file counts.")
    @ApiResponse(responseCode = "200", description = "Catalog summary metrics",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = CatalogSummaryResponse.class)))
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
    @Operation(summary = "Query Catalog Files", description = "Searches and paginates files stored in the metadata index with optional path and hash filters.")
    @ApiResponse(responseCode = "200", description = "Paginated catalog file records",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = CatalogPageResponse.class)))
    public ResponseEntity<CatalogPageResponse<CatalogFileResponse>> getCatalogFiles(
            @Parameter(description = "Case-insensitive substring filter for file path")
            @RequestParam(value = "path", required = false) String path,
            @Parameter(description = "SHA-256 content hash filter")
            @RequestParam(value = "hash", required = false) String hash,
            @Parameter(description = "Zero-based page index (default: 0)")
            @RequestParam(value = "page", defaultValue = "0") int page,
            @Parameter(description = "Page size between 1 and 100 (default: 20)")
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        CatalogPageResponse<CatalogFileResponse> response = catalogService.getFiles(path, hash, page, size);
        return ResponseEntity.ok(response);
    }
}
