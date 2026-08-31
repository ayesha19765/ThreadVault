package controller;

import dto.response.CatalogFileResponse;
import dto.response.CatalogPageResponse;
import dto.response.CatalogSummaryResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import service.CatalogService;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class CatalogControllerTest {

    private MockMvc mockMvc;

    private static class StubCatalogService implements CatalogService {
        @Override
        public CatalogSummaryResponse getSummary() {
            return new CatalogSummaryResponse(
                    10,
                    8,
                    50000L,
                    20000L,
                    30000L,
                    60.0,
                    2,
                    "2026-08-31T12:00:00"
            );
        }

        @Override
        public CatalogPageResponse<CatalogFileResponse> getFiles(String pathFilter, String hashFilter, int page, int size) {
            CatalogFileResponse file = new CatalogFileResponse();
            file.setOriginalPath("docs/sample.txt");
            file.setHash("abc123hash");
            file.setOriginalSize(500L);
            file.setCompressedSize(200L);
            file.setDeduplicated(false);

            return new CatalogPageResponse<>(
                    List.of(file),
                    page,
                    size,
                    1L,
                    1,
                    false
            );
        }
    }

    @BeforeEach
    void setUp() {
        CatalogService stubService = new StubCatalogService();
        CatalogController controller = new CatalogController(stubService);
        this.mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void testGetCatalogSummary_Returns200OK() throws Exception {
        mockMvc.perform(get("/api/catalog"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalFiles").value(10))
                .andExpect(jsonPath("$.uniqueFiles").value(8))
                .andExpect(jsonPath("$.totalOriginalBytes").value(50000))
                .andExpect(jsonPath("$.totalStoredBytes").value(20000))
                .andExpect(jsonPath("$.spaceSavedPercentage").value(60.0));
    }

    @Test
    void testGetCatalogFiles_Returns200OK() throws Exception {
        mockMvc.perform(get("/api/catalog/files?page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].originalPath").value("docs/sample.txt"))
                .andExpect(jsonPath("$.content[0].hash").value("abc123hash"));
    }
}

