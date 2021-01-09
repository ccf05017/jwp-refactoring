package kitchenpos.domain.menu;

import java.math.BigDecimal;
import java.util.List;

public interface SafeProduct {
    BigDecimal getProductPrice(Long productId);
    void isValidMenuPrice(MenuPrice menuPrice, List<MenuProduct> menuProducts);
}
