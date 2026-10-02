package ci.allogaz.catalog.application.port.out;

import java.util.List;
import java.util.Optional;

import ci.allogaz.catalog.domain.Category;

public interface CategoryRepository {

    List<Category> findAll();

    Optional<Category> findBySlug(String slug);
}
