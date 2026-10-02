package ci.allogaz.catalog.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import ci.allogaz.catalog.domain.Product;
import ci.allogaz.catalog.domain.ProductFilter;

public interface ProductRepository {

    List<Product> findActive(ProductFilter filter);

    Optional<Product> findById(UUID id);

    Product save(Product product);
}
