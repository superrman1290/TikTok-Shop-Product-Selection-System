package com.tiktokinsight.product.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.tiktokinsight.common.exception.ApiException;
import com.tiktokinsight.product.domain.ProductCatalogRepository;
import com.tiktokinsight.product.domain.ProductStatus;
import org.junit.jupiter.api.Test;

class ProductCatalogServiceTest {

    @Test
    void createsValidatedServerSideQuery() {
        var query = ProductCatalogService.query(
                2, 50, "us", 3L, " blender ", ProductStatus.ACTIVE, "currentPrice", "asc"
        );
        assertThat(query.offset()).isEqualTo(50);
        assertThat(query.market()).isEqualTo("US");
        assertThat(query.keyword()).isEqualTo("blender");
        assertThat(query.sort().column()).isEqualTo("p.current_price");
    }

    @Test
    void rejectsUnboundedPagesAndUnknownSortFields() {
        assertThatThrownBy(() -> ProductCatalogService.query(
                1, 101, null, null, null, ProductStatus.ACTIVE, "rating", "desc"
        )).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> ProductCatalogService.query(
                1, 20, null, null, null, ProductStatus.ACTIVE, "dropTable", "desc"
        )).isInstanceOf(ApiException.class);
    }

    @Test
    void rejectsReversedTrendDateRange() {
        ProductCatalogRepository repository = mock(ProductCatalogRepository.class);
        ProductCatalogService service = new ProductCatalogService(repository);
        assertThatThrownBy(() -> service.stats(
                1, java.time.LocalDate.parse("2026-07-21"), java.time.LocalDate.parse("2026-07-20")
        )).isInstanceOf(ApiException.class);
    }
}
